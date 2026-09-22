function resolveApiBaseUrl() {
  if (window.SHIPHAPPENS_API) return window.SHIPHAPPENS_API;
  const saved = localStorage.getItem('shiphappens_api_url');
  if (saved) return saved.trim().replace(/\/+$/, '');
  if (window.location.hostname.includes('onrender.com')) {
    return 'https://shiphappens-api-yyru.onrender.com/api/v1';
  }
  if (window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
    return `${window.location.origin}/api/v1`;
  }
  return 'http://localhost:8080/api/v1';
}

const API = resolveApiBaseUrl();

// Setup Swagger Docs link
const docsLink = document.querySelector('#apiDocs');
const mobileApiDocs = document.querySelector('#mobileApiDocs');
if (docsLink) {
  docsLink.href = `${API.replace(/\/api\/v1$/, '')}/swagger-ui/index.html`;
}
if (mobileApiDocs) {
  mobileApiDocs.href = `${API.replace(/\/api\/v1$/, '')}/swagger-ui/index.html`;
}

// State Management
let authToken = localStorage.getItem('shiphappens_token') || '';
let currentUser = JSON.parse(localStorage.getItem('shiphappens_user') || 'null');

// DOM Elements
const toast = document.querySelector('#toast');
const authContainer = document.querySelector('#authContainer');
const loginBtn = document.querySelector('#loginBtn');
const logoutBtn = document.querySelector('#logoutBtn');
const userProfile = document.querySelector('#userProfile');
const userEmailSpan = document.querySelector('#userEmail');
const userRoleBadge = document.querySelector('#userRole');
const loginModal = document.querySelector('#loginModal');
const closeLoginModal = document.querySelector('#closeLoginModal');
const loginForm = document.querySelector('#loginForm');

const mobileMenuToggle = document.querySelector('#mobileMenuToggle');
const mobileNavDrawer = document.querySelector('#mobileNavDrawer');
const mobileNavBackdrop = document.querySelector('#mobileNavBackdrop');
const closeMobileNavBtn = document.querySelector('#closeMobileNavBtn');
const mobileNewOrderBtn = document.querySelector('#mobileNewOrderBtn');
const mobileAuthRow = document.querySelector('#mobileAuthRow');
const mobileApiStatusText = document.querySelector('#mobileApiStatusText');

const statusFilter = document.querySelector('#statusFilter');
const reloadBtn = document.querySelector('#reload');
const shipmentsTableBody = document.querySelector('#shipmentsTableBody');

const trackingForm = document.querySelector('#trackingForm');
const shipmentIdInput = document.querySelector('#shipmentId');
const trackingMeta = document.querySelector('#trackingResultMeta');
const trackShipmentCode = document.querySelector('#trackShipmentCode');
const trackStatusBadge = document.querySelector('#trackStatusBadge');
const trackTrackingCode = document.querySelector('#trackTrackingCode');
const eventsList = document.querySelector('#events');

const createOrderForm = document.querySelector('#createOrderForm');
const assignDriverForm = document.querySelector('#assignDriverForm');
const updateStatusForm = document.querySelector('#updateStatusForm');
const integrationForm = document.querySelector('#integrationForm');
const integrationOutput = document.querySelector('#integrationOutput');
const openNewOrderBtn = document.querySelector('#openNewOrderBtn');

// Mobile Navigation Drawer Controls
function openMobileDrawer() {
  if (!mobileNavDrawer || !mobileNavBackdrop) return;
  mobileNavDrawer.classList.remove('hidden');
  mobileNavBackdrop.classList.remove('hidden');
  mobileMenuToggle?.setAttribute('aria-expanded', 'true');
  mobileNavDrawer.setAttribute('aria-hidden', 'false');
  document.body.style.overflow = 'hidden';
}

