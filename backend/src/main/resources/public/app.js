// ===== Helpers =====
function pick(obj, keys, fallback = "") {
    for (const k of keys) {
      if (obj && obj[k] !== undefined && obj[k] !== null) return obj[k];
    }
    return fallback;
  }
  
  function toNumber(x, fallback = null) {
    const n = Number(x);
    return Number.isFinite(n) ? n : fallback;
  }
  
  function guessEmoji(desc = "") {
    const d = desc.toLowerCase();
    if (d.includes("rain") || d.includes("regn")) return "🌧️";
    if (d.includes("snow") || d.includes("snö")) return "❄️";
    if (d.includes("cloud") || d.includes("moln")) return "☁️";
    if (d.includes("clear") || d.includes("klart")) return "☀️";
    if (d.includes("storm") || d.includes("åska")) return "⛈️";
    return "⛅";
  }
  
  function classifyCard(rec) {
    const type = (pick(rec, ["type", "category", "tag"], "") + "").toLowerCase();
    const indoor = (pick(rec, ["indoor"], false) + "").toLowerCase() === "true";
  
    if (type.includes("cafe") || type.includes("café")) return "cafe";
    if (indoor || type.includes("museum") || type.includes("indoor")) return "indoor";
    if (type.includes("park") || type.includes("outdoor")) return "outdoor";
    return "indoor";
  }
  
  function iconFor(rec) {
    const type = (pick(rec, ["type", "category", "tag"], "") + "").toLowerCase();
    if (type.includes("museum")) return "🏛️";
    if (type.includes("cafe") || type.includes("café")) return "☕";
    if (type.includes("park")) return "🌳";
    if (type.includes("restaurant")) return "🍽️";
    return "📍";
  }
  
  // ===== Leaflet map =====
  let map;
  let markersLayer;
  
  function initMap() {
    map = L.map("map").setView([59.3293, 18.0686], 12); // Stockholm default
  
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);
  
    markersLayer = L.layerGroup().addTo(map);
  }
  
  function setMapCenter(lat, lon, zoom = 12) {
    if (lat != null && lon != null) {
      map.setView([lat, lon], zoom);
    }
  }
  
  function clearMarkers() {
    markersLayer.clearLayers();
  }
  
  function addMarker(lat, lon, title) {
    if (lat == null || lon == null) return;
    L.marker([lat, lon]).addTo(markersLayer).bindPopup(title || "Plats");
  }
  
  // ===== UI updates =====
  function setLoading(isLoading) {
    document.getElementById("loading").style.display = isLoading ? "flex" : "none";
  }
  
  function renderWeather(weather) {
    // Stöder flera möjliga fältnamn (beroende på din Weather-model)
    const city = pick(weather, ["city", "name"], "Okänd stad");
    const temp = pick(weather, ["temperature", "temp", "tempC"], "--");
    const desc = pick(weather, ["description", "condition", "weatherDescription"], "—");
  
    document.getElementById("weatherCity").textContent = city;
    document.getElementById("weatherTemp").textContent = (temp === "--") ? "--°C" : `${Math.round(Number(temp))}°C`;
    document.getElementById("weatherDesc").textContent = desc;
    document.getElementById("weatherEmoji").textContent = guessEmoji(desc);
  
    // Center map if coords exist
    const lat = toNumber(pick(weather, ["lat", "latitude"], null), null);
    const lon = toNumber(pick(weather, ["lon", "lng", "longitude"], null), null);
    if (lat != null && lon != null) setMapCenter(lat, lon, 12);
  }
  
  function renderRecommendations(recs) {
    const list = document.getElementById("recommendationsList");
    list.innerHTML = "";
  
    if (!recs || recs.length === 0) {
      list.innerHTML = `<div class="rec-card"><div class="rec-title">Inga rekommendationer hittades</div></div>`;
      return;
    }
  
    recs.forEach((rec) => {
      const title = pick(rec, ["name", "title"], "Okänd plats");
      const score = pick(rec, ["score", "rating", "matchScore"], null);
      const note = pick(rec, ["reason", "description", "why"], "");
      const category = pick(rec, ["category", "type"], "");
      const indoorOutdoor = pick(rec, ["environment", "placeType"], "");
  
      const cls = classifyCard(rec);
      const icon = iconFor(rec);
  
      const metaParts = [];
      if (category) metaParts.push(`<span class="badge">${category}</span>`);
      if (indoorOutdoor) metaParts.push(`<span class="badge">${indoorOutdoor}</span>`);
  
      const scoreHtml = (score != null && score !== "")
        ? `<div class="score">[${score} ⭐]</div>`
        : "";
  
      const html = `
        <div class="rec-card ${cls}">
          <div class="rec-top">
            <div>
              <div class="rec-title">${icon} ${title}</div>
              <div class="rec-meta">
                ${metaParts.join("")}
              </div>
              ${note ? `<div class="rec-note">${note}</div>` : ""}
            </div>
            ${scoreHtml}
          </div>
        </div>
      `;
      list.insertAdjacentHTML("beforeend", html);
    });
  }
  
  // ===== API calls =====
  async function fetchJson(url) {
    const res = await fetch(url);
    if (!res.ok) {
      throw new Error(`${res.status} ${res.statusText}`);
    }
    return res.json();
  }
  
  async function loadCity(city) {
    setLoading(true);
    clearMarkers();
  
    try {
      // 1) Weather
      const weather = await fetchJson(`/api/v1/weather/${encodeURIComponent(city)}`);
      renderWeather(weather);
  
      // 2) Recommendations
      const recs = await fetchJson(`/api/v1/recommendations?city=${encodeURIComponent(city)}`);
      renderRecommendations(recs);
  
      // 3) Activities -> map markers (om endpoint finns)
      // Om din activity innehåller lat/lon så visas pins.
      try {
        const activities = await fetchJson(`/api/v1/activities?city=${encodeURIComponent(city)}`);
        if (Array.isArray(activities)) {
          activities.forEach((a) => {
            const name = pick(a, ["name", "title"], "Aktivitet");
            const lat = toNumber(pick(a, ["lat", "latitude"], null), null);
            const lon = toNumber(pick(a, ["lon", "lng", "longitude"], null), null);
            addMarker(lat, lon, name);
          });
        }
      } catch {
        // Om activities endpoint inte finns eller saknar coords: ignorera
      }
  
    } catch (err) {
      console.error(err);
      renderRecommendations([]);
      document.getElementById("weatherDesc").textContent = "Kunde inte hämta data";
    } finally {
      setLoading(false);
    }
  }
  
  // ===== Start =====
  document.addEventListener("DOMContentLoaded", () => {
    initMap();
  
    const input = document.getElementById("cityInput");
    const btn = document.getElementById("searchBtn");
  
    btn.addEventListener("click", () => loadCity(input.value.trim()));
    input.addEventListener("keydown", (e) => {
      if (e.key === "Enter") loadCity(input.value.trim());
    });
  
    loadCity(input.value.trim() || "Stockholm");
  });
  