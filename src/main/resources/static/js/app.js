// Poll for new notifications so the badge updates without a page reload.
(function () {
  var badge = document.getElementById("notif-badge");
  if (!badge) return;
  function refresh() {
    fetch(badge.dataset.url, { credentials: "same-origin" })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (data) {
        if (!data) return;
        badge.textContent = data.unread;
        badge.hidden = data.unread === 0;
      })
      .catch(function () {});
  }
  setInterval(refresh, 30000);
})();