function closeMobileDrawer() {
  if (!mobileNavDrawer || !mobileNavBackdrop) return;
  mobileNavDrawer.classList.add('hidden');
  mobileNavBackdrop.classList.add('hidden');
  mobileMenuToggle?.setAttribute('aria-expanded', 'false');
  mobileNavDrawer.setAttribute('aria-hidden', 'true');
  document.body.style.overflow = '';
}

mobileMenuToggle?.addEventListener('click', () => {
  const isExpanded = mobileMenuToggle.getAttribute('aria-expanded') === 'true';
  if (isExpanded) {
    closeMobileDrawer();
  } else {
    openMobileDrawer();
  }
});

closeMobileNavBtn?.addEventListener('click', closeMobileDrawer);
mobileNavBackdrop?.addEventListener('click', closeMobileDrawer);

document.querySelectorAll('.mobile-nav-link').forEach(link => {
  link.addEventListener('click', () => {
    document.querySelectorAll('.mobile-nav-link').forEach(l => l.classList.remove('active'));
    link.classList.add('active');
    closeMobileDrawer();
  });
});

mobileNewOrderBtn?.addEventListener('click', () => {
  closeMobileDrawer();
  document.querySelector('[data-tab="tabOrder"]')?.click();
  document.querySelector('#operations-section')?.scrollIntoView({ behavior: 'smooth' });
});

loginModal?.addEventListener('click', (e) => {
  if (e.target === loginModal) {
    loginModal.classList.add('hidden');
  }
});

// Toast Notification
function showToast(message, type = 'success') {
  toast.textContent = message;
  toast.className = `toast toast-${type}`;
  toast.classList.remove('hidden');
  setTimeout(() => {
    toast.classList.add('hidden');
  }, 4000);
}

// HTTP Helper with Bearer & X-Request-ID
async function request(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  if (authToken && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${authToken}`;
  }

  let res;
  try {
    res = await fetch(`${API}${path}`, {
      ...options,
      headers
    });
  } catch (netErr) {
    throw new Error(`Cannot reach backend at ${API}. Make sure your backend server is running!`);
  }

  if (res.status === 204) return null;

  if (!res.ok) {
    let errorDetail = `${res.status} ${res.statusText}`;
    try {
      const errJson = await res.json();
      errorDetail = errJson.message || errJson.error || errorDetail;
    } catch {
      const text = await res.text();
      if (text) errorDetail = text;
    }
    throw new Error(errorDetail);
  }

  return res.json();
}

// Authentication UI Sync
function syncAuthUI() {
  if (authToken && currentUser) {
    loginBtn.classList.add('hidden');
    userProfile.classList.remove('hidden');
    userEmailSpan.textContent = currentUser.email;
    userRoleBadge.textContent = currentUser.role;

    if (mobileAuthRow) {
      mobileAuthRow.innerHTML = `
        <div style="display:flex; flex-direction:column; gap:6px; background:rgba(255,255,255,0.06); padding:10px 12px; border-radius:6px; border:1px solid rgba(255,255,255,0.1);">
          <span style="font-size:12px; color:#e2e8f0; font-weight:600; word-break:break-all;">${currentUser.email}</span>
          <span style="font-size:11px; color:#34d399; font-family:var(--font-mono);">${currentUser.role}</span>
          <button id="mobileLogoutBtn" class="btn btn-xs btn-ghost" style="margin-top:4px; text-align:left; padding:4px 0; color:#ef4444; cursor:pointer;">Sign out</button>
        </div>
      `;
      document.querySelector('#mobileLogoutBtn')?.addEventListener('click', () => {
        logoutBtn.click();
        closeMobileDrawer();
      });
    }
  } else {
    loginBtn.classList.remove('hidden');
    userProfile.classList.add('hidden');

    if (mobileAuthRow) {
      mobileAuthRow.innerHTML = `<button id="mobileLoginBtn" class="btn btn-sm btn-outline btn-block">Operator Login</button>`;
      document.querySelector('#mobileLoginBtn')?.addEventListener('click', () => {
        closeMobileDrawer();
        loginModal.classList.remove('hidden');
      });
    }
  }
}

// Load Metrics
async function loadDashboard() {
  try {
    const metrics = await request('/dashboard');
    const keys = ['activeShipments', 'inTransit', 'outForDelivery', 'atWarehouse', 'pickedUp', 'delivered', 'failed'];
    keys.forEach(k => {
      const el = document.querySelector(`#${k}`);
      if (el && metrics[k] !== undefined) {
        el.textContent = metrics[k];
      }
    });
    if (mobileApiStatusText) mobileApiStatusText.textContent = 'ONLINE';
  } catch (err) {
    console.error('Failed to load dashboard metrics:', err);
    if (mobileApiStatusText) mobileApiStatusText.textContent = 'RETRYING';
  }
}

