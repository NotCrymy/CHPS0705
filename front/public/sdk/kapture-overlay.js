/*!
 * SDK Kapture — intègre l'overlay de signalement dans un jeu web.
 * Protocole complet : docs/V2.md §4. Aucune dépendance.
 *
 *   <script src="https://kapture.example/sdk/kapture-overlay.js"></script>
 *   const kapture = KaptureOverlay.mount({
 *     session: 42, user: "Storm_X9",
 *     getState: () => ({ niveau: 3, position: { x, y } }),          // payload joint aux signalements
 *     onTriggerBug: (titre) => jeu.declencher(titre),                // l'application demande un bug : true si fait
 *     onReportOpened: () => jeu.pause(), onReportClosed: () => jeu.reprendre(),
 *   });
 *   kapture.bugAppeared("BUG2");                                     // un bug apparaît spontanément
 */
(function () {
  "use strict";

  var scriptCourant = document.currentScript;

  function mount(options) {
    var o = options || {};
    if (!o.session || !o.user) throw new Error("KaptureOverlay.mount : session et user sont obligatoires.");

    var base = o.baseUrl || (scriptCourant ? new URL(scriptCourant.src).origin : location.origin);
    var origineOverlay = new URL(base, location.href).origin;
    var getState = o.getState || function () { return {}; };
    var trace = o.onMessage || function () {};

    /* ── iframe plein écran, transparente, qui laisse passer les clics par défaut ── */
    var iframe = document.createElement("iframe");
    iframe.title = "Kapture — signalement de bugs";
    iframe.src = base + "/overlay/?" + new URLSearchParams({ session: String(o.session), user: String(o.user) });
    iframe.setAttribute("allowtransparency", "true");
    // Sans conteneur : plein écran. Avec conteneur (qui doit être positionné) : couvre le conteneur seul.
    Object.assign(iframe.style, {
      position: o.container ? "absolute" : "fixed", inset: "0", width: "100%", height: "100%", border: "0",
      background: "transparent", colorScheme: "normal",          // même color-scheme que l'overlay : sinon fond opaque
      zIndex: String(o.zIndex || 2147483000), pointerEvents: "none",
    });
    (o.container || document.body).appendChild(iframe);

    var pret = false;
    var enAttente = [];
    var zones = [];
    var capture = false;

    function envoyer(msg) {
      msg.source = "kapture-game";
      if (!pret) { enAttente.push(msg); return; }
      trace("→ overlay", msg);
      iframe.contentWindow.postMessage(msg, origineOverlay);
    }

    function souris(actif) { iframe.style.pointerEvents = actif ? "auto" : "none"; }

    function dansUneZone(x, y) {
      var r = iframe.getBoundingClientRect();
      for (var i = 0; i < zones.length; i++) {
        var z = zones[i];
        if (x >= r.left + z.x && x <= r.left + z.x + z.w && y >= r.top + z.y && y <= r.top + z.y + z.h) return true;
      }
      return false;
    }

    function surDeplacement(e) {
      if (!capture && dansUneZone(e.clientX, e.clientY)) souris(true);
    }
    window.addEventListener("pointermove", surDeplacement, true);

    var focusAvant = null;

    function surMessage(e) {
      var m = e.data;
      if (e.source !== iframe.contentWindow || e.origin !== origineOverlay || !m || m.source !== "kapture-overlay") return;
      trace("← overlay", m);
      switch (m.type) {
        case "ready":
          pret = true;
          enAttente.splice(0).forEach(envoyer);
          break;
        case "zones":
          zones = Array.isArray(m.zones) ? m.zones : [];
          capture = !!m.capture;
          if (capture) souris(true);
          break;
        case "passthrough":
          if (!capture) souris(false);
          break;
        case "get-state":
          envoyer({ type: "state", request_id: m.request_id, payload: getState() });
          break;
        case "trigger-bug":
          declencher(m);
          break;
        case "report-opened":
          focusAvant = document.activeElement;
          iframe.focus();
          o.onReportOpened && o.onReportOpened();
          break;
        case "report-closed":
          souris(false);
          if (focusAvant && focusAvant.focus) focusAvant.focus(); else window.focus();
          o.onReportClosed && o.onReportClosed();
          break;
        case "report-submitted":
          o.onReportSubmitted && o.onReportSubmitted(m.id);
          break;
      }
    }
    window.addEventListener("message", surMessage);

    function declencher(m) {
      var fin = function (succes, message) {
        envoyer({ type: "trigger-result", commande_id: m.commande_id, succes: !!succes, message: message || null, payload: getState() });
      };
      if (!o.onTriggerBug) return fin(false, "Le jeu n'accepte pas les déclenchements.");
      Promise.resolve()
        .then(function () { return o.onTriggerBug(m.titre); })
        .then(function (r) { fin(r !== false, r === false ? "Bug non disponible dans le jeu." : null); })
        .catch(function (err) { fin(false, String((err && err.message) || err)); });
    }

    return {
      iframe: iframe,
      /** Un bug vient d'apparaître spontanément dans le jeu. */
      bugAppeared: function (titre, payload) {
        envoyer({ type: "bug", titre: titre, payload: payload || getState() });
      },
      destroy: function () {
        window.removeEventListener("pointermove", surDeplacement, true);
        window.removeEventListener("message", surMessage);
        iframe.remove();
      },
    };
  }

  window.KaptureOverlay = { mount: mount, version: "2.0.0" };
})();
