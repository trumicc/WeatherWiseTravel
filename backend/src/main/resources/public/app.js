const weatherInfo = document.getElementById("weatherInfo");
const cityInput = document.getElementById("cityInput");
const searchBtn = document.getElementById("searchBtn");
const list = document.getElementById("recommendationsList");
const loading = document.getElementById("loading");

if (!weatherInfo || !cityInput || !searchBtn || !list || !loading) {
  throw new Error("Missing required elements (weatherInfo/cityInput/searchBtn/loading/recommendationsList)");
}

/* ---------- Leaflet map ---------- */
const map = L.map("map").setView([59.3293, 18.0686], 12);
L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
  attribution: "&copy; OpenStreetMap contributors",
}).addTo(map);

let markersLayer = L.layerGroup().addTo(map);

// Get selected categories from checkboxes
function getSelectedCategories() {
  const checkboxes = document.querySelectorAll('.categories input[type="checkbox"]:checked');
  const categories = Array.from(checkboxes).map(cb => cb.value);
  if (categories.length === 0) {
    alert("No categories found.");
    return;
  }
  return categories;
}

function setWeatherUI(city, weather) {
  const cityEl = weatherInfo.querySelector(".city");
  const tempEl = weatherInfo.querySelector(".temp");
  const condEl = weatherInfo.querySelector(".condition");

  if (!cityEl || !tempEl || !condEl) return;

  cityEl.textContent = city;
  tempEl.textContent = `${Math.round(weather.temperature)}°C`;
  condEl.textContent = weather.description ?? "-";
}

/* simple SVG icons */
function iconFor(categoryText) {
  const c = (categoryText || "").toLowerCase();

  if (c.includes("museum")) return `
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" aria-hidden="true">
      <path d="M4 10h16" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
      <path d="M6 10V19M10 10V19M14 10V19M18 10V19" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
      <path d="M4 19h16" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
      <path d="M12 4 3.5 8.5h17L12 4Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/>
    </svg>
  `;

  if (c.includes("cafe") || c.includes("café") || c.includes("coffee")) return `
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" aria-hidden="true">
      <path d="M5 8h10v5a5 5 0 0 1-10 0V8Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/>
      <path d="M15 9h2.2a2.3 2.3 0 0 1 0 4.6H15" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
      <path d="M4 19h14" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
    </svg>
  `;

  if (c.includes("park") || c.includes("nature")) return `
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" aria-hidden="true">
      <path d="M12 3c3 0 5 2.2 5 5 0 2.5-1.7 4.3-5 4.3S7 10.5 7 8c0-2.8 2-5 5-5Z"
            stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/>
      <path d="M12 12.3V21" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
      <path d="M8 21h8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
    </svg>
  `;

  return `
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" aria-hidden="true">
      <path d="M6 12h12" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
      <path d="M12 6v12" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
    </svg>
  `;
}

function starIcon() {
  return `
    <svg class="score-star" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M12 2.6l2.84 5.76 6.36.92-4.6 4.48 1.09 6.33L12 17.84 6.31 20.1l1.09-6.33-4.6-4.48 6.36-.92L12 2.6z"/>
    </svg>
  `;
}

function variantClass(categoryText, index) {
  const c = (categoryText || "").toLowerCase();
  if (c.includes("museum")) return "rec-blue";
  if (c.includes("cafe") || c.includes("café") || c.includes("coffee")) return "rec-orange";
  if (c.includes("park") || c.includes("nature")) return "rec-green";
  return ["rec-blue", "rec-orange", "rec-green"][index % 3];
}

function indoorOutdoor(activity) {
  if (typeof activity.indoor === "boolean") return activity.indoor ? "Indoor" : "Outdoor";
  if (typeof activity.isIndoor === "boolean") return activity.isIndoor ? "Indoor" : "Outdoor";

  if (activity.type) {
    const t = String(activity.type).toLowerCase();
    if (t.includes("indoor")) return "Indoor";
    if (t.includes("outdoor")) return "Outdoor";
  }
  return "";
}

function renderRecommendations(recs) {
  list.innerHTML = "";

  recs.forEach((r, i) => {
    const a = r.activity || {};
    const name = a.name || "Aktivitet";
    const category = a.category || a.type || "Kategori";
    const io = indoorOutdoor(a);
    const score = Number.isFinite(r.score) ? Math.round(r.score) : r.score;

    const card = document.createElement("div");
    card.className = `rec-card ${variantClass(category, i)}`;

    card.innerHTML = `
      <div class="row1">
        <div class="rec-title">
          <span class="rec-ic">${iconFor(category)}</span>
          <h3 title="${escapeHtml(name)}">${escapeHtml(name)}</h3>
        </div>
        <div class="rec-score">[${escapeHtml(String(score))}] ${starIcon()}</div>
      </div>

      <div class="rec-meta">
        <span>${escapeHtml(category)}</span>
        ${io ? `<span class="dot"></span><span>${escapeHtml(io)}</span>` : ``}
      </div>

      <div class="rec-reason">${escapeHtml(r.reason || "")}</div>
    `;

    list.appendChild(card);
  });
}

