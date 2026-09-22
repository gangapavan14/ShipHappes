// Render Static Site uses the production default. Local development stays local.
const savedApi = localStorage.getItem('shiphappens_api_url');
window.SHIPHAPPENS_API = savedApi || window.SHIPHAPPENS_API ||
  ((location.hostname === 'localhost' || location.hostname === '127.0.0.1')
    ? 'http://localhost:8080/api/v1'
    : 'https://shiphappens-api-yyru.onrender.com/api/v1');