// Load Shipments Table
async function loadShipments() {
  try {
    const filter = statusFilter ? statusFilter.value : '';
    const query = filter ? `?status=${encodeURIComponent(filter)}` : '';
    const shipments = await request(`/shipments${query}`);

    if (!shipments || shipments.length === 0) {
      shipmentsTableBody.innerHTML = `
        <tr>
          <td colspan="6" class="table-empty">No shipments found for the selected filter. Create one to begin.</td>
        </tr>`;
      return;
    }

    shipmentsTableBody.innerHTML = shipments.map(s => {
      const statusClass = `badge-${s.status.toLowerCase()}`;
      const destination = s.deliveryAddress ? `${s.deliveryAddress.city}, ${s.deliveryAddress.state}` : '—';
      const orderRef = s.orderNumber || '—';
      const cust = s.customerName || `Customer #${s.customerId || '—'}`;

      return `
        <tr>
          <td>
            <div class="shipment-code">${s.shipmentNumber}</div>
            <div class="tracking-code">${s.trackingNumber}</div>
          </td>
          <td><code>${orderRef}</code></td>
          <td><strong>${cust}</strong></td>
          <td>${destination}</td>
          <td><span class="badge ${statusClass}">${s.status.replace(/_/g, ' ')}</span></td>
          <td>
            <button class="btn btn-secondary btn-xs track-row-btn" data-id="${s.id}" data-shp="${s.shipmentNumber}" data-trk="${s.trackingNumber}" data-status="${s.status}">
              Track
            </button>
          </td>
        </tr>
      `;
    }).join('');

    // Attach row track click handlers
    document.querySelectorAll('.track-row-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = e.target.getAttribute('data-id');
        const shp = e.target.getAttribute('data-shp');
        const trk = e.target.getAttribute('data-trk');
        const status = e.target.getAttribute('data-status');
        shipmentIdInput.value = id;
        trackShipment(id, shp, trk, status);
        document.querySelector('#tracking-section').scrollIntoView({ behavior: 'smooth' });
      });
    });
  } catch (err) {
    shipmentsTableBody.innerHTML = `<tr><td colspan="6" class="table-empty" style="color:#ef4444">Error loading shipments: ${err.message}</td></tr>`;
  }
}