function renderMarkers(recs) {
  markersLayer.clearLayers();

  recs.forEach((r) => {
    const a = r.activity || {};
    if (a.latitude && a.longitude) {
      const marker = L.marker([a.latitude, a.longitude]).bindPopup(
          `<b>${escapeHtml(a.name || "")}</b><br>${escapeHtml(a.category || a.type || "")}<br>Score: ${Math.round(r.score)}`
      );
      markersLayer.addLayer(marker);
    }
  });

  const first = recs.find(rr => rr.activity && rr.activity.latitude && rr.activity.longitude);
  if (first) map.setView([first.activity.latitude, first.activity.longitude], 12);
}

async function fetchWeather(city) {
  const res = await fetch(`/api/v1/weather/${encodeURIComponent(city)}`);
  if (!res.ok) throw new Error("Kunde inte hämta väder");
  return await res.json();
}

async function fetchRecommendations(city) {
  const categories = getSelectedCategories();
  if (!categories) return null;

  const categoriesStr = categories.join(',');
  const res = await fetch(`/api/v1/recommendations?city=${encodeURIComponent(city)}&categories=${categoriesStr}`);
  if (!res.ok) throw new Error("Kunde inte hämta rekommendationer");
  return await res.json();
}

async function loadCity(city) {
  loading.style.display = "block";
  list.innerHTML = "";

  try {
    const weather = await fetchWeather(city);
    setWeatherUI(city, weather);

    const recs = await fetchRecommendations(city);
    if (!recs) {
      loading.style.display = "none";
      return;
    }
    renderRecommendations(recs);
    renderMarkers(recs);

    // IMPORTANT: re-calc Leaflet size after DOM/layout updates
    setTimeout(() => {
      map.invalidateSize(true);
    }, 50);

  } catch (err) {
    console.error(err);
    list.innerHTML = `<p style="color:#b91c1c; font-weight:800;">Fel: ${escapeHtml(err.message)}</p>`;
  } finally {
    loading.style.display = "none";
  }
}

searchBtn.addEventListener("click", () => {
  const city = cityInput.value.trim();
  if (!city) return;
  loadCity(city);
});

cityInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter") {
    e.preventDefault();
    searchBtn.click();
  }
});

function escapeHtml(str) {
  return String(str)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
}

/* also refresh map on window resize (desktop -> prevent weird collapses) */
window.addEventListener("resize", () => {
  setTimeout(() => {
    map.invalidateSize(true);
  }, 50);
});

/* klik på map för att söka stad */
map.on('click', async function(e) {
  const lat = e.latlng.lat;
  const lon = e.latlng.lng;

  console.log('Map clicked on:', lat, lon);

  loading.style.display = "block";
  list.innerHTML = '';

  try {
    const weatherRes = await fetch(`/api/v1/weather/coordinates?lat=${lat}&lon=${lon}`);
    if (!weatherRes.ok) throw new Error("Kunde inte hämta väder för koordinater");
    const weather = await weatherRes.json();

    setWeatherUI(weather.city || "Okänd plats", weather);

    // recommendationer with selected categories
    const categories = getSelectedCategories();
    if (!categories) {
      loading.style.display = "none";
      return;
    }
    const categoriesStr = categories.join(',');
    const recRes = await fetch(`/api/v1/recommendations/coordinates?lat=${lat}&lon=${lon}&categories=${categoriesStr}`);
    if (!recRes.ok) throw new Error("Kunde inte hämta rekommendationer");
    const recs = await recRes.json();

    console.log('Got', recs.length, 'recommendations');

    renderRecommendations(recs);
    renderMarkers(recs);

    const clickMarker = L.marker([lat, lon], {
      icon: L.icon({
        iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
        shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowSize: [41, 41]
      })
    }).addTo(map).bindPopup('Du klickade här!').openPopup();
    
    setTimeout(() => {
      map.removeLayer(clickMarker);
    }, 3000);

    setTimeout(() => {
      map.invalidateSize(true);
    }, 50);

  } catch (err) {
    console.error(err);
    list.innerHTML = `<p style="color:#b91c1c; font-weight:800;">Fel: ${escapeHtml(err.message)}</p>`;
  } finally {
    loading.style.display = "none";
  }
});