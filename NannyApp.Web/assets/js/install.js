(function () {
  'use strict';
  var button = document.getElementById('install-app');
  var status = document.getElementById('install-status');
  if (!button || !status) return;
  var promptEvent = null;
  function installed() {
    promptEvent = null;
    button.hidden = true;
    status.textContent = 'You are ready to use Nanny-App from your device.';
  }
  if (window.matchMedia('(display-mode: standalone)').matches || navigator.standalone) installed();
  window.addEventListener('beforeinstallprompt', function (event) {
    event.preventDefault();
    promptEvent = event;
    button.hidden = false;
  });
  button.addEventListener('click', async function () {
    if (!promptEvent) return;
    var event = promptEvent;
    promptEvent = null;
    button.disabled = true;
    try {
      await event.prompt();
      var choice = await event.userChoice;
      status.textContent = choice.outcome === 'accepted'
        ? 'Installation requested. Follow your browser instructions.'
        : 'You can install later from your browser menu.';
    } catch (_) {
      status.textContent = 'Use Install app or Add to Home Screen in your browser menu.';
    } finally {
      button.hidden = true;
      button.disabled = false;
    }
  });
  window.addEventListener('appinstalled', installed);
})();