// Track Shipment by ID
async function trackShipment(id, shpCode = null, trkCode = null, status = null) {
  try {
    const events = await request(`/shipments/${id}/tracking`);

    trackShipmentCode.textContent = shpCode ? `Shipment: ${shpCode}` : `Shipment #${id}`;
    trackTrackingCode.textContent = trkCode ? `Tracking Ref: ${trkCode}` : '';
    if (status) {
      trackStatusBadge.textContent = status.replace(/_/g, ' ');
      trackStatusBadge.className = `badge badge-${status.toLowerCase()}`;
      trackStatusBadge.classList.remove('hidden');
    } else {
      trackStatusBadge.classList.add('hidden');
    }
    trackingMeta.classList.remove('hidden');

    if (!events || events.length === 0) {
      eventsList.innerHTML = `
        <li class="timeline-empty">
          <div class="timeline-bullet"></div>
          <div class="timeline-content">
            <strong>No tracking events logged yet</strong>
            <p>This shipment was recently created and is pending initial warehouse pickup.</p>
          </div>
        </li>`;
      return;
    }

    eventsList.innerHTML = events.map(ev => `
      <li class="event-item">
        <div class="event-bullet"></div>
        <div class="event-content">
          <strong>${ev.eventType.replace(/_/g, ' ')}</strong>
          <div class="event-desc">${ev.description || 'Status update logged'}${ev.location ? ` &middot; 📍 ${ev.location}` : ''}</div>
          <div class="event-time">${new Date(ev.createdAt).toLocaleString()}</div>
        </div>
      </li>
    `).join('');
  } catch (err) {
    showToast(`Tracking error: ${err.message}`, 'error');
  }
}

// Tab Switching
document.querySelectorAll('.tab-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
    btn.classList.add('active');
    const tabId = btn.getAttribute('data-tab');
    document.querySelector(`#${tabId}`).classList.add('active');
  });
});

// Event Listeners
reloadBtn?.addEventListener('click', () => {
  loadDashboard();
  loadShipments();
  showToast('Dashboard refreshed');
});

statusFilter?.addEventListener('change', loadShipments);

trackingForm?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = shipmentIdInput.value.trim();
  if (id) {
    trackShipment(id);
  }
});

openNewOrderBtn?.addEventListener('click', () => {
  document.querySelector('[data-tab="tabOrder"]').click();
  document.querySelector('#operations-section').scrollIntoView({ behavior: 'smooth' });
});

// Login Modal Handlers
loginBtn?.addEventListener('click', () => loginModal.classList.remove('hidden'));
closeLoginModal?.addEventListener('click', () => loginModal.classList.add('hidden'));

loginForm?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const email = document.querySelector('#loginEmail').value.trim();
  const password = document.querySelector('#loginPassword').value;

  try {
    const res = await request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });

    authToken = res.accessToken;
    currentUser = { email: res.email, role: res.role };
    localStorage.setItem('shiphappens_token', authToken);
    localStorage.setItem('shiphappens_user', JSON.stringify(currentUser));

    syncAuthUI();
    loginModal.classList.add('hidden');
    showToast(`Signed in as ${res.email} (${res.role})`);
  } catch (err) {
    showToast(`Authentication failed: ${err.message}`, 'error');
  }
});

logoutBtn?.addEventListener('click', () => {
  authToken = '';
  currentUser = null;
  localStorage.removeItem('shiphappens_token');
  localStorage.removeItem('shiphappens_user');
  syncAuthUI();
  showToast('Signed out');
});

// Create Order Form
createOrderForm?.addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!authToken) {
    showToast('Please sign in as Operator first to create orders.', 'error');
    loginModal.classList.remove('hidden');
    return;
  }
  const payload = {
    customerId: parseInt(document.querySelector('#orderCustomerId').value, 10),
    pickup: {
      addressLine1: document.querySelector('#pickupLine1').value,
      city: document.querySelector('#pickupCity').value,
      state: document.querySelector('#pickupState').value,
      postalCode: document.querySelector('#pickupPostal').value,
      country: document.querySelector('#pickupCountry').value
    },
    delivery: {
      addressLine1: document.querySelector('#deliveryLine1').value,
      city: document.querySelector('#deliveryCity').value,
      state: document.querySelector('#deliveryState').value,
      postalCode: document.querySelector('#deliveryPostal').value,
      country: document.querySelector('#deliveryCountry').value
    },
    packages: [
      {
        description: document.querySelector('#pkgDescription').value,
        weightKg: parseFloat(document.querySelector('#pkgWeight').value),
        declaredValue: parseFloat(document.querySelector('#pkgValue').value),
        lengthCm: 15,
        widthCm: 15,
        heightCm: 15
      }
    ]
  };

  try {
    const orderRes = await request('/orders', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    showToast(`Order created successfully: ${orderRes.orderNumber}!`);
    loadDashboard();
    loadShipments();
    createOrderForm.reset();
  } catch (err) {
    showToast(`Order creation failed: ${err.message}`, 'error');
  }
});

