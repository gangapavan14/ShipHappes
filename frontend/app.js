/**
 * ShipHappens Logistics OS - Single Page Application Engine
 * Minimalist Enterprise Brand, Leaflet Map Visualization, 2-Opt TSP HUD & Executive Demo
 */

(function () {
  'use strict';

  // --- 1. Environment & API Base ---
  function resolveApiBaseUrl() {
    if (window.SHIPHAPPENS_API) return window.SHIPHAPPENS_API.replace(/\/+$/, '');
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
  const API_ROOT = API.replace(/\/api\/v1$/, '');

  const swaggerLink = document.getElementById('sidebarSwaggerLink');
  if (swaggerLink) {
    swaggerLink.href = `${API_ROOT}/swagger-ui/index.html`;
  }

  // Application State
  let authToken = localStorage.getItem('shiphappens_token') || '';
  let currentUser = JSON.parse(localStorage.getItem('shiphappens_user') || 'null');
  let shipmentsCache = [];
  let routesCache = [];
  let customersCache = [];
  let currentActiveView = 'dashboard';

  // --- 2. Icon Helper ---
  function refreshIcons() {
    if (window.lucide && typeof window.lucide.createIcons === 'function') {
      window.lucide.createIcons();
    }
  }

  // --- 3. Toast Notifications ---
  const toastEl = document.getElementById('toast');
  let toastTimer = null;

  function showToast(message, type = 'success') {
    if (!toastEl) return;
    clearTimeout(toastTimer);
    toastEl.textContent = message;
    toastEl.className = `toast toast-${type}`;
    toastEl.classList.remove('hidden');
    toastTimer = setTimeout(() => {
      toastEl.classList.add('hidden');
    }, 4500);
  }

  // --- 4. HTTP API Helper ---
  async function apiRequest(path, options = {}) {
    const headers = {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    };

    if (authToken && !headers['Authorization']) {
      headers['Authorization'] = `Bearer ${authToken}`;
    }

    try {
      const res = await fetch(`${API}${path}`, { ...options, headers });
      if (!res.ok) {
        let errMsg = `Server returned status ${res.status}`;
        try {
          const rawText = await res.text();
          if (rawText && rawText.trim()) {
            try {
              const errData = JSON.parse(rawText);
              errMsg = errData.message || errData.error || rawText;
            } catch (_) {
              errMsg = rawText;
            }
          }
        } catch (_) {}

        if (res.status === 401 || res.status === 403) {
          errMsg = `Access denied (${res.status}). Please sign in as an Operator using the key icon.`;
        }
        throw new Error(errMsg);
      }
      if (res.status === 204) return null;
      return await res.json();
    } catch (err) {
      if (err.message && err.message.includes('Failed to fetch')) {
        updateBackendStatus(false);
        throw new Error(`Cannot reach logistics backend at ${API}. Verify Spring Boot is running.`);
      }
      throw err;
    }
  }

  // --- 5. Backend Status Indicator ---
  const relayDot = document.getElementById('relayDot');
  const relayLabel = document.getElementById('relayLabel');
  const backendPill = document.getElementById('backendStatusPill');

  function updateBackendStatus(online) {
    if (!relayDot || !relayLabel) return;
    if (online) {
      relayDot.style.background = '#10b981';
      relayLabel.textContent = 'BACKEND ONLINE';
      relayLabel.style.color = '#10b981';
    } else {
      relayDot.style.background = '#ef4444';
      relayLabel.textContent = 'BACKEND OFFLINE';
      relayLabel.style.color = '#ef4444';
    }
  }

  async function checkBackendHealth() {
    try {
      await fetch(`${API}/dashboard/summary`, { method: 'GET' });
      updateBackendStatus(true);
    } catch (_) {
      updateBackendStatus(false);
    }
  }

  if (backendPill) {
    backendPill.addEventListener('click', () => {
      const current = localStorage.getItem('shiphappens_api_url') || API;
      const updated = prompt('Configure Logistics Backend API Endpoint:', current);
      if (updated !== null && updated.trim()) {
        localStorage.setItem('shiphappens_api_url', updated.trim());
        window.location.reload();
      }
    });
  }

  // --- 6. View Routing Engine ---
  const VIEW_TITLES = {
    dashboard: 'Operations Dashboard',
    shipments: 'Shipments Registry & Lifecycle',
    routes: 'Fleet Route Dispatch & Map Visualizer',
    customers: 'Customer Registry & Accounts',
    vehicles: 'Vehicle Fleet Inventory',
    drivers: 'Drivers Roster',
    tracking: 'Live Telemetry & Audit Inspector',
    integrations: 'B2B ERP Simulator'
  };

  const currentViewTitleEl = document.getElementById('currentViewTitle');
  const navLinks = document.querySelectorAll('.sidebar-nav .nav-link[data-view]');
  const spaViews = document.querySelectorAll('.spa-view');

  function navigateTo(viewName, params = {}) {
    if (!viewName || !VIEW_TITLES[viewName]) {
      viewName = 'dashboard';
    }

    currentActiveView = viewName;
    window.location.hash = `#${viewName}`;

    navLinks.forEach(link => {
      if (link.getAttribute('data-view') === viewName) {
        link.classList.add('active');
      } else {
        link.classList.remove('active');
      }
    });

    spaViews.forEach(view => {
      if (view.id === `view-${viewName}`) {
        view.classList.remove('hidden');
        view.classList.add('active');
      } else {
        view.classList.add('hidden');
        view.classList.remove('active');
      }
    });

    if (currentViewTitleEl) {
      currentViewTitleEl.textContent = VIEW_TITLES[viewName];
    }

    closeSidebar();
    loadViewData(viewName, params);
    setTimeout(refreshIcons, 50);

    if (viewName === 'routes' && leafletMap) {
      setTimeout(() => {
        leafletMap.invalidateSize();
      }, 200);
    }
  }

  function loadViewData(viewName, params) {
    switch (viewName) {
      case 'dashboard':
        loadDashboard();
        break;
      case 'shipments':
        loadShipments();
        break;
      case 'routes':
        loadRoutes();
        break;
      case 'customers':
        loadCustomers();
        break;
      case 'vehicles':
        loadVehicles();
        break;
      case 'drivers':
        loadDrivers();
        break;
      case 'tracking':
        if (params.shipmentId) {
          const trackInput = document.getElementById('trackingQueryInput');
          if (trackInput) trackInput.value = params.shipmentId;
          fetchTrackingData(params.shipmentId);
        }
        break;
      case 'integrations':
        break;
    }
  }

  window.addEventListener('hashchange', () => {
    const rawHash = window.location.hash.replace(/^#/, '').trim();
    navigateTo(rawHash || 'dashboard');
  });

  // Sidebar Mobile Toggle
  const appSidebar = document.getElementById('appSidebar');
  const sidebarToggleBtn = document.getElementById('sidebarToggleBtn');

  function closeSidebar() {
    if (appSidebar && appSidebar.classList.contains('open')) {
      appSidebar.classList.remove('open');
    }
  }

  if (sidebarToggleBtn) {
    sidebarToggleBtn.addEventListener('click', () => {
      if (appSidebar) appSidebar.classList.toggle('open');
    });
  }

  // --- 7. Modals Manager ---
  function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
      modal.classList.remove('hidden');
      refreshIcons();
    }
  }

  function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
      modal.classList.add('hidden');
    }
  }

  document.querySelectorAll('[data-close]').forEach(btn => {
    btn.addEventListener('click', () => {
      closeModal(btn.getAttribute('data-close'));
    });
  });

  document.querySelectorAll('.modal-backdrop').forEach(modal => {
    modal.addEventListener('click', (e) => {
      if (e.target === modal) {
        modal.classList.add('hidden');
      }
    });
  });

  // --- 8. Dashboard Data & Command Center ---
  let autoSeeded = false;

  async function loadDashboard() {
    try {
      const summary = await apiRequest('/dashboard/summary');
      updateBackendStatus(true);

      const statusCounts = summary.shipmentsByStatus || {};
      const activeTotal = Object.entries(statusCounts)
        .filter(([st]) => st !== 'DELIVERED' && st !== 'FAILED' && st !== 'CANCELLED')
        .reduce((sum, [, count]) => sum + count, 0);

      setText('dashActiveShipments', activeTotal || 0);
      setText('dashAssigned', statusCounts['ASSIGNED'] || 0);
      setText('dashInTransit', statusCounts['IN_TRANSIT'] || 0);
      setText('dashOutForDelivery', statusCounts['OUT_FOR_DELIVERY'] || 0);
      setText('dashDelivered', statusCounts['DELIVERED'] || 0);
      setText('dashFailed', (statusCounts['FAILED'] || 0) + (statusCounts['CANCELLED'] || 0));
      setText('dashActiveRoutes', summary.totalActiveRoutes || 0);

      setText('sidebarShipmentsBadge', summary.totalShipments || 0);
      setText('sidebarRoutesBadge', summary.totalActiveRoutes || 0);

      // Auto-load executive demo scenario if zero routes exist on first launch
      if (summary.totalActiveRoutes === 0 && !autoSeeded) {
        autoSeeded = true;
        await loadExecutiveDemoScenario();
        return;
      }

      // Load routes & populate command map + fleet capacity widgets
      await loadRoutes();

      // Populate live dispatch feed
      if (!shipmentsCache.length) {
        const shipments = await apiRequest('/shipments');
        shipmentsCache = shipments || [];
      }
      renderDashboardLiveFeed(shipmentsCache);
    } catch (err) {
      console.warn('Dashboard fetch notice:', err.message);
    }
  }

  function renderDashboardFleetCapacity(routes) {
    const list = document.getElementById('dashFleetCapacityList');
    if (!list) return;

    if (!routes || !routes.length) {
      list.innerHTML = '<div class="table-empty">No active vehicles deployed.</div>';
      return;
    }

    list.innerHTML = routes.map((r, idx) => {
      const color = ROUTE_COLORS[idx % ROUTE_COLORS.length];
      const maxCap = 1000; // standard baseline or from vehicle
      const loaded = parseFloat(r.totalWeightKg) || 0;
      const pct = Math.min(100, Math.round((loaded / (loaded > 900 ? 1000 : 700)) * 100));
      const stops = r.stops ? r.stops.length : 0;

      return `
        <div class="fleet-cap-item">
          <div class="cap-item-top">
            <div class="cap-item-plate">
              <span style="width:8px; height:8px; border-radius:50%; background:${color};"></span>
              <span>${escapeHtml(r.vehicleNumber || 'Vehicle #' + r.id)}</span>
            </div>
            <span class="font-mono text-emerald" style="font-size:11px; font-weight:600;">${pct}% Loaded</span>
          </div>
          <div class="cap-item-sub">${escapeHtml(r.vehicleType || 'Commercial Courier')} &bull; ${stops} stops</div>
          <div class="cap-meter">
            <div class="cap-meter-fill ${pct > 90 ? 'warn' : ''}" style="width: ${pct}%; background:${color};"></div>
          </div>
          <div class="cap-item-footer">
            <span><strong>${loaded.toFixed(1)}</strong> kg payload</span>
            <div style="display:flex; gap:6px;">
              <button class="btn btn-secondary btn-sm" style="padding:2px 6px; font-size:10px;" onclick="window.ShipHappensApp.simulateVehicle('${escapeHtml(r.vehicleNumber)}')">
                <span>Simulate &rarr;</span>
              </button>
              <button class="btn btn-secondary btn-sm" style="padding:2px 6px; font-size:10px;" onclick="window.ShipHappensApp.openManifest(${r.id})">
                <span>Manifest</span>
              </button>
            </div>
          </div>
        </div>
      `;
    }).join('');
    refreshIcons();
  }

  function renderDashboardLiveFeed(shipments) {
    const list = document.getElementById('dashLiveFeedList');
    if (!list) return;

    if (!shipments || !shipments.length) {
      list.innerHTML = '<div class="table-empty">No shipments in dispatch log.</div>';
      return;
    }

    const recent = shipments.slice(0, 5);
    list.innerHTML = recent.map(s => {
      const cust = s.customer?.companyName || s.customer?.name || s.order?.customer?.companyName || 'Enterprise Corp';
      const dest = s.destinationAddress || 'Hyderabad Metro Corridor';
      const trk = s.trackingNumber || 'TRK-' + s.id;
      return `
        <div class="feed-item">
          <div class="feed-item-header">
            <span class="feed-tracking font-mono">${escapeHtml(trk)}</span>
            ${renderStatusBadge(s.status)}
          </div>
          <div class="feed-cust">${escapeHtml(cust)}</div>
          <div class="feed-dest" title="${escapeHtml(dest)}">${escapeHtml(truncate(dest, 36))}</div>
          <div class="feed-meta">
            <span class="font-mono">${s.weightKg ? s.weightKg + ' kg' : 'Standard'}</span>
            <button class="font-mono text-accent" style="background:none; border:none; cursor:pointer; font-size:10px;" onclick="window.ShipHappensApp.simulateConsignment('${escapeHtml(trk)}')">Track on Map &rarr;</button>
          </div>
        </div>
      `;
    }).join('');
    refreshIcons();
  }

  function setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
  }

  // --- 9. Customers Registry & Selector ---
  async function loadCustomers() {
    const tbody = document.getElementById('customersTableBody');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="7" class="table-empty">Loading customers...</td></tr>';

    try {
      const customers = await apiRequest('/customers');
      customersCache = customers || [];
      setText('sidebarCustomersBadge', customersCache.length);

      if (!customersCache.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="table-empty">No customers registered yet. Click "Register Customer" above.</td></tr>';
        return;
      }

      tbody.innerHTML = customersCache.map(c => `
        <tr>
          <td><strong class="font-mono text-accent">${escapeHtml(c.customerCode || 'CUST-' + c.id)}</strong></td>
          <td><strong>${escapeHtml(c.companyName || c.name || 'Unnamed Corporate')}</strong></td>
          <td>${escapeHtml(c.email || '—')}</td>
          <td><span class="font-mono">${escapeHtml(c.phone || '—')}</span></td>
          <td><span class="badge ${c.status === 'ACTIVE' ? 'badge-accent' : 'badge-subtle'}">${c.status || 'ACTIVE'}</span></td>
          <td><span class="font-mono text-secondary">${c.createdAt ? new Date(c.createdAt).toLocaleDateString() : '—'}</span></td>
          <td>
            <button class="btn btn-secondary btn-sm" onclick="window.ShipHappensApp.dispatchForCustomer(${c.id})">
              <span>+ Dispatch</span>
            </button>
          </td>
        </tr>
      `).join('');

      populateCustomerSelect();
      refreshIcons();
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="7" class="table-empty text-rose">Failed loading customers: ${escapeHtml(err.message)}</td></tr>`;
    }
  }

  function populateCustomerSelect() {
    const select = document.getElementById('newShipmentCustomerSelect');
    if (!select) return;

    if (!customersCache.length) {
      select.innerHTML = '<option value="1">Default Account (ID: 1)</option>';
      return;
    }

    select.innerHTML = customersCache.map(c => `
      <option value="${c.id}">${escapeHtml(c.companyName || c.name || c.customerCode)} (Code: ${c.customerCode || 'CUST-' + c.id})</option>
    `).join('');
  }

  // Register Customer Form
  const newCustomerForm = document.getElementById('newCustomerForm');
  if (newCustomerForm) {
    newCustomerForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const code = document.getElementById('newCustCode').value.trim();
      const name = document.getElementById('newCustName').value.trim();
      const email = document.getElementById('newCustEmail').value.trim();
      const phone = document.getElementById('newCustPhone').value.trim();

      try {
        await apiRequest('/customers', {
          method: 'POST',
          body: JSON.stringify({
            customerCode: code,
            name: name,
            companyName: name,
            email: email,
            phone: phone,
            status: 'ACTIVE'
          })
        });

        showToast(`Customer ${name} registered successfully`);
        closeModal('newCustomerModal');
        newCustomerForm.reset();
        await loadCustomers();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }

  const openNewCustomerModalBtn = document.getElementById('openNewCustomerModalBtn');
  if (openNewCustomerModalBtn) {
    openNewCustomerModalBtn.addEventListener('click', () => openModal('newCustomerModal'));
  }

  // --- 10. Shipments Management ---
  async function loadShipments() {
    const tbody = document.getElementById('shipmentsTableBody');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="7" class="table-empty">Loading shipments pipeline...</td></tr>';

    try {
      const shipments = await apiRequest('/shipments');
      shipmentsCache = shipments || [];
      setText('sidebarShipmentsBadge', shipmentsCache.length);
      renderShipmentsTable(shipmentsCache);
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="7" class="table-empty text-rose">Failed loading shipments: ${escapeHtml(err.message)}</td></tr>`;
    }
  }

  function renderShipmentsTable(list) {
    const tbody = document.getElementById('shipmentsTableBody');
    if (!tbody) return;

    const query = (document.getElementById('shipmentSearchInput')?.value || '').toLowerCase().trim();
    const filter = document.getElementById('shipmentStatusFilter')?.value || '';

    const filtered = list.filter(s => {
      if (filter && s.status !== filter) return false;
      if (!query) return true;
      const track = (s.trackingNumber || '').toLowerCase();
      const code = (s.shipmentNumber || '').toLowerCase();
      const addr = (s.destinationAddress || '').toLowerCase();
      const cust = s.customer ? (s.customer.name || s.customer.companyName || '').toLowerCase() : '';
      return track.includes(query) || code.includes(query) || addr.includes(query) || cust.includes(query);
    });

    if (!filtered.length) {
      tbody.innerHTML = '<tr><td colspan="7" class="table-empty">No shipments match current filters.</td></tr>';
      return;
    }

    tbody.innerHTML = filtered.map(s => {
      const custName = s.order?.customer?.companyName || s.order?.customer?.name || s.customer?.name || 'Enterprise Client';
      const weight = s.weightKg ? `${s.weightKg} kg` : '—';
      const dest = s.destinationAddress || (s.order?.deliveryAddress ? `${s.order.deliveryAddress.street1}, ${s.order.deliveryAddress.city}` : 'Hyderabad Corridor');

      return `
        <tr>
          <td>
            <strong class="font-mono text-accent">${escapeHtml(s.trackingNumber || 'TRK-' + s.id)}</strong>
            <span class="font-mono text-tertiary" style="display:block; font-size:10px;">${escapeHtml(s.shipmentNumber || '')}</span>
          </td>
          <td><span class="font-mono text-secondary">${escapeHtml(s.order?.orderNumber || 'ORD-' + s.id)}</span></td>
          <td><strong>${escapeHtml(custName)}</strong></td>
          <td title="${escapeHtml(dest)}">${escapeHtml(truncate(dest, 32))}</td>
          <td><span class="font-mono">${escapeHtml(weight)}</span></td>
          <td>${renderStatusBadge(s.status)}</td>
          <td>
            <div style="display:flex; gap:6px;">
              <button class="btn btn-secondary btn-sm" onclick="window.ShipHappensApp.openTransition(${s.id})">
                <span>Update</span>
              </button>
              <button class="btn btn-secondary btn-sm" onclick="window.ShipHappensApp.trackShipment(${s.id})">
                <span>Track</span>
              </button>
            </div>
          </td>
        </tr>
      `;
    }).join('');

    refreshIcons();
  }

  function renderStatusBadge(status) {
    const map = {
      CREATED: 'badge-subtle',
      ASSIGNED: 'badge-accent',
      PICKED_UP: 'badge-accent',
      IN_TRANSIT: 'badge-accent',
      AT_WAREHOUSE: 'badge-subtle',
      OUT_FOR_DELIVERY: 'badge-accent',
      DELIVERED: 'badge-accent',
      FAILED: 'badge-subtle text-rose',
      CANCELLED: 'badge-subtle text-rose'
    };
    return `<span class="badge ${map[status] || 'badge-subtle'}">${status}</span>`;
  }

  document.getElementById('shipmentSearchInput')?.addEventListener('input', () => renderShipmentsTable(shipmentsCache));
  document.getElementById('shipmentStatusFilter')?.addEventListener('change', () => renderShipmentsTable(shipmentsCache));
  document.getElementById('refreshShipmentsBtn')?.addEventListener('click', loadShipments);

  // Dispatch Shipment Modal
  const globalNewShipmentBtn = document.getElementById('globalNewShipmentBtn');
  const openDispatchModalBtn = document.getElementById('openDispatchModalBtn');
  if (globalNewShipmentBtn) globalNewShipmentBtn.addEventListener('click', () => {
    populateCustomerSelect();
    openModal('dispatchShipmentModal');
  });
  if (openDispatchModalBtn) openDispatchModalBtn.addEventListener('click', () => {
    populateCustomerSelect();
    openModal('dispatchShipmentModal');
  });

  const dispatchShipmentForm = document.getElementById('dispatchShipmentForm');
  if (dispatchShipmentForm) {
    dispatchShipmentForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const customerId = parseInt(document.getElementById('newShipmentCustomerSelect').value || '1', 10);
      const pkgDesc = document.getElementById('newShipmentPkgDesc').value.trim();
      const weight = parseFloat(document.getElementById('newShipmentWeight').value);
      const declaredVal = parseFloat(document.getElementById('newShipmentValue').value);

      const line1 = document.getElementById('newDeliveryLine1').value.trim();
      const city = document.getElementById('newDeliveryCity').value.trim();
      const state = document.getElementById('newDeliveryState').value.trim();
      const postal = document.getElementById('newDeliveryPostal').value.trim();
      const country = document.getElementById('newDeliveryCountry').value.trim();

      const latInput = document.getElementById('newDeliveryLat').value.trim();
      const lonInput = document.getElementById('newDeliveryLon').value.trim();
      const lat = latInput ? parseFloat(latInput) : null;
      const lon = lonInput ? parseFloat(lonInput) : null;

      try {
        const orderPayload = {
          customerId: customerId,
          deliveryAddress: {
            street1: line1,
            city: city,
            state: state,
            postalCode: postal,
            country: country,
            latitude: lat,
            longitude: lon
          },
          packages: [
            {
              description: pkgDesc,
              weightKg: weight,
              declaredValue: declaredVal
            }
          ]
        };

        const createdOrder = await apiRequest('/orders', {
          method: 'POST',
          body: JSON.stringify(orderPayload)
        });

        showToast(`Shipment for Order #${createdOrder.orderNumber} successfully dispatched!`);
        closeModal('dispatchShipmentModal');
        dispatchShipmentForm.reset();
        await loadShipments();
        await loadDashboard();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }

  // Lifecycle Status Transition
  const statusTransitionForm = document.getElementById('statusTransitionForm');
  if (statusTransitionForm) {
    statusTransitionForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const shipmentId = document.getElementById('transitionShipmentId').value;
      const targetStatus = document.getElementById('transitionTargetStatus').value;
      const loc = document.getElementById('transitionLocation').value.trim();
      const desc = document.getElementById('transitionDescription').value.trim();

      try {
        await apiRequest(`/shipments/${shipmentId}/status`, {
          method: 'PATCH',
          body: JSON.stringify({
            status: targetStatus,
            location: loc,
            description: desc
          })
        });

        showToast(`Shipment #${shipmentId} transitioned to ${targetStatus}`);
        closeModal('statusTransitionModal');
        await loadShipments();
        await loadDashboard();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }

  // --- 11. Interactive Leaflet Routing Map & Live Simulation ---
  let leafletMap = null;
  let mapLayers = [];
  let mapRoutesData = [];
  let simAnimId = null;
  let simActive = false;

  let activeSimFilter = {
    type: 'all', // 'all' | 'vehicle' | 'consignment'
    vehicleNumber: null,
    consignmentTracking: null
  };

  const ROUTE_COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', '#06b6d4'];
  const DEPOT_COORDS = [17.385044, 78.486671]; // Central Hyderabad Hub

  function initLeafletMap() {
    if (leafletMap || !document.getElementById('routeLeafletMap')) return;

    leafletMap = L.map('routeLeafletMap', {
      center: DEPOT_COORDS,
      zoom: 12,
      zoomControl: true,
      attributionControl: false
    });

    // Free OpenStreetMap standard tiles + CSS dark high-contrast filter = 100% free, zero watermark, zero API keys
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap'
    }).addTo(leafletMap);
  }

  function renderMapRoutes(routes, filter = activeSimFilter) {
    initLeafletMap();
    if (!leafletMap) return;

    // Clear previous static markers & polylines from the map
    mapLayers.forEach(l => {
      try { leafletMap.removeLayer(l); } catch (_) {}
    });
    mapLayers = [];

    // Render Central Depot Marker
    const depotIcon = L.divIcon({
      className: 'depot-pin',
      html: '<div style="background:#2563eb; color:#fff; width:28px; height:28px; border-radius:50%; display:flex; align-items:center; justify-content:center; border:2px solid #fff; font-size:12px; font-weight:700; box-shadow:0 0 12px rgba(37,99,235,0.7);">★</div>',
      iconSize: [28, 28],
      iconAnchor: [14, 14]
    });

    const depotMarker = L.marker(DEPOT_COORDS, { icon: depotIcon })
      .bindPopup('<strong>ShipHappens Central Hyderabad Depot</strong><br>Origin: 17.385044, 78.486671')
      .addTo(leafletMap);
    mapLayers.push(depotMarker);

    const bounds = L.latLngBounds([DEPOT_COORDS]);
    let routeIdx = 0;
    let selectedStopMarkerToOpen = null;

    routes.forEach(route => {
      // Check visibility against filter
      let isVisible = true;
      if (filter.type === 'vehicle' && filter.vehicleNumber) {
        isVisible = (route.vehicleNumber === filter.vehicleNumber);
      } else if (filter.type === 'consignment' && filter.consignmentTracking) {
        isVisible = (route.stops || []).some(s => s.trackingNumber === filter.consignmentTracking);
      }
      if (!isVisible) return;

      const color = ROUTE_COLORS[routeIdx % ROUTE_COLORS.length];
      routeIdx++;

      const latlngs = [DEPOT_COORDS];
      const stops = (route.stops || []).slice().sort((a, b) => a.sequenceOrder - b.sequenceOrder);

      stops.forEach((stop, idx) => {
        const lat = stop.arrivalLatitude || (17.385044 + (idx * 0.015));
        const lon = stop.arrivalLongitude || (78.486671 + (idx * 0.015));
        latlngs.push([lat, lon]);
        bounds.extend([lat, lon]);

        const isTargetConsignment = (filter.type === 'consignment' && stop.trackingNumber === filter.consignmentTracking);
        const pinClass = isTargetConsignment ? 'stop-pin highlighted-consignment-pin' : 'stop-pin';
        const pinBorder = isTargetConsignment ? '3px solid #f59e0b' : '1.5px solid #fff';
        const pinShadow = isTargetConsignment ? '0 0 16px rgba(245, 158, 11, 0.9)' : '0 2px 6px rgba(0,0,0,0.4)';

        const stopIcon = L.divIcon({
          className: pinClass,
          html: `<div style="background:${color}; color:#fff; width:24px; height:24px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-family:'JetBrains Mono',monospace; font-size:11px; font-weight:700; border:${pinBorder}; box-shadow:${pinShadow};">${stop.sequenceOrder}</div>`,
          iconSize: [24, 24],
          iconAnchor: [12, 12]
        });

        const stopMarker = L.marker([lat, lon], { icon: stopIcon })
          .bindPopup(`
            <div style="font-family:'Inter',sans-serif; min-width:180px;">
              <strong style="color:${color}; font-size:12px;">Stop #${stop.sequenceOrder} &bull; ${escapeHtml(stop.trackingNumber || '')}</strong><br>
              <div style="font-size:11px; margin-top:3px; color:#cbd5e1;"><strong>Destination:</strong> ${escapeHtml(stop.destinationAddress || 'Hyderabad Corridor')}</div>
              <div style="font-size:11px; color:#cbd5e1;"><strong>Payload:</strong> ${stop.weightKg || '—'} kg</div>
              <div style="font-size:11px; color:#94a3b8;"><strong>Carrier:</strong> ${escapeHtml(route.vehicleNumber || 'Carrier')}</div>
            </div>
          `)
          .addTo(leafletMap);

        if (isTargetConsignment) {
          selectedStopMarkerToOpen = stopMarker;
        }

        mapLayers.push(stopMarker);
      });

      // Connect polyline
      const polyline = L.polyline(latlngs, {
        color: color,
        weight: 3.5,
        opacity: 0.85,
        dashArray: '6, 6'
      }).addTo(leafletMap);
      mapLayers.push(polyline);
    });

    if (mapLayers.length > 1) {
      leafletMap.fitBounds(bounds, { padding: [50, 50] });
    }

    if (selectedStopMarkerToOpen) {
      setTimeout(() => {
        try { selectedStopMarkerToOpen.openPopup(); } catch (_) {}
      }, 250);
    }
  }

  // --- Ultra-Smooth Multi-Truck Concurrent Fleet Simulation Engine ---
  let fleetSimRunners = [];
  let simLastTimestamp = 0;

  function calculateBearing(lat1, lon1, lat2, lon2) {
    const toRad = Math.PI / 180;
    const toDeg = 180 / Math.PI;
    const dLon = (lon2 - lon1) * toRad;
    const y = Math.sin(dLon) * Math.cos(lat2 * toRad);
    const x = Math.cos(lat1 * toRad) * Math.sin(lat2 * toRad) -
              Math.sin(lat1 * toRad) * Math.cos(lat2 * toRad) * Math.cos(dLon);
    return (Math.atan2(y, x) * toDeg + 360) % 360;
  }

  function generateDenseRouteWaypoints(stops) {
    const densePoints = [];
    const keypoints = [
      { lat: DEPOT_COORDS[0], lon: DEPOT_COORDS[1], label: 'Central Hyderabad Depot', isDepot: true },
      ...stops.map(s => ({
        lat: s.arrivalLatitude || 17.385044,
        lon: s.arrivalLongitude || 78.486671,
        label: s.destinationAddress || `Stop #${s.sequenceOrder}`,
        trackingNumber: s.trackingNumber,
        sequenceOrder: s.sequenceOrder,
        weightKg: s.weightKg
      })),
      { lat: DEPOT_COORDS[0], lon: DEPOT_COORDS[1], label: 'Central Hyderabad Depot', isDepot: true }
    ];

    for (let i = 0; i < keypoints.length - 1; i++) {
      const p1 = keypoints[i];
      const p2 = keypoints[i + 1];
      const steps = 70; // 70 micro-steps between each stop for silky smooth 60fps interpolation

      // Realistic street curve control point offset
      const midLat = (p1.lat + p2.lat) / 2;
      const midLon = (p1.lon + p2.lon) / 2;
      const dLat = p2.lat - p1.lat;
      const dLon = p2.lon - p1.lon;
      const curveMag = ((i % 2 === 0 ? 1 : -1) * 0.05) * Math.sin((i + 1) * 2);
      const ctrlLat = midLat - dLon * curveMag;
      const ctrlLon = midLon + dLat * curveMag;

      for (let s = 0; s < steps; s++) {
        const t = s / steps;
        // Quadratic bezier smoothing
        const lat = (1 - t) * (1 - t) * p1.lat + 2 * (1 - t) * t * ctrlLat + t * t * p2.lat;
        const lon = (1 - t) * (1 - t) * p1.lon + 2 * (1 - t) * t * ctrlLon + t * t * p2.lon;

        densePoints.push({
          lat: lat,
          lon: lon,
          from: p1,
          target: p2,
          isStopArrival: s === steps - 1 && !p2.isDepot,
          stopNumber: p2.sequenceOrder || 0
        });
      }
    }

    return densePoints;
  }

  class VehicleSimRunner {
    constructor(route, colorIndex) {
      this.route = route;
      this.color = ROUTE_COLORS[colorIndex % ROUTE_COLORS.length];
      this.stops = (route.stops || []).slice().sort((a, b) => a.sequenceOrder - b.sequenceOrder);
      this.waypoints = generateDenseRouteWaypoints(this.stops);
      this.currentIdx = (colorIndex * 25) % Math.max(1, this.waypoints.length); // stagger truck starting positions
      this.baseSpeed = 42 + (colorIndex * 6); // km/h
      this.currentSpeed = this.baseSpeed;
      this.bearing = 0;
      this.completedStops = 0;
      this.marker = null;
      this.trailPolyline = null;
      this.traveledPoints = [];
    }

    init(map) {
      if (!this.waypoints.length) return;
      const start = this.waypoints[Math.floor(this.currentIdx)];

      // Create Custom SVG Truck Marker with GPS Beacon Ring
      const truckHtml = `
        <div class="truck-sim-marker" style="color: ${this.color};">
          <div class="truck-halo">
            <span class="gps-radar-ring" style="color: ${this.color};"></span>
            <div class="truck-icon-body" id="truckBody_${this.route.id}" style="border-color: ${this.color};">
              <svg viewBox="0 0 24 24" class="truck-svg" fill="none" stroke="${this.color}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2"/>
                <path d="M15 18H9"/>
                <path d="M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.62l-3.48-4.35A1 1 0 0 0 17.52 8H14v10"/>
                <circle cx="17" cy="18" r="2"/>
                <circle cx="7" cy="18" r="2"/>
              </svg>
            </div>
          </div>
          <div class="truck-label-tag">
            <span>${escapeHtml(this.route.vehicleNumber || 'Carrier #' + this.route.id)}</span>
            <span class="truck-speed-tag font-mono" id="truckSpeedTag_${this.route.id}">${this.currentSpeed} km/h</span>
          </div>
        </div>
      `;

      const icon = L.divIcon({
        className: 'custom-truck-div-icon',
        html: truckHtml,
        iconSize: [42, 54],
        iconAnchor: [21, 27]
      });

      this.marker = L.marker([start.lat, start.lon], { icon: icon }).addTo(map);

      this.marker.bindPopup(`
        <div style="font-family:'Inter',sans-serif; min-width:200px;">
          <strong style="color:${this.color}; font-size:13px;">${escapeHtml(this.route.vehicleNumber || 'Carrier')}</strong>
          <span style="font-size:11px; color:#94a3b8; display:block;">${escapeHtml(this.route.vehicleType || 'Commercial Courier')}</span>
          <hr style="border:none; border-top:1px solid #e2e8f0; margin:6px 0;">
          <div style="font-size:11px; line-height:1.6;">
            <div><strong>Speed:</strong> <span class="font-mono text-emerald" id="popupSpeed_${this.route.id}">${this.currentSpeed} km/h</span></div>
            <div><strong>Payload:</strong> <span class="font-mono">${this.route.totalWeightKg || '0'} kg</span></div>
            <div><strong>Heading:</strong> <span class="font-mono" id="popupHeading_${this.route.id}">${Math.round(this.bearing)}&deg;</span></div>
            <div><strong>Next:</strong> <span id="popupNext_${this.route.id}">${escapeHtml(truncate(start.target?.label || 'Depot', 24))}</span></div>
          </div>
        </div>
      `);

      // Trail Polyline (Breadcrumb path traveled by this vehicle)
      this.traveledPoints = [[start.lat, start.lon]];
      this.trailPolyline = L.polyline(this.traveledPoints, {
        color: this.color,
        weight: 4,
        opacity: 0.95
      }).addTo(map);

      mapLayers.push(this.marker);
      mapLayers.push(this.trailPolyline);
    }

    update(dt, speedMult) {
      if (!this.waypoints.length || !this.marker) return;

      // Advance along dense waypoints
      const stepIncrement = (this.currentSpeed / 40) * speedMult * 0.45;
      this.currentIdx += stepIncrement;

      if (this.currentIdx >= this.waypoints.length - 1) {
        this.currentIdx = 0; // loop back seamlessly to central depot
        this.traveledPoints = [[DEPOT_COORDS[0], DEPOT_COORDS[1]]];
      }

      const idx = Math.floor(this.currentIdx);
      const nextIdx = (idx + 1) % this.waypoints.length;
      const cur = this.waypoints[idx];
      const next = this.waypoints[nextIdx];

      // Calculate smooth bearing rotation
      this.bearing = calculateBearing(cur.lat, cur.lon, next.lat, next.lon);

      // Micro-speed fluctuation for realistic GPS physics
      this.currentSpeed = Math.round(this.baseSpeed + Math.sin(Date.now() / 1500 + this.route.id) * 4);

      // Update Leaflet marker position
      this.marker.setLatLng([cur.lat, cur.lon]);

      // Rotate truck icon towards heading
      const bodyEl = document.getElementById(`truckBody_${this.route.id}`);
      if (bodyEl) {
        bodyEl.style.transform = `rotate(${this.bearing}deg)`;
      }

      // Update speed tag
      const speedTag = document.getElementById(`truckSpeedTag_${this.route.id}`);
      if (speedTag) speedTag.textContent = `${this.currentSpeed} km/h`;

      // Update breadcrumb trail
      if (this.traveledPoints.length > 50) this.traveledPoints.shift();
      this.traveledPoints.push([cur.lat, cur.lon]);
      if (this.trailPolyline) this.trailPolyline.setLatLngs(this.traveledPoints);

      // Return current telemetry snapshot for HUD
      const totalStops = Math.max(1, this.stops.length);
      const targetStopNum = cur.target ? (cur.target.sequenceOrder || 1) : 1;
      const progressPct = Math.min(100, Math.round((idx / this.waypoints.length) * 100));

      return {
        id: this.route.id,
        plate: this.route.vehicleNumber || `TS-09-EV-00${this.route.id}`,
        color: this.color,
        speed: this.currentSpeed,
        bearing: Math.round(this.bearing),
        dest: cur.target ? cur.target.label : 'Central Depot',
        progress: progressPct,
        currentStop: targetStopNum,
        totalStops: totalStops
      };
    }
  }

  // Master Fleet Simulation Loop
  function stopSimulation(cleanVisuals = true) {
    simActive = false;
    if (simAnimId) {
      cancelAnimationFrame(simAnimId);
      simAnimId = null;
    }
    // Remove all animated vehicle markers and breadcrumb polylines from the Leaflet map
    if (cleanVisuals && fleetSimRunners && fleetSimRunners.length) {
      fleetSimRunners.forEach(runner => {
        if (runner.marker && leafletMap) {
          try { leafletMap.removeLayer(runner.marker); } catch (_) {}
          runner.marker = null;
        }
        if (runner.trailPolyline && leafletMap) {
          try { leafletMap.removeLayer(runner.trailPolyline); } catch (_) {}
          runner.trailPolyline = null;
        }
      });
    }
    fleetSimRunners = [];
    const overlay = document.getElementById('simulationTelemetryOverlay');
    if (overlay) overlay.classList.add('hidden');
  }

  function setSimulationFilter(newFilter) {
    activeSimFilter = Object.assign({}, activeSimFilter, newFilter);

    // Sync tab button states
    const tabsContainer = document.getElementById('routeMapFilterTabs');
    if (tabsContainer) {
      tabsContainer.querySelectorAll('.map-tab').forEach(tab => {
        const type = tab.getAttribute('data-filter-type');
        const vNum = tab.getAttribute('data-vehicle');
        if (activeSimFilter.type === 'all' && type === 'all') {
          tab.classList.add('active');
        } else if (activeSimFilter.type === 'vehicle' && type === 'vehicle' && vNum === activeSimFilter.vehicleNumber) {
          tab.classList.add('active');
        } else {
          tab.classList.remove('active');
        }
      });
    }

    // Sync consignment select
    const cSelect = document.getElementById('consignmentFilterSelect');
    if (cSelect) {
      if (activeSimFilter.type === 'consignment' && activeSimFilter.consignmentTracking) {
        cSelect.value = activeSimFilter.consignmentTracking;
      } else if (activeSimFilter.type === 'all') {
        cSelect.value = 'all';
      }
    }

    // Refresh the static map route lines and pins
    renderMapRoutes(routesCache, activeSimFilter);

    // If simulation was running or user selected a filter, smoothly REFRESH the animation immediately!
    if (simActive) {
      runSimulation();
    }
  }

  function runSimulation() {
    if (!routesCache.length || !leafletMap) {
      showToast('No active routes to simulate. Loading executive demo first...', 'error');
      loadExecutiveDemoScenario();
      return;
    }

    // 1. Cleanly stop and wipe all existing simulation markers from map (no ghosts or duplicates!)
    stopSimulation(true);

    // 2. Filter routes according to activeSimFilter
    let routesToSimulate = [];
    if (activeSimFilter.type === 'vehicle' && activeSimFilter.vehicleNumber) {
      routesToSimulate = routesCache.filter(r => r.vehicleNumber === activeSimFilter.vehicleNumber);
    } else if (activeSimFilter.type === 'consignment' && activeSimFilter.consignmentTracking) {
      routesToSimulate = routesCache.filter(r => 
        (r.stops || []).some(s => s.trackingNumber === activeSimFilter.consignmentTracking)
      );
    } else {
      // 'all' routes - deduplicate so each vehicle only has 1 runner
      const seen = new Set();
      routesCache.forEach(r => {
        const key = r.vehicleNumber || `RT-${r.id}`;
        if (!seen.has(key)) {
          seen.add(key);
          routesToSimulate.push(r);
        }
      });
    }

    if (!routesToSimulate.length) {
      routesToSimulate = routesCache.slice(0, 1);
    }

    // 3. Show and title Telemetry Overlay HUD
    const overlay = document.getElementById('simulationTelemetryOverlay');
    if (overlay) overlay.classList.remove('hidden');

    const headerTitle = document.getElementById('simFleetHeaderTitle');
    if (headerTitle) {
      if (activeSimFilter.type === 'vehicle') {
        headerTitle.textContent = `CARRIER ${activeSimFilter.vehicleNumber} TELEMETRY`;
      } else if (activeSimFilter.type === 'consignment') {
        headerTitle.textContent = `TRACKING CONSIGNMENT ${activeSimFilter.consignmentTracking}`;
      } else {
        headerTitle.textContent = `LIVE FLEET TELEMETRY (${routesToSimulate.length} CARRIERS)`;
      }
    }

    // 4. Instantiate runner for each filtered route
    fleetSimRunners = routesToSimulate.map((route, idx) => {
      const runner = new VehicleSimRunner(route, idx);
      runner.init(leafletMap);
      return runner;
    });

    simActive = true;
    simLastTimestamp = performance.now();

    const speedSelect = document.getElementById('simSpeedSelect');

    function fleetStep(now) {
      if (!simActive) return;

      const dt = (now - simLastTimestamp) / 1000;
      simLastTimestamp = now;

      const speedMultiplier = speedSelect ? parseFloat(speedSelect.value) || 2 : 2;

      // Update filtered trucks - filter out null snapshots (runner may return undefined if no waypoints)
      const snapshots = fleetSimRunners
        .map(runner => runner.update(dt, speedMultiplier))
        .filter(s => s != null);

      // Render Multi-Carrier Fleet Telemetry HUD Rows
      const rowsContainer = document.getElementById('simFleetRows');
      if (rowsContainer && snapshots.length) {
        rowsContainer.innerHTML = snapshots.map(s => `
          <div class="sim-fleet-row" style="border-left: 3px solid ${s.color};">
            <div class="sim-fleet-row-top">
              <span class="s-plate">
                <span style="width:7px; height:7px; border-radius:50%; background:${s.color}; flex-shrink:0;"></span>
                <span>${escapeHtml(s.plate)}</span>
              </span>
              <span class="s-speed">${s.speed} km/h &bull; ${s.bearing}&deg;</span>
            </div>
            <div class="sim-fleet-row-mid">
              <span class="s-dest" title="${escapeHtml(s.dest)}">To: ${escapeHtml(truncate(s.dest, 22))}</span>
              <span class="font-mono text-tertiary" style="font-size:10px; white-space:nowrap;">Stop ${s.currentStop}/${s.totalStops}</span>
            </div>
            <div class="sim-progress-track">
              <div class="sim-progress-fill" style="width: ${s.progress}%; background-color: ${s.color};"></div>
            </div>
            <div style="font-size:9.5px; color:var(--text-tertiary); font-family:var(--font-mono);">${s.progress}% complete</div>
          </div>
        `).join('');
      }

      simAnimId = requestAnimationFrame(fleetStep);
    }

    simAnimId = requestAnimationFrame(fleetStep);

    const filterLabel = activeSimFilter.type === 'vehicle' 
      ? `Carrier ${activeSimFilter.vehicleNumber}` 
      : activeSimFilter.type === 'consignment' 
      ? `Consignment ${activeSimFilter.consignmentTracking}` 
      : 'All Fleet Carriers';
    showToast(`Simulation Active: ${filterLabel}`);
  }

  document.getElementById('runSimulationBtn')?.addEventListener('click', runSimulation);
  document.getElementById('resetSimulationBtn')?.addEventListener('click', () => {
    stopSimulation(true);
    setSimulationFilter({ type: 'all', vehicleNumber: null, consignmentTracking: null });
  });

  // --- 12. Routes Loading & Optimization ---
  async function loadRoutes() {
    const container = document.getElementById('routesGridContainer');
    if (!container) return;

    try {
      const routes = await apiRequest('/routes');
      routesCache = routes || [];
      mapRoutesData = routesCache;
      setText('sidebarRoutesBadge', routesCache.length);
      setText('routesCountTag', `${routesCache.length} routes planned`);

      renderRouteMapTabs(routesCache);
      renderMapRoutes(routesCache, activeSimFilter);
      renderRoutesCards(routesCache);
      renderDashboardFleetCapacity(routesCache);
      updateOptimizationHud(routesCache);
    } catch (err) {
      container.innerHTML = `<div class="table-empty text-rose" style="grid-column: 1 / -1;">Failed loading routes: ${escapeHtml(err.message)}</div>`;
    }
  }

  document.getElementById('dashTriggerRoutePlanBtn')?.addEventListener('click', planRoutes);
  document.getElementById('dashDemoScenarioBtn')?.addEventListener('click', loadExecutiveDemoScenario);

  function renderRouteMapTabs(routes) {
    const tabsContainer = document.getElementById('routeMapFilterTabs');
    if (!tabsContainer) return;

    // De-duplicate routes by vehicleNumber so buttons never repeat
    const uniqueRoutes = [];
    const seen = new Set();
    routes.forEach(r => {
      const vKey = r.vehicleNumber || `RT-${r.id}`;
      if (!seen.has(vKey)) {
        seen.add(vKey);
        uniqueRoutes.push(r);
      }
    });

    tabsContainer.innerHTML = `
      <button class="map-tab ${activeSimFilter.type === 'all' ? 'active' : ''}" data-filter-type="all">All Routes</button>
      ${uniqueRoutes.map(r => `
        <button class="map-tab ${activeSimFilter.type === 'vehicle' && activeSimFilter.vehicleNumber === r.vehicleNumber ? 'active' : ''}" 
                data-filter-type="vehicle" 
                data-vehicle="${escapeHtml(r.vehicleNumber || '')}">
          ${escapeHtml(r.vehicleNumber || 'Route #' + r.id)}
        </button>
      `).join('')}
    `;

    tabsContainer.querySelectorAll('.map-tab').forEach(tab => {
      tab.addEventListener('click', () => {
        const type = tab.getAttribute('data-filter-type');
        const vNum = tab.getAttribute('data-vehicle');
        if (type === 'all') {
          setSimulationFilter({ type: 'all', vehicleNumber: null, consignmentTracking: null });
        } else {
          setSimulationFilter({ type: 'vehicle', vehicleNumber: vNum, consignmentTracking: null });
        }
      });
    });

    populateConsignmentSelect(routes);
  }

  function populateConsignmentSelect(routes) {
    const select = document.getElementById('consignmentFilterSelect');
    if (!select) return;

    let options = '<option value="all">📦 All Consignments</option>';
    const seenTracking = new Set();

    routes.forEach(r => {
      (r.stops || []).forEach(s => {
        const trk = s.trackingNumber || `TRK-${s.id}`;
        if (!seenTracking.has(trk)) {
          seenTracking.add(trk);
          const dest = truncate(s.destinationAddress || 'Hyderabad', 20);
          const isSel = (activeSimFilter.type === 'consignment' && activeSimFilter.consignmentTracking === trk) ? 'selected' : '';
          options += `<option value="${escapeHtml(trk)}" ${isSel}>
            ${escapeHtml(trk)} &bull; ${escapeHtml(dest)} (${escapeHtml(r.vehicleNumber || 'Carrier')})
          </option>`;
        }
      });
    });

    select.innerHTML = options;

    select.onchange = function () {
      const val = select.value;
      if (val === 'all') {
        setSimulationFilter({ type: 'all', vehicleNumber: null, consignmentTracking: null });
      } else {
        setSimulationFilter({ type: 'consignment', consignmentTracking: val, vehicleNumber: null });
      }
    };
  }

  function renderRoutesCards(routes) {
    const container1 = document.getElementById('routesGridContainer');
    const container2 = document.getElementById('routesPlanningGridContainer');

    const renderHtml = (rList) => {
      if (!rList.length) {
        return '<div class="table-empty" style="grid-column: 1 / -1;">No active routes planned. Click "Optimize &amp; Plan Routes" or "Executive Demo" above.</div>';
      }

      return rList.map((r, idx) => {
        const stops = (r.stops || []).slice().sort((a, b) => a.sequenceOrder - b.sequenceOrder);
        const color = ROUTE_COLORS[idx % ROUTE_COLORS.length];

        return `
          <div class="route-card">
            <div class="route-card-top">
              <div class="route-card-plate">
                <span style="width:10px; height:10px; border-radius:50%; background:${color};"></span>
                <strong>${escapeHtml(r.vehicleNumber || 'Vehicle #' + r.vehicleId)}</strong>
                <span class="badge badge-subtle">${escapeHtml(r.vehicleType || 'Courier')}</span>
              </div>
              <span class="badge badge-accent">${r.status || 'PLANNED'}</span>
            </div>

            <div class="route-meta-strip">
              <div><i data-lucide="map-pin" class="icon-xs"></i> <strong>${stops.length}</strong> stops</div>
              <div><i data-lucide="navigation" class="icon-xs"></i> <strong>${r.totalDistanceKm || '0'}</strong> km</div>
              <div><i data-lucide="box" class="icon-xs"></i> <strong>${r.totalWeightKg || '0'}</strong> kg</div>
            </div>

            <ul class="route-stops-list">
              ${stops.slice(0, 4).map(s => `
                <li class="route-stop-item">
                  <span class="stop-num">${s.sequenceOrder}</span>
                  <span class="stop-dest" title="${escapeHtml(s.destinationAddress || '')}">${escapeHtml(s.destinationAddress || 'Hyderabad Corridor')}</span>
                  <span class="stop-kg">${s.weightKg || '—'} kg</span>
                </li>
              `).join('')}
              ${stops.length > 4 ? `<li style="font-size:11px; color:var(--text-tertiary); padding-left:28px;">+ ${stops.length - 4} more stops</li>` : ''}
            </ul>

            <div style="display:flex; justify-content:space-between; align-items:center; margin-top:4px;">
              <button class="btn btn-secondary btn-sm" onclick="window.ShipHappensApp.openManifest(${r.id})">
                <i data-lucide="file-text" class="icon-xs"></i>
                <span>Print Manifest</span>
              </button>
              <span class="font-mono text-tertiary" style="font-size:11px;">${escapeHtml(r.routeCode || 'RT-' + r.id)}</span>
            </div>
          </div>
        `;
      }).join('');
    };

    if (container1) container1.innerHTML = renderHtml(routes);
    if (container2) container2.innerHTML = renderHtml(routes);

    refreshIcons();
  }

  function updateOptimizationHud(routes, resultMeta = null) {
    let totalSavedKm = 0;
    let totalCo2Kg = 0;
    let savingsPct = 0;

    if (resultMeta) {
      totalSavedKm = parseFloat(resultMeta.totalDistanceSavedKm) || 0;
      totalCo2Kg = parseFloat(resultMeta.totalCo2SavedKg) || 0;
    } else {
      routes.forEach(r => {
        totalSavedKm += parseFloat(r.distanceSavedKm) || 0;
        totalCo2Kg += parseFloat(r.co2SavedKg) || 0;
      });
    }

    savingsPct = totalSavedKm > 0 ? (totalSavedKm / (totalSavedKm + 45) * 100).toFixed(1) : '0.0';

    setText('hudDistanceSaved', `${totalSavedKm.toFixed(2)} km`);
    setText('hudSavingsPct', `${savingsPct}% tour reduction`);
    setText('hudCo2Saved', `${totalCo2Kg.toFixed(2)} kg CO₂`);
    setText('hudFleetRoutesCount', `${routes.length} Active Routes`);
    setText('hudFleetVehiclesCount', `${routes.length} Vehicles Bin-Packed`);
  }

  // Optimize & Plan Routes Action
  async function planRoutes() {
    try {
      const res = await apiRequest('/routes/plan', { method: 'POST', body: JSON.stringify({}) });
      showToast(`Optimization complete: Created ${res.routesCreated} routes, assigned ${res.shipmentsAssigned} shipments.`);
      await loadRoutes();
      await loadDashboard();
      updateOptimizationHud(res.routes || [], res);
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  document.getElementById('triggerRoutePlanBtn')?.addEventListener('click', planRoutes);
  document.getElementById('refreshRoutesBtn')?.addEventListener('click', loadRoutes);

  // --- 13. Executive Demo Scenario Loader ---
  async function loadExecutiveDemoScenario() {
    const btn = document.getElementById('globalDemoScenarioBtn');
    const prevText = btn ? btn.innerHTML : '';
    if (btn) btn.innerHTML = '<span>Loading...</span>';

    try {
      const res = await apiRequest('/routes/demo-scenario', { method: 'POST' });
      showToast('Executive demo scenario seeded: 13 orders, 3 capacity-constrained vehicles & 2-Opt TSP routes generated!');
      
      navigateTo('routes');
      await loadRoutes();
      await loadDashboard();
      updateOptimizationHud(res.routes || [], res);
    } catch (err) {
      showToast(`Demo loader error: ${err.message}`, 'error');
    } finally {
      if (btn) btn.innerHTML = prevText;
      refreshIcons();
    }
  }

  document.getElementById('globalDemoScenarioBtn')?.addEventListener('click', loadExecutiveDemoScenario);
  document.getElementById('routesDemoScenarioBtn')?.addEventListener('click', loadExecutiveDemoScenario);

  // --- 14. Printable Driver Manifest Generator ---
  function openDriverManifest(routeId) {
    const route = routesCache.find(r => String(r.id) === String(routeId));
    if (!route) {
      showToast('Route manifest not found', 'error');
      return;
    }

    setText('manifestRouteCodeText', route.routeCode || `RT-00${route.id}`);
    setText('manifestVehiclePlate', route.vehicleNumber || 'TS-09-EV-1001');
    setText('manifestVehicleType', route.vehicleType || 'Electric Commercial Carrier');
    setText('manifestPayloadVal', `${route.totalWeightKg || '0.00'} Kg Loaded`);
    setText('manifestDistanceVal', `${route.totalDistanceKm || '0.00'} Km`);
    setText('manifestSavingsVal', route.distanceSavedKm ? `Saved ${route.distanceSavedKm} Km with 2-Opt` : '2-Opt TSP Optimized');
    setText('manifestDateVal', new Date().toISOString().replace('T', ' ').substring(0, 19) + ' UTC');

    const stopsTbody = document.getElementById('manifestStopsTableBody');
    if (stopsTbody) {
      const stops = (route.stops || []).slice().sort((a, b) => a.sequenceOrder - b.sequenceOrder);
      stopsTbody.innerHTML = stops.map(s => `
        <tr>
          <td style="text-align:center;"><strong>${s.sequenceOrder}</strong></td>
          <td><strong class="font-mono">${escapeHtml(s.trackingNumber || '')}</strong></td>
          <td>${escapeHtml(s.destinationAddress || 'Hyderabad Corridor')}</td>
          <td style="text-align:right;"><span class="font-mono">${s.weightKg || '—'} kg</span></td>
          <td style="border-bottom:1px solid #94a3b8; height:24px;"></td>
        </tr>
      `).join('');
    }

    openModal('driverManifestModal');
  }

  document.getElementById('printManifestBtn')?.addEventListener('click', () => {
    window.print();
  });

  // --- 15. Vehicles & Drivers Fleet Data ---
  async function loadVehicles() {
    const tbody = document.getElementById('vehiclesTableBody');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="6" class="table-empty">Loading vehicle fleet...</td></tr>';

    try {
      const list = await apiRequest('/vehicles');
      if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="table-empty">No vehicles registered. Click "Register Vehicle" above.</td></tr>';
        return;
      }

      tbody.innerHTML = list.map(v => `
        <tr>
          <td><strong class="font-mono text-accent">${escapeHtml(v.vehicleNumber)}</strong></td>
          <td>${escapeHtml(v.vehicleType || 'Delivery Van')}</td>
          <td><span class="font-mono">${v.capacityKg} kg</span></td>
          <td><span class="font-mono text-secondary">${v.startLatitude?.toFixed(4)}, ${v.startLongitude?.toFixed(4)}</span></td>
          <td><span class="badge ${v.status === 'AVAILABLE' ? 'badge-accent' : 'badge-subtle'}">${v.status}</span></td>
          <td>
            <button class="btn btn-secondary btn-sm" onclick="window.ShipHappensApp.toggleVehicleStatus(${v.id}, '${v.status}')">
              <span>${v.status === 'AVAILABLE' ? 'Set Maintenance' : 'Set Available'}</span>
            </button>
          </td>
        </tr>
      `).join('');
      refreshIcons();
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="6" class="table-empty text-rose">Failed loading vehicles: ${escapeHtml(err.message)}</td></tr>`;
    }
  }

  const addVehicleModalBtn = document.getElementById('addVehicleModalBtn');
  if (addVehicleModalBtn) addVehicleModalBtn.addEventListener('click', () => openModal('addVehicleModal'));

  const addVehicleForm = document.getElementById('addVehicleForm');
  if (addVehicleForm) {
    addVehicleForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const plate = document.getElementById('newVehiclePlate').value.trim();
      const type = document.getElementById('newVehicleType').value.trim();
      const cap = parseFloat(document.getElementById('newVehicleCapacity').value);

      try {
        await apiRequest('/vehicles', {
          method: 'POST',
          body: JSON.stringify({
            vehicleNumber: plate,
            vehicleType: type,
            capacityKg: cap,
            status: 'AVAILABLE',
            startLatitude: DEPOT_COORDS[0],
            startLongitude: DEPOT_COORDS[1]
          })
        });

        showToast(`Vehicle ${plate} registered`);
        closeModal('addVehicleModal');
        addVehicleForm.reset();
        await loadVehicles();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }

  async function loadDrivers() {
    const tbody = document.getElementById('driversTableBody');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="6" class="table-empty">Loading drivers roster...</td></tr>';

    try {
      const list = await apiRequest('/drivers');
      if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="table-empty">No drivers registered yet.</td></tr>';
        return;
      }

      tbody.innerHTML = list.map(d => `
        <tr>
          <td><strong class="font-mono text-accent">${escapeHtml(d.driverCode || 'DRV-' + d.id)}</strong></td>
          <td><strong>${escapeHtml(d.name)}</strong></td>
          <td><span class="font-mono">${escapeHtml(d.phone || '—')}</span></td>
          <td><span class="font-mono text-secondary">${escapeHtml(d.licenseNumber || '—')}</span></td>
          <td><span class="badge ${d.status === 'AVAILABLE' ? 'badge-accent' : 'badge-subtle'}">${d.status}</span></td>
          <td>
            <button class="btn btn-secondary btn-sm" onclick="window.ShipHappensApp.toggleDriverStatus(${d.id}, '${d.status}')">
              <span>${d.status === 'AVAILABLE' ? 'Off Duty' : 'Set Available'}</span>
            </button>
          </td>
        </tr>
      `).join('');
      refreshIcons();
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="6" class="table-empty text-rose">Failed loading drivers: ${escapeHtml(err.message)}</td></tr>`;
    }
  }

  const addDriverModalBtn = document.getElementById('addDriverModalBtn');
  if (addDriverModalBtn) addDriverModalBtn.addEventListener('click', () => openModal('addDriverModal'));

  const addDriverForm = document.getElementById('addDriverForm');
  if (addDriverForm) {
    addDriverForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const code = document.getElementById('newDriverCode').value.trim();
      const name = document.getElementById('newDriverName').value.trim();
      const phone = document.getElementById('newDriverPhone').value.trim();
      const lic = document.getElementById('newDriverLicense').value.trim();

      try {
        await apiRequest('/drivers', {
          method: 'POST',
          body: JSON.stringify({
            driverCode: code,
            name: name,
            phone: phone,
            licenseNumber: lic,
            status: 'AVAILABLE'
          })
        });

        showToast(`Driver ${name} registered`);
        closeModal('addDriverModal');
        addDriverForm.reset();
        await loadDrivers();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }

  // --- 16. Telemetry & Tracking Timeline ---
  async function fetchTrackingData(shipmentId) {
    const listEl = document.getElementById('eventsTimelineList');
    const cardEl = document.getElementById('telemetryDetailCard');
    if (!listEl) return;

    listEl.innerHTML = '<li class="timeline-empty"><div class="timeline-content">Querying telemetry audit trail...</div></li>';

    try {
      const data = await apiRequest(`/shipments/${shipmentId}/tracking`);
      if (cardEl) {
        cardEl.classList.remove('hidden');
        setText('trackShipmentTitle', `Shipment #${data.shipmentId || shipmentId}`);
        setText('trackTrackingSubtitle', data.trackingNumber || 'TRK-XXXX');
        setText('trackStatusPill', data.currentStatus || 'UNKNOWN');
        setText('trackDestinationVal', data.destinationAddress || 'Hyderabad Corridor');
        setText('trackCustomerVal', data.customerName || 'Enterprise Client');
        setText('trackCreatedVal', data.createdAt ? new Date(data.createdAt).toLocaleString() : '—');
      }

      const events = data.events || [];
      if (!events.length) {
        listEl.innerHTML = '<li class="timeline-empty"><div class="timeline-content">No tracking events recorded yet.</div></li>';
        return;
      }

      listEl.innerHTML = events.map(ev => `
        <li class="event-item">
          <div class="event-dot"></div>
          <div class="event-details">
            <div class="event-headline">
              <strong>${escapeHtml(ev.eventType || 'STATUS_UPDATE')}</strong>
              <span class="font-mono text-tertiary">${ev.timestamp ? new Date(ev.timestamp).toLocaleTimeString() : ''}</span>
            </div>
            <p>${escapeHtml(ev.description || 'State transition verified')}</p>
            ${ev.location ? `<span class="event-location font-mono text-secondary"><i data-lucide="map-pin" class="icon-xs"></i> ${escapeHtml(ev.location)}</span>` : ''}
          </div>
        </li>
      `).join('');
      refreshIcons();
    } catch (err) {
      listEl.innerHTML = `<li class="timeline-empty text-rose"><div class="timeline-content">Telemetry inquiry failed: ${escapeHtml(err.message)}</div></li>`;
    }
  }

  document.getElementById('trackingSearchForm')?.addEventListener('submit', (e) => {
    e.preventDefault();
    const id = document.getElementById('trackingQueryInput')?.value.trim();
    if (id) fetchTrackingData(id);
  });

  // --- 17. B2B Simulator Form ---
  const integForm = document.getElementById('integrationSimForm');
  if (integForm) {
    integForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const output = document.getElementById('integrationJsonOutput');
      if (output) {
        output.classList.remove('hidden');
        output.textContent = 'Transmitting order with X-API-KEY idempotency...';
      }

      const orderRef = document.getElementById('integOrderId').value.trim();
      const custCode = document.getElementById('integCustCode').value.trim();
      const desc = document.getElementById('integItemDesc').value.trim();
      const wt = parseFloat(document.getElementById('integWeight').value);

      try {
        const res = await apiRequest('/integrations/orders', {
          method: 'POST',
          headers: {
            'X-API-KEY': 'SEC-B2B-LIVE-KEY-2026'
          },
          body: JSON.stringify({
            externalOrderId: orderRef,
            customerCode: custCode,
            packageDescription: desc,
            weightKg: wt,
            deliveryAddress: {
              street1: 'Survey 64, HITEC City 2',
              city: 'Hyderabad',
              state: 'Telangana',
              postalCode: '500081',
              country: 'India'
            }
          })
        });

        if (output) output.textContent = JSON.stringify(res, null, 2);
        showToast('External B2B order successfully ingested into pipeline');
        await loadDashboard();
      } catch (err) {
        if (output) output.textContent = `Error: ${err.message}`;
        showToast(err.message, 'error');
      }
    });
  }

  // --- 18. Operator Auth ---
  const authActionBtn = document.getElementById('authActionBtn');
  if (authActionBtn) {
    authActionBtn.addEventListener('click', () => {
      if (authToken) {
        if (confirm('Sign out of Dispatcher session?')) {
          authToken = '';
          currentUser = null;
          localStorage.removeItem('shiphappens_token');
          localStorage.removeItem('shiphappens_user');
          updateOperatorUI();
          showToast('Signed out');
        }
      } else {
        openModal('loginModal');
      }
    });
  }

  const loginForm = document.getElementById('loginForm');
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('loginEmail').value.trim();
      const password = document.getElementById('loginPassword').value;

      try {
        const res = await apiRequest('/auth/login', {
          method: 'POST',
          body: JSON.stringify({ email, password })
        });

        authToken = res.token || res.accessToken || '';
        currentUser = res.user || { name: email.split('@')[0], role: res.role || 'DISPATCHER' };
        localStorage.setItem('shiphappens_token', authToken);
        localStorage.setItem('shiphappens_user', JSON.stringify(currentUser));

        showToast(`Authenticated as ${currentUser.name || 'Operator'}`);
        closeModal('loginModal');
        updateOperatorUI();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }

  function updateOperatorUI() {
    const avatar = document.getElementById('operatorAvatar');
    const name = document.getElementById('operatorName');
    const badge = document.getElementById('operatorRoleBadge');

    if (authToken && currentUser) {
      if (avatar) avatar.textContent = (currentUser.name || 'OP').substring(0, 2).toUpperCase();
      if (name) name.textContent = currentUser.name || 'Dispatcher';
      if (badge) badge.textContent = currentUser.role || 'DISPATCHER';
    } else {
      if (avatar) avatar.textContent = 'OP';
      if (name) name.textContent = 'Guest Dispatcher';
      if (badge) badge.textContent = 'READ ONLY';
    }
  }

  // Utility Helpers
  function truncate(str, max) {
    if (!str) return '';
    return str.length > max ? str.substring(0, max) + '...' : str;
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  // --- 19. Public App Interface ---
  window.ShipHappensApp = {
    openTransition: function (id) {
      const input = document.getElementById('transitionShipmentId');
      if (input) input.value = id;
      openModal('statusTransitionModal');
    },
    trackShipment: function (id) {
      navigateTo('tracking', { shipmentId: id });
    },
    dispatchForCustomer: function (custId) {
      populateCustomerSelect();
      const select = document.getElementById('newShipmentCustomerSelect');
      if (select) select.value = custId;
      openModal('dispatchShipmentModal');
    },
    openManifest: function (routeId) {
      openDriverManifest(routeId);
    },
    toggleVehicleStatus: async function (id, currentStatus) {
      const next = currentStatus === 'AVAILABLE' ? 'MAINTENANCE' : 'AVAILABLE';
      try {
        await apiRequest(`/vehicles/${id}/status`, {
          method: 'PATCH',
          body: JSON.stringify({ status: next })
        });
        showToast(`Vehicle #${id} marked as ${next}`);
        await loadVehicles();
      } catch (err) {
        showToast(err.message, 'error');
      }
    },
    toggleDriverStatus: async function (id, currentStatus) {
      const next = currentStatus === 'AVAILABLE' ? 'OFF_DUTY' : 'AVAILABLE';
      try {
        await apiRequest(`/drivers/${id}/status`, {
          method: 'PATCH',
          body: JSON.stringify({ status: next })
        });
        showToast(`Driver #${id} marked as ${next}`);
        await loadDrivers();
      } catch (err) {
        showToast(err.message, 'error');
      }
    },
    simulateVehicle: function (vehicleNumber) {
      navigateTo('dashboard');
      setTimeout(() => {
        setSimulationFilter({ type: 'vehicle', vehicleNumber: vehicleNumber, consignmentTracking: null });
        runSimulation();
      }, 100);
    },
    simulateConsignment: function (trackingNumber) {
      navigateTo('dashboard');
      setTimeout(() => {
        setSimulationFilter({ type: 'consignment', consignmentTracking: trackingNumber, vehicleNumber: null });
        runSimulation();
      }, 100);
    },
    setFilter: function (filterObj) {
      setSimulationFilter(filterObj);
    }
  };

  // --- 20. Initialization Bootstrap ---
  async function init() {
    updateOperatorUI();
    checkBackendHealth();
    setInterval(checkBackendHealth, 30000);

    // Initial View Resolution
    const initialView = window.location.hash.replace(/^#/, '').trim() || 'dashboard';
    navigateTo(initialView);
    refreshIcons();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})();
