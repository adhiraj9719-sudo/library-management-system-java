(function () {
  var root = document.getElementById("charts");
  if (!root || typeof Chart === "undefined") return;
  fetch(root.dataset.url, { credentials: "same-origin" })
    .then(function (r) { return r.json(); })
    .then(function (d) {
      var spruce = "#2e5e4e", brass = "#b07d2b", stamp = "#b23a2e";
      new Chart(document.getElementById("trendChart"), {
        type: "line",
        data: { labels: d.trends.labels, datasets: [
          { label: "Borrowed", data: d.trends.borrowed, borderColor: spruce, backgroundColor: spruce, tension: .25 },
          { label: "Returned", data: d.trends.returned, borderColor: brass, backgroundColor: brass, tension: .25 }
        ]},
        options: { maintainAspectRatio: false, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
      });
      new Chart(document.getElementById("genreChart"), {
        type: "bar",
        data: { labels: d.genres.labels, datasets: [
          { label: "Total copies", data: d.genres.copies, backgroundColor: spruce },
          { label: "On loan", data: d.genres.on_loan, backgroundColor: stamp }
        ]},
        options: { maintainAspectRatio: false, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }
      });
    });
})();
