(() => {
  const API_BASE = (window.SKYWASH_API_BASE || 'https://skywash-api.fly.dev').replace(/\/$/, '');
  const pinEl = document.getElementById('opsPin');
  const partnerEl = document.getElementById('opsPartner');
  const unlockBtn = document.getElementById('opsUnlock');
  const refreshBtn = document.getElementById('opsRefresh');
  const statusEl = document.getElementById('opsStatus');
  const errorEl = document.getElementById('opsError');
  const listEl = document.getElementById('opsOrders');
  const liveEl = document.getElementById('opsLive');

  let pin = sessionStorage.getItem('skywash_ops_pin') || '';
  let partnerId = sessionStorage.getItem('skywash_ops_partner') || '';
  let pollTimer = null;

  if (pin) pinEl.value = pin;

  async function api(path, opts = {}) {
    const headers = Object.assign({ 'Content-Type': 'application/json' }, opts.headers || {});
    if (pin) headers['X-Ops-Pin'] = pin;
    const res = await fetch(API_BASE + path, Object.assign({}, opts, { headers }));
    const text = await res.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch (_) {}
    if (!res.ok) {
      const msg = (data && (data.error || data.message)) || text || res.statusText;
      throw new Error(msg);
    }
    return data;
  }

  function setError(msg) {
    errorEl.textContent = msg || '';
  }

  function chipClass(order) {
    if (order.customer_confirmed) return 'chip chip-ok';
    if (order.awaiting_customer_confirm) return 'chip chip-wait';
    return 'chip chip-idle';
  }

  function notifLine(n) {
    const label = n.channel === 'whatsapp' ? 'WhatsApp' : 'Email';
    const st = String(n.status || '').toUpperCase();
    let extra = '';
    if (n.channel === 'whatsapp' && n.deeplink && n.status !== 'sent') {
      extra = ` · <a href="${n.deeplink}" target="_blank" rel="noopener">Open WhatsApp</a>`;
    }
    return `<span class="channel"><b>${label}</b> ${st}${n.target ? ' → ' + n.target : ''}${extra}</span>`;
  }

  function renderOrders(payload) {
    const orders = (payload && payload.orders) || [];
    const partner = (payload && payload.partner) || {};
    statusEl.textContent = partner.name
      ? `${partner.name} · ${partner.city || ''} ${partner.area ? '· ' + partner.area : ''}`
      : '';
    if (!orders.length) {
      listEl.innerHTML = `<div class="empty">No orders for this laundry yet.</div>`;
      return;
    }
    listEl.innerHTML = orders.map(o => {
      const notes = (o.notifications || []).map(notifLine).join('');
      return `
        <article class="order">
          <div class="order-top">
            <h3>${o.service_label || 'Order'} · ${String(o.id || '').slice(0, 8)}</h3>
            <span class="${chipClass(o)}">${o.status_label || o.status}</span>
          </div>
          <p>${o.pickup_address || '—'}<br>
          Total: ${o.currency || 'NGN'} ${o.total != null ? o.total : '—'}</p>
          <div class="channels">${notes || '<span class="channel">No partner alerts yet</span>'}</div>
        </article>`;
    }).join('');
  }

  async function loadPartners() {
    const data = await api('/api/ops/partners');
    const partners = data.partners || [];
    partnerEl.innerHTML = partners.map(p =>
      `<option value="${p.id}">${p.name} (${p.city} · ${p.area})</option>`
    ).join('');
    if (partnerId && partners.some(p => p.id === partnerId)) {
      partnerEl.value = partnerId;
    } else if (partners[0]) {
      partnerId = partners[0].id;
      partnerEl.value = partnerId;
    }
  }

  async function loadOrders() {
    if (!pin || !partnerId) return;
    const data = await api('/api/ops/orders?partner_id=' + encodeURIComponent(partnerId) + '&limit=40');
    renderOrders(data);
    liveEl.textContent = 'Updated ' + new Date().toLocaleTimeString();
  }

  function startPoll() {
    if (pollTimer) clearInterval(pollTimer);
    pollTimer = setInterval(() => {
      loadOrders().catch(() => {});
    }, 4000);
  }

  unlockBtn.addEventListener('click', async () => {
    setError('');
    pin = (pinEl.value || '').trim();
    if (!pin) {
      setError('Enter the ops PIN');
      return;
    }
    unlockBtn.disabled = true;
    try {
      sessionStorage.setItem('skywash_ops_pin', pin);
      await loadPartners();
      partnerId = partnerEl.value;
      sessionStorage.setItem('skywash_ops_partner', partnerId);
      await loadOrders();
      startPoll();
    } catch (err) {
      setError(err.message || 'Could not unlock ops');
    } finally {
      unlockBtn.disabled = false;
    }
  });

  refreshBtn.addEventListener('click', async () => {
    setError('');
    try {
      partnerId = partnerEl.value;
      sessionStorage.setItem('skywash_ops_partner', partnerId);
      await loadOrders();
    } catch (err) {
      setError(err.message || 'Refresh failed');
    }
  });

  partnerEl.addEventListener('change', async () => {
    partnerId = partnerEl.value;
    sessionStorage.setItem('skywash_ops_partner', partnerId);
    try {
      await loadOrders();
    } catch (err) {
      setError(err.message || 'Could not load orders');
    }
  });

  if (pin) {
    unlockBtn.click();
  }
})();
