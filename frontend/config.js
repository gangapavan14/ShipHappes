// Render Static Site uses the production default. Local development stays local.
window.SHIPHAPPENS_API = window.SHIPHAPPENS_API ||
  ((location.hostname === 'localhost' || location.hostname === '127.0.0.1')
    ? 'http://localhost:8080/api/v1'
    : 'https://shiphappens-api.onrender.com/api/v1');