// Assign Driver Form
assignDriverForm?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const payload = {
    shipmentId: parseInt(document.querySelector('#assignShipmentId').value, 10),
    driverId: parseInt(document.querySelector('#assignDriverId').value, 10),
    vehicleId: parseInt(document.querySelector('#assignVehicleId').value, 10)
  };

  try {
    const res = await request('/deliveries/assign', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    showToast(`Delivery assigned to driver #${res.driverId}!`);
    loadDashboard();
    loadShipments();
  } catch (err) {
    showToast(`Assignment failed: ${err.message}`, 'error');
  }
});

// Update Status Form
updateStatusForm?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = parseInt(document.querySelector('#updateShipmentId').value, 10);
  const status = document.querySelector('#updateShipmentStatus').value;
  const location = document.querySelector('#updateLocation').value;
  const description = document.querySelector('#updateDescription').value;

  try {
    const res = await request(`/shipments/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status, location, description })
    });
    showToast(`Shipment ${res.shipmentNumber} updated to ${res.status}`);
    loadDashboard();
    loadShipments();
    trackShipment(id);
  } catch (err) {
    showToast(`Status update failed: ${err.message}`, 'error');
  }
});

// B2B Integration Simulator (External API Key)
integrationForm?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const extOrderId = document.querySelector('#integOrderId').value;
  const custCode = document.querySelector('#integCustCode').value;

  const payload = {
    externalOrderId: extOrderId,
    customerCode: custCode,
    pickup: {
      addressLine1: "100 Partner Hub",
      city: "Hyderabad",
      state: "Telangana",
      postalCode: "500001",
      country: "India"
    },
    delivery: {
      addressLine1: "500 Retail Depot",
      city: "Bengaluru",
      state: "Karnataka",
      postalCode: "560100",
      country: "India"
    },
    packages: [
      {
        description: "B2B Pallet 1",
        weightKg: 45.0,
        declaredValue: 25000.0,
        lengthCm: 80,
        widthCm: 60,
        heightCm: 50
      }
    ]
  };

  try {
    const res = await request('/integrations/orders', {
      method: 'POST',
      headers: {
        'X-API-KEY': 'vInp25bltikxqF3VTmX9Wh0LOGYSBM4g78uRe6ao'
      },
      body: JSON.stringify(payload)
    });

    integrationOutput.textContent = JSON.stringify(res, null, 2);
    integrationOutput.classList.remove('hidden');
    showToast(res.idempotent ? 'Idempotent request recognized (existing order returned)' : 'External order accepted & created!');
    loadDashboard();
    loadShipments();
  } catch (err) {
    integrationOutput.textContent = `Error: ${err.message}`;
    integrationOutput.classList.remove('hidden');
    showToast(`Integration submission failed: ${err.message}`, 'error');
  }
});

// Initial Bootstrap & Real-time Live Polling
syncAuthUI();
loadDashboard();
loadShipments();
setInterval(() => {
  loadDashboard();
  loadShipments();
}, 8000);

const apiStatusBadge = document.querySelector('#apiStatusBadge');
if (apiStatusBadge) {
  apiStatusBadge.addEventListener('click', () => {
    const next = window.prompt(
      `Current Backend API URL:\n${API}\n\nEnter custom Backend API Base URL (or leave blank to reset to auto-detect):`,
      API
    );
    if (next !== null) {
      if (next.trim() === '') {
        localStorage.removeItem('shiphappens_api_url');
      } else {
        localStorage.setItem('shiphappens_api_url', next.trim());
      }
      window.location.reload();
    }
  });
}

