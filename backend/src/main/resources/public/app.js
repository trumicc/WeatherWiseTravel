const weatherInfo = document.getElementById("weatherInfo");
const cityInput = document.getElementById("cityInput");
const searchBtn = document.getElementById("searchBtn");
const list = document.getElementById("recommendationsList");
const loading = document.getElementById("loading");

// --- Leaflet map ---
const map = L.map("map").setView([59.3293, 18.0686], 12); // Stockholm default
L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
  attribution: "&copy; OpenStreetMap contributors",
}).addTo(map);

let markersLayer = L.layerGroup().addTo(map);

function setWeatherUI(city, weather) {
  weatherInfo.querySelector(".city").textContent = city;
  weatherInfo.querySelector(".temp").textContent = `${weather.temperature}°C`;
  weatherInfo.querySelector(".condition").textContent = weather.description;
}

function renderRecommendations(recs) {
  list.innerHTML = "";

  recs.forEach((r) => {
    const div = document.createElement("div");
    div.className = "recommendation-card";
    div.innerHTML = `
      <h4>${r.activity.name}</h4>
      <p>${r.reason}</p>
      <small>Score: ${r.score}</small>
    `;
    list.appendChild(div);
  });
}

function renderMarkers(recs) {
  markersLayer.clearLayers();

  recs.forEach((r) => {
    const a = r.activity;
    if (a.latitude && a.longitude) {
      const marker = L.marker([a.latitude, a.longitude]).bindPopup(
        `<b>${a.name}</b><br>${a.category}<br>Score: ${r.score}`
      );
      markersLayer.addLayer(marker);
    }
  });

  // zoom to first marker if exists
  const first = recs.find(r => r.activity.latitude && r.activity.longitude);
  if (first) {
    map.setView([first.activity.latitude, first.activity.longitude], 12);
  }
}

async function fetchWeather(city) {
  const res = await fetch(`/api/v1/weather/${encodeURIComponent(city)}`);
  if (!res.ok) throw new Error("Kunde inte hämta väder");
  return await res.json();
}

async function fetchRecommendations(city) {
  const res = await fetch(`/api/v1/recommendations?city=${encodeURIComponent(city)}`);
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
    renderRecommendations(recs);
    renderMarkers(recs);
  } catch (err) {
    console.error(err);
    list.innerHTML = `<p style="color:red;">Fel: ${err.message}</p>`;
  } finally {
    loading.style.display = "none";
  }
}

searchBtn.addEventListener("click", () => {
  const city = cityInput.value.trim();
  if (city) loadCity(city);
});

// Auto-load default
loadCity("Stockholm");
