/* skyWash frontend — consumes Spring Boot API at API_BASE */
// Default '' = same-origin /api via serve.py. Drop stale direct :8080 overrides.
try {
  const stored = localStorage.getItem('skywash_api_base');
  if (stored === 'http://127.0.0.1:8080' || stored === 'http://localhost:8080') {
    localStorage.removeItem('skywash_api_base');
  }
} catch (_) {}
const API_BASE = window.SKYWASH_API_BASE || '';

const NEARBY_RADIUS_KM = 40;

const STATUSES = [
  {key:'confirmed', label:'Request confirmed'},
  {key:'enroute',   label:'Partner heading to you'},
  {key:'pickedup',  label:'Picked up from you'},
  {key:'washing',   label:'Washing at the laundromat'},
  {key:'delivering',label:'Out for delivery'},
  {key:'delivered', label:'Delivered'},
];

let partners = [];
let userLoc = null;
let selectedServices = [{ type:'wash', rate:500, unit:'kg', label:'Wash & Fold' }];
let weight = 3;
let matchedProvider = null;
let currentStatusIdx = 0;
let animFrame = null;
let selectedPayment = { key:'card', name:'Debit / Credit Card' };
let isScheduled = false;
let promoCode = '';
let lastQuote = null;
let currentOrderId = null;
let currentOrderSnapshot = null;
let pollTimer = null;
let lastPolledStatus = null;
let authToken = null;
let currentUser = null;
let quoteTimer = null;
let storeMarkers = {};
let pendingAuthEmail = '';

function getStoredActiveOrderId(){
  try{ return sessionStorage.getItem('skywash_active_order'); }catch(_){ return null; }
}
function setActiveOrderId(id){
  currentOrderId = id || null;
  try{
    if(id) sessionStorage.setItem('skywash_active_order', id);
    else sessionStorage.removeItem('skywash_active_order');
  }catch(_){}
}
let pendingProfileToken = '';
let pendingFromGoogle = false;
let googleClientId = '500952331386-52o07dcf0ujd76134u9aaia4gvjqgamp.apps.googleusercontent.com';
let googleReady = false;
let obStep = 'welcome'; // welcome | email | otp | profile
let authIntent = 'start'; // start | signin | reset

async function api(path, options = {}) {
  const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
  if (authToken) headers.Authorization = 'Bearer ' + authToken;
  let res;
  try {
    res = await fetch(API_BASE + path, Object.assign({}, options, { headers }));
  } catch (networkErr) {
    const target = (API_BASE || location.origin) + path;
    const err = new Error(`Cannot reach API (${target}). Start the backend on :8080.`);
    err.status = 0;
    err.cause = networkErr;
    throw err;
  }
  let data = null;
  const text = await res.text();
  try { data = text ? JSON.parse(text) : null; } catch (_) { data = { raw: text }; }
  if (!res.ok) {
    const msg = (data && data.error) || res.statusText || 'Request failed';
    const err = new Error(msg);
    err.status = res.status;
    err.data = data;
    throw err;
  }
  return data;
}

function loadPaymentPref(){
  try{
    const raw = localStorage.getItem('skywash_payment');
    if(raw) selectedPayment = JSON.parse(raw);
  }catch(e){}
}
function savePaymentPref(){
  try{ localStorage.setItem('skywash_payment', JSON.stringify(selectedPayment)); }catch(e){}
}
function loadAuth(){
  try{ authToken = localStorage.getItem('skywash_token') || null; }catch(e){ authToken = null; }
}
function saveAuth(token){
  authToken = token;
  try{
    if(token) localStorage.setItem('skywash_token', token);
    else localStorage.removeItem('skywash_token');
  }catch(e){}
}
loadPaymentPref();
loadAuth();

function showObStep(step){
  obStep = step;
  ['obWelcome','obEmail','obOtp','obProfile'].forEach(id => {
    const el = document.getElementById(id);
    if(el) el.classList.add('hidden');
  });
  const map = { welcome:'obWelcome', email:'obEmail', otp:'obOtp', profile:'obProfile' };
  const target = document.getElementById(map[step]);
  if(target) target.classList.remove('hidden');
  const focusId = { email:'obEmailInput', otp:'obOtpInput', profile:'obNameInput' }[step];
  if(focusId) setTimeout(() => document.getElementById(focusId)?.focus(), 60);
}

function showOnboarding(step){
  document.getElementById('onboarding').classList.remove('hidden');
  document.getElementById('mainApp').classList.add('hidden');
  showObStep(step || 'welcome');
}

function enterApp(){
  document.getElementById('onboarding').classList.add('hidden');
  document.getElementById('mainApp').classList.remove('hidden');
  renderAuthChip();
  setTimeout(() => { try{ map.invalidateSize(); }catch(_){} }, 80);
}

function userInitials(name){
  const parts = String(name || 'U').trim().split(/\s+/).filter(Boolean);
  if(!parts.length) return 'U';
  if(parts.length === 1) return parts[0].slice(0,2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

function closeAuthMenu(){
  const menu = document.getElementById('authDropdown');
  const btn = document.getElementById('authAvatarBtn');
  if(menu){
    menu.classList.add('hidden');
    menu.style.top = '';
    menu.style.right = '';
    menu.style.left = '';
  }
  if(btn) btn.setAttribute('aria-expanded', 'false');
}

function positionAuthMenu(){
  const menu = document.getElementById('authDropdown');
  const btn = document.getElementById('authAvatarBtn');
  if(!menu || !btn || menu.classList.contains('hidden')) return;
  const rect = btn.getBoundingClientRect();
  const gap = 10;
  const width = Math.min(270, window.innerWidth - 24);
  let top = rect.bottom + gap;
  let left = rect.right - width;
  left = Math.max(12, Math.min(left, window.innerWidth - width - 12));
  // Keep on screen if near bottom
  const estimatedHeight = menu.offsetHeight || 260;
  if(top + estimatedHeight > window.innerHeight - 12){
    top = Math.max(12, rect.top - estimatedHeight - gap);
  }
  menu.style.top = `${Math.round(top)}px`;
  menu.style.left = `${Math.round(left)}px`;
  menu.style.right = 'auto';
}

function toggleAuthMenu(){
  const menu = document.getElementById('authDropdown');
  const btn = document.getElementById('authAvatarBtn');
  if(!menu || !btn) return;
  const willOpen = menu.classList.contains('hidden');
  if(willOpen){
    // Render above map / leaflet panes (body-level fixed layer)
    if(menu.parentElement !== document.body) document.body.appendChild(menu);
    menu.classList.remove('hidden');
    btn.setAttribute('aria-expanded', 'true');
    positionAuthMenu();
  } else {
    closeAuthMenu();
  }
}

function renderAuthChip(){
  const chip = document.getElementById('authChip');
  if(!chip) return;
  // Remove any orphaned body-level dropdown from a previous render
  const orphan = document.getElementById('authDropdown');
  if(orphan && orphan.parentElement === document.body) orphan.remove();

  if(currentUser && currentUser.id && currentUser.id !== 'anon'){
    const name = currentUser.name || 'Account';
    const email = currentUser.email || currentUser.phone || '';
    const initials = userInitials(name);
    chip.innerHTML = `
      <div class="auth-menu-wrap">
        <button type="button" class="auth-avatar-btn" id="authAvatarBtn" aria-haspopup="menu" aria-expanded="false" aria-label="Account menu for ${name}">
          <span class="auth-avatar" aria-hidden="true">${initials}</span>
        </button>
      </div>`;
    // Dropdown lives on body so the map cannot cover it
    const dropdown = document.createElement('div');
    dropdown.className = 'auth-dropdown hidden';
    dropdown.id = 'authDropdown';
    dropdown.setAttribute('role', 'menu');
    dropdown.innerHTML = `
      <div class="auth-dropdown-head">
        <span class="auth-avatar-sm" aria-hidden="true">${initials}</span>
        <div>
          <div class="auth-dropdown-name">${name}</div>
          <div class="auth-dropdown-email">${email}</div>
        </div>
      </div>
      <button type="button" class="auth-dropdown-item" role="menuitem" id="menuProfileBtn">Profile</button>
      <button type="button" class="auth-dropdown-item" role="menuitem" id="menuResetBtn">Reset access</button>
      <button type="button" class="auth-dropdown-item" role="menuitem" id="menuLogoutBtn">Log out</button>
      <button type="button" class="auth-dropdown-item danger" role="menuitem" id="menuLogoutAllBtn">Log out everywhere</button>`;
    document.body.appendChild(dropdown);

    document.getElementById('authAvatarBtn').onclick = (e)=>{ e.stopPropagation(); toggleAuthMenu(); };
    document.getElementById('menuProfileBtn').onclick = ()=>{ closeAuthMenu(); activateTab('account'); };
    document.getElementById('menuResetBtn').onclick = ()=>{ closeAuthMenu(); startResetAccess(); };
    document.getElementById('menuLogoutBtn').onclick = ()=>{
      closeAuthMenu();
      if(confirm('Log out of skyWash on this device?')) logout(false);
    };
    document.getElementById('menuLogoutAllBtn').onclick = ()=>{
      closeAuthMenu();
      if(confirm('Log out on every device? You’ll need a new email code to sign back in.')) logout(true);
    };
    dropdown.addEventListener('click', (e)=> e.stopPropagation());
  } else {
    chip.innerHTML = '';
  }
}

function startResetAccess(){
  if(!confirm('We’ll log you out and send a new login code to your email.')) return;
  const email = currentUser && currentUser.email;
  logout(false).then(() => {
    if(email) document.getElementById('obEmailInput').value = email;
    openEmailStep('reset');
  });
}

async function logout(everywhere){
  try{
    if(authToken){
      await api(everywhere ? '/api/auth/sessions' : '/api/auth/sessions/current', { method:'DELETE' });
    }
  }catch(_){}
  saveAuth(null);
  currentUser = null;
  pendingAuthEmail = '';
  pendingProfileToken = '';
  pendingFromGoogle = false;
  authIntent = 'start';
  renderAuthChip();
  showOnboarding('welcome');
  initGoogleSignIn();
}

async function refreshMe(){
  try{
    const me = await api('/api/account');
    currentUser = me.user || null;
    if(authToken && (!currentUser || currentUser.id === 'anon' || currentUser.demo)){
      saveAuth(null);
      currentUser = null;
    }
    if(currentUser && currentUser.payment_preference && currentUser.payment_preference.key){
      selectedPayment = currentUser.payment_preference;
      savePaymentPref();
      document.querySelectorAll('.pay-option').forEach(o=>{
        o.classList.toggle('selected', o.dataset.pay===selectedPayment.key);
      });
    }
  }catch(_){
    currentUser = null;
  }
}

function isLoggedIn(){
  return !!(authToken && currentUser && currentUser.id && currentUser.id !== 'anon');
}

function hideObErrors(){
  ['obEmailError','obOtpError','obProfileError','obGoogleError'].forEach(id => {
    const el = document.getElementById(id);
    if(el){ el.classList.add('hidden'); el.textContent = ''; }
  });
}

function waitForGoogle(maxMs = 8000){
  return new Promise((resolve, reject) => {
    const start = Date.now();
    const tick = () => {
      if(typeof google !== 'undefined' && google.accounts && google.accounts.id){
        resolve();
        return;
      }
      if(Date.now() - start > maxMs){
        reject(new Error('Google sign-in failed to load. Check your connection and try again.'));
        return;
      }
      setTimeout(tick, 120);
    };
    tick();
  });
}

function ensureGoogleInitialized(){
  if(googleReady || !googleClientId) return;
  google.accounts.id.initialize({
    client_id: googleClientId,
    callback: handleGoogleCredential,
    auto_select: false,
    cancel_on_tap_outside: true
  });
  googleReady = true;
}

async function initGoogleSignIn(){
  try{
    const cfg = await api('/api/auth/google/config');
    if(cfg && cfg.client_id) googleClientId = cfg.client_id;
    // Only hide if backend is explicitly disabled AND we have no public client id fallback
    if(cfg && cfg.enabled === false && !googleClientId){
      const wrap = document.getElementById('obGoogleWrap');
      if(wrap) wrap.hidden = true;
      return;
    }
  }catch(_){}
  if(!googleClientId){
    const wrap = document.getElementById('obGoogleWrap');
    if(wrap) wrap.hidden = true;
    return;
  }
  try{
    await waitForGoogle();
    ensureGoogleInitialized();
    const host = document.getElementById('obGoogleBtn');
    if(!host || host.dataset.rendered === '1') return;
    host.innerHTML = '';
    google.accounts.id.renderButton(host, {
      theme: 'outline',
      size: 'large',
      shape: 'pill',
      text: 'continue_with',
      width: Math.min(320, Math.max(260, host.parentElement?.clientWidth || 280))
    });
    host.dataset.rendered = '1';
  }catch(ex){
    const err = document.getElementById('obGoogleError');
    if(err){
      err.textContent = ex.message || 'Google sign-in unavailable';
      err.classList.remove('hidden');
    }
  }
}

async function handleGoogleCredential(response){
  hideObErrors();
  const err = document.getElementById('obGoogleError');
  try{
    const res = await api('/api/auth/google', {
      method:'POST',
      body: JSON.stringify({ id_token: response.credential })
    });
    if(res.registration_required){
      pendingProfileToken = res.registration_token;
      pendingFromGoogle = true;
      pendingAuthEmail = res.email || '';
      if(res.name) document.getElementById('obNameInput').value = res.name;
      showObStep('profile');
    } else {
      pendingFromGoogle = false;
      saveAuth(res.token);
      currentUser = res.user;
      enterApp();
      activateTab('book');
    }
  }catch(ex){
    if(err){
      err.textContent = ex.message || 'Google sign-in failed';
      err.classList.remove('hidden');
    }
  }
}

function openEmailStep(intent){
  authIntent = intent || 'start';
  hideObErrors();
  const title = document.getElementById('obEmailTitle');
  const copy = document.getElementById('obEmailCopy');
  if(authIntent === 'reset'){
    title.textContent = 'Reset access';
    copy.textContent = 'Enter the email on your account. We’ll send a new login code.';
  } else if(authIntent === 'signin'){
    title.textContent = 'Welcome back';
    copy.textContent = 'Enter your email and we’ll send a one-time login code.';
  } else {
    title.textContent = 'Create your account';
    copy.textContent = 'Enter your email — we’ll send a one-time code. No password needed.';
  }
  showObStep('email');
}

document.getElementById('obGetStarted').onclick = () => openEmailStep('start');
document.getElementById('obSignIn').onclick = () => openEmailStep('signin');
document.getElementById('obForgot').onclick = () => openEmailStep('reset');
document.getElementById('obBackEmail').onclick = () => showObStep('welcome');
document.getElementById('obBackOtp').onclick = () => openEmailStep(authIntent);
document.getElementById('obBackProfile').onclick = () => {
  if(pendingFromGoogle){
    pendingFromGoogle = false;
    pendingProfileToken = '';
    showObStep('welcome');
  } else {
    showObStep('otp');
  }
};

document.getElementById('obEmailForm').addEventListener('submit', async (e)=>{
  e.preventDefault();
  hideObErrors();
  const err = document.getElementById('obEmailError');
  const btn = document.getElementById('obEmailBtn');
  const email = document.getElementById('obEmailInput').value.trim();
  btn.disabled = true;
  try{
    const body = { email };
    if(authIntent === 'reset') body.purpose = 'reset';
    await api('/api/auth/verification-codes', {
      method:'POST',
      body: JSON.stringify(body)
    });
    pendingAuthEmail = email.toLowerCase();
    document.getElementById('obOtpCopy').textContent =
      'We sent a 6-digit code to ' + pendingAuthEmail + '.';
    document.getElementById('obOtpInput').value = '';
    showObStep('otp');
  }catch(ex){
    err.textContent = ex.message || 'Could not send code';
    err.classList.remove('hidden');
  }finally{
    btn.disabled = false;
  }
});

document.getElementById('obResendBtn').onclick = async () => {
  hideObErrors();
  const err = document.getElementById('obOtpError');
  const btn = document.getElementById('obResendBtn');
  btn.disabled = true;
  try{
    const body = { email: pendingAuthEmail };
    if(authIntent === 'reset') body.purpose = 'reset';
    await api('/api/auth/verification-codes', {
      method:'POST',
      body: JSON.stringify(body)
    });
  }catch(ex){
    err.textContent = ex.message || 'Could not resend';
    err.classList.remove('hidden');
  }finally{
    btn.disabled = false;
  }
};

document.getElementById('obOtpForm').addEventListener('submit', async (e)=>{
  e.preventDefault();
  hideObErrors();
  const err = document.getElementById('obOtpError');
  const btn = document.getElementById('obOtpBtn');
  const code = document.getElementById('obOtpInput').value.trim();
  btn.disabled = true;
  try{
    const res = await api('/api/auth/verification-codes/confirmations', {
      method:'POST',
      body: JSON.stringify({ email: pendingAuthEmail, code })
    });
    if(res.registration_required){
      pendingProfileToken = res.registration_token;
      pendingFromGoogle = false;
      showObStep('profile');
    } else {
      saveAuth(res.token);
      currentUser = res.user;
      enterApp();
      activateTab('book');
    }
  }catch(ex){
    err.textContent = ex.message || 'Invalid code';
    err.classList.remove('hidden');
  }finally{
    btn.disabled = false;
  }
});

document.getElementById('obProfileForm').addEventListener('submit', async (e)=>{
  e.preventDefault();
  hideObErrors();
  const err = document.getElementById('obProfileError');
  const btn = document.getElementById('obProfileBtn');
  btn.disabled = true;
  try{
    const res = await api('/api/auth/registrations', {
      method:'POST',
      body: JSON.stringify({
        registration_token: pendingProfileToken,
        name: document.getElementById('obNameInput').value.trim(),
        phone: document.getElementById('obPhoneInput').value.trim()
      })
    });
    saveAuth(res.token);
    currentUser = res.user;
    pendingProfileToken = '';
    pendingFromGoogle = false;
    enterApp();
    activateTab('book');
  }catch(ex){
    err.textContent = ex.message || 'Could not finish signup';
    err.classList.remove('hidden');
  }finally{
    btn.disabled = false;
  }
});

const map = L.map('map', { zoomControl:true }).setView([9.0820, 8.6753], 6);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
  maxZoom:19, attribution:'&copy; OpenStreetMap contributors'
}).addTo(map);

if(navigator.geolocation){
  navigator.geolocation.getCurrentPosition(pos=>{
    map.setView([pos.coords.latitude, pos.coords.longitude], 12);
  }, ()=>{}, {enableHighAccuracy:false, timeout:6000});
}

function pinIcon(color, emoji){
  return L.divIcon({
    className:'',
    html:`<div style="width:30px;height:30px;border-radius:50% 50% 50% 0;background:${color};transform:rotate(-45deg);box-shadow:0 3px 8px rgba(0,0,0,0.3);display:flex;align-items:center;justify-content:center;">
            <div style="transform:rotate(45deg);font-size:12px;">${emoji||''}</div>
          </div>`,
    iconSize:[30,30], iconAnchor:[15,29], popupAnchor:[0,-28]
  });
}
const ICON_STORE = pinIcon('#134f6e','🧺');
const ICON_STORE_ACTIVE = pinIcon('#f5a623','🧺');
const ICON_RIDER = L.divIcon({
  className:'', html:`<div style="width:26px;height:26px;border-radius:50%;background:#0a2230;border:3px solid #ffc93c;box-shadow:0 3px 10px rgba(0,0,0,0.35);display:flex;align-items:center;justify-content:center;font-size:12px;">🚴</div>`,
  iconSize:[26,26], iconAnchor:[13,13]
});

let userMarker=null, riderMarker=null, routeLine=null;

function fmtNaira(n){ return '₦' + Math.round(n).toLocaleString('en-NG'); }

const panelBooking=document.getElementById('panelBooking');
const panelBrowse=document.getElementById('panelBrowse');
const panelHistory=document.getElementById('panelHistory');
const panelAccount=document.getElementById('panelAccount');
const layoutEl = document.querySelector('.layout');
const mapFab = document.getElementById('mapFab');
const mapCloseBtn = document.getElementById('mapCloseBtn');

const PAYMENT_OPTIONS = {
  card: 'Debit / Credit Card',
  transfer: 'Bank Transfer',
  cash: 'Cash on pickup'
};

function activateTab(tabName){
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.toggle('active', b.dataset.tab===tabName));
  [panelBooking,panelBrowse,panelHistory,panelAccount].forEach(p=>p.classList.add('hidden'));
  if(tabName==='book') panelBooking.classList.remove('hidden');
  if(tabName==='browse'){ panelBrowse.classList.remove('hidden'); renderBrowse(); }
  if(tabName==='history'){ panelHistory.classList.remove('hidden'); renderHistory(); }
  if(tabName==='account'){ panelAccount.classList.remove('hidden'); fillAccountForm(); }
  if(tabName==='browse') openMobileMap(); else closeMobileMap();
}
document.querySelectorAll('.nav-btn').forEach(btn=>{
  btn.addEventListener('click', ()=> activateTab(btn.dataset.tab));
});

function fillAccountForm(){
  if(!currentUser) return;
  document.getElementById('acctName').value = currentUser.name || '';
  document.getElementById('acctPhone').value = currentUser.phone || '';
  document.getElementById('acctEmail').value = currentUser.email || '';
  const payKey = (currentUser.payment_preference && currentUser.payment_preference.key) || selectedPayment.key || 'card';
  document.getElementById('acctPayment').value = PAYMENT_OPTIONS[payKey] ? payKey : 'card';
  document.getElementById('acctError').classList.add('hidden');
  document.getElementById('acctOk').classList.add('hidden');
}

document.getElementById('accountForm').addEventListener('submit', async (e)=>{
  e.preventDefault();
  const err = document.getElementById('acctError');
  const ok = document.getElementById('acctOk');
  const btn = document.getElementById('acctSaveBtn');
  err.classList.add('hidden');
  ok.classList.add('hidden');
  btn.disabled = true;
  try{
    const payKey = document.getElementById('acctPayment').value;
    const res = await api('/api/account', {
      method:'PATCH',
      body: JSON.stringify({
        name: document.getElementById('acctName').value.trim(),
        phone: document.getElementById('acctPhone').value.trim(),
        payment_preference: { key: payKey, name: PAYMENT_OPTIONS[payKey] || payKey }
      })
    });
    currentUser = res.user;
    if(currentUser.payment_preference){
      selectedPayment = currentUser.payment_preference;
      savePaymentPref();
      document.querySelectorAll('.pay-option').forEach(o=>{
        o.classList.toggle('selected', o.dataset.pay===selectedPayment.key);
      });
    }
    renderAuthChip();
    ok.classList.remove('hidden');
  }catch(ex){
    err.textContent = ex.message || 'Could not save';
    err.classList.remove('hidden');
  }finally{
    btn.disabled = false;
  }
});

document.getElementById('acctLogoutBtn').onclick = () => {
  if(confirm('Log out of skyWash on this device?')) logout(false);
};
document.getElementById('acctLogoutAllBtn').onclick = () => {
  if(confirm('Log out on every device? You’ll need a new email code to sign back in.')) logout(true);
};
document.getElementById('acctResetAccessBtn').onclick = () => startResetAccess();

document.addEventListener('click', (e)=>{
  const menu = document.getElementById('authDropdown');
  const btn = document.getElementById('authAvatarBtn');
  if(!menu || menu.classList.contains('hidden')) return;
  if(btn && btn.contains(e.target)) return;
  if(menu.contains(e.target)) return;
  closeAuthMenu();
});
document.addEventListener('keydown', (e)=>{
  if(e.key === 'Escape') closeAuthMenu();
});
window.addEventListener('resize', ()=> positionAuthMenu());
window.addEventListener('scroll', ()=> positionAuthMenu(), true);

function openMobileMap(){
  layoutEl.classList.add('map-open');
  setTimeout(()=> map.invalidateSize(), 220);
}
function closeMobileMap(){
  layoutEl.classList.remove('map-open');
}
mapFab.addEventListener('click', openMobileMap);
mapCloseBtn.addEventListener('click', ()=>{
  closeMobileMap();
  const active = document.querySelector('.nav-btn.active');
  if(active && active.dataset.tab==='browse') activateTab('book');
});

function clearMarkers(){
  Object.values(storeMarkers).forEach(m => map.removeLayer(m));
  storeMarkers = {};
}

function placePartnerMarkers(list){
  clearMarkers();
  list.forEach(p=>{
    const m = L.marker([p.lat, p.lng], {icon:ICON_STORE}).addTo(map);
    m.bindPopup(`<p class="popup-title">${p.name}</p><p class="popup-addr">${p.address}</p>`);
    storeMarkers[p.id]=m;
  });
}

async function renderHistory(){
  const list = document.getElementById('historyList');
  list.innerHTML = `<div class="history-empty">Loading orders…</div>`;
  try{
    const data = await api('/api/orders?limit=50');
    const orders = data.orders || [];
    list.innerHTML = '';
    if(!orders.length){
      list.innerHTML = `<div class="history-empty">No orders yet — your completed pickups will show up here.</div>`;
      return;
    }
    orders.forEach(o=>{
      const rating = o.rating || 0;
      const date = o.created_at
        ? new Date(o.created_at).toLocaleDateString('en-NG', {day:'numeric', month:'short', year:'numeric'})
        : '';
      const card = document.createElement('div');
      card.className = 'history-card';
      card.innerHTML = `
        <div class="htop">
          <div><h4>${o.provider_name || 'Partner'}</h4><div class="hdate">${date} · ${o.service_label || ''}</div></div>
          <div class="hprice">${o.total_display || fmtNaira(o.total || 0)}</div>
        </div>
        <div class="hmeta">
          <span>${o.payment || ''} · ${o.status || ''}</span>
          <span class="hstars">${rating ? '★'.repeat(rating)+'☆'.repeat(5-rating) : '—'}</span>
        </div>
        <button class="reorder-btn">Book again</button>`;
      card.querySelector('.reorder-btn').onclick = ()=>{ activateTab('book'); showStep('stepForm'); };
      list.appendChild(card);
    });
  }catch(e){
    list.innerHTML = `<div class="history-empty">Couldn’t load orders: ${e.message}</div>`;
  }
}

function renderBrowse(){
  const term = document.getElementById('browseSearch').value.trim();
  const list = document.getElementById('browseList');
  list.innerHTML = `<div class="history-empty">Loading partners…</div>`;
  const qs = term ? `?q=${encodeURIComponent(term)}` : '';
  api('/api/partners' + qs).then(data=>{
    const rows = data.partners || [];
    list.innerHTML = '';
    if(!rows.length){
      list.innerHTML = `<div class="history-empty">No partners match that search.</div>`;
      return;
    }
    rows.forEach(p=>{
      const card = document.createElement('div');
      card.className = 'card';
      card.innerHTML = `<h3>${p.name}</h3><p class="addr">${p.address}</p>
        <div class="rowb"><span class="rating-chip">${Number(p.rating).toFixed(1)} ★</span>
        <span style="color:var(--ink-soft)">${p.city} · ${p.area}</span></div>`;
      card.onclick = ()=>{
        map.flyTo([p.lat, p.lng], 15, {duration:0.6});
        if(storeMarkers[p.id]) storeMarkers[p.id].openPopup();
      };
      list.appendChild(card);
    });
  }).catch(e=>{
    list.innerHTML = `<div class="history-empty">Couldn’t load partners: ${e.message}</div>`;
  });
}
document.getElementById('browseSearch').addEventListener('input', ()=>{
  clearTimeout(renderBrowse._t);
  renderBrowse._t = setTimeout(renderBrowse, 250);
});

const addrInput=document.getElementById('addrInput');
const geoBtn=document.getElementById('geoBtn');
const locConfirmed=document.getElementById('locConfirmed');
const requestBtn=document.getElementById('requestBtn');

geoBtn.onclick=()=>{
  if(!navigator.geolocation){ alert("Geolocation isn't available in this browser."); return; }
  geoBtn.style.opacity=0.5;
  navigator.geolocation.getCurrentPosition(async pos=>{
    const lat = pos.coords.latitude;
    const lng = pos.coords.longitude;
    let address = `Current location (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
    try{
      const geo = await api('/api/geocode', { method:'POST', body: JSON.stringify({ lat, lng }) });
      if(geo.formatted_address) address = geo.formatted_address;
    }catch(_){ /* keep coordinate fallback */ }
    userLoc = { lat, lng, address };
    addrInput.value = userLoc.address;
    placeUserMarker();
    onLocationSet();
    geoBtn.style.opacity=1;
  }, ()=>{
    alert("Couldn't get your location. Try typing an address instead.");
    geoBtn.style.opacity=1;
  }, {enableHighAccuracy:true, timeout:8000});
};

addrInput.addEventListener('change', async ()=>{
  const address = addrInput.value.trim();
  if(!address) return;
  try{
    const geo = await api('/api/geocode', { method:'POST', body: JSON.stringify({ address }) });
    userLoc = { lat: geo.lat, lng: geo.lng, address: geo.formatted_address || address };
    addrInput.value = userLoc.address;
    placeUserMarker();
    onLocationSet();
  }catch(e){
    alert('Geocode failed: ' + e.message);
  }
});

function placeUserMarker(){
  if(userMarker) map.removeLayer(userMarker);
  userMarker = L.circleMarker([userLoc.lat,userLoc.lng], {radius:9,color:'#fff',weight:3,fillColor:'#0a2230',fillOpacity:1}).addTo(map).bindPopup('Pickup location');
  map.flyTo([userLoc.lat,userLoc.lng], 13, {duration:0.8});
}
function onLocationSet(){
  locConfirmed.classList.add('show');
  requestBtn.disabled=false;
  requestBtn.textContent='Request pickup';
  updateEstimate();
}

function getSelectedServicesFromDOM(){
  return [...document.querySelectorAll('.service-card.selected')].map(card => ({
    type: card.dataset.service,
    rate: parseFloat(card.dataset.rate),
    unit: card.dataset.unit,
    label: card.querySelector('.name').textContent
  }));
}
function syncQuantityLabel(){
  const hasKg = selectedServices.some(s => s.unit === 'kg');
  const hasItem = selectedServices.some(s => s.unit === 'item');
  const label = document.getElementById('weightLabel');
  const sub = document.querySelector('.weight-row .sub');
  if(hasKg && hasItem){
    label.textContent = 'Quantity (kg / items)';
    sub.textContent = 'Applies to each selected service';
  } else if(hasItem){
    label.textContent = 'Number of items';
    sub.textContent = 'How many pieces to dry clean';
  } else {
    label.textContent = 'Estimated weight';
    sub.textContent = 'Roughly 1 laundry bag';
  }
}
function formatServicesLabel(){
  if(lastQuote && lastQuote.label) return lastQuote.label;
  if(!selectedServices.length) return 'No service selected';
  return selectedServices.map(s => `${s.label} · ${weight}${s.unit}`).join(' + ');
}

function renderServiceGrid(services){
  const grid = document.getElementById('serviceGrid');
  grid.innerHTML = '';
  services.forEach((s, i)=>{
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'service-card' + (i===0 ? ' selected' : '');
    btn.setAttribute('role', 'checkbox');
    btn.setAttribute('aria-checked', i===0 ? 'true' : 'false');
    btn.dataset.service = s.type;
    btn.dataset.rate = s.rate;
    btn.dataset.unit = s.unit;
    btn.innerHTML = `<span class="check" aria-hidden="true"></span>
      <span class="icon">${s.icon || '🧺'}</span><span class="name">${s.label}</span>
      <span class="price">${fmtNaira(s.rate)}/${s.unit}</span>`;
    grid.appendChild(btn);
  });
  selectedServices = getSelectedServicesFromDOM();
  syncQuantityLabel();
}

document.getElementById('serviceGrid').addEventListener('click', (e)=>{
  const card = e.target.closest('.service-card');
  if(!card) return;
  e.preventDefault();
  const selectedCount = document.querySelectorAll('.service-card.selected').length;
  if(card.classList.contains('selected')){
    if(selectedCount <= 1) return;
    card.classList.remove('selected');
    card.setAttribute('aria-checked', 'false');
  } else {
    card.classList.add('selected');
    card.setAttribute('aria-checked', 'true');
  }
  selectedServices = getSelectedServicesFromDOM();
  syncQuantityLabel();
  updateEstimate();
});

document.getElementById('wMinus').onclick=()=>{ weight=Math.max(1,weight-1); updateEstimate(); };
document.getElementById('wPlus').onclick=()=>{ weight=Math.min(20,weight+1); updateEstimate(); };

const scheduleNowBtn=document.getElementById('scheduleNowBtn');
const scheduleLaterBtn=document.getElementById('scheduleLaterBtn');
const scheduleTimeRow=document.getElementById('scheduleTimeRow');
const scheduleDateTimeInput=document.getElementById('scheduleDateTime');

function pad2(n){ return String(n).padStart(2,'0'); }
function toDateTimeLocalValue(d){
  return `${d.getFullYear()}-${pad2(d.getMonth()+1)}-${pad2(d.getDate())}T${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}
function roundUpToStep(d, minutes){
  const ms = minutes * 60 * 1000;
  return new Date(Math.ceil(d.getTime() / ms) * ms);
}
function initScheduleDefaults(){
  const soon = roundUpToStep(new Date(Date.now() + 60 * 60 * 1000), 15);
  const max = new Date();
  max.setDate(max.getDate() + 30);
  scheduleDateTimeInput.min = toDateTimeLocalValue(new Date());
  scheduleDateTimeInput.max = toDateTimeLocalValue(max);
  scheduleDateTimeInput.value = toDateTimeLocalValue(soon);
}
initScheduleDefaults();

function getScheduledPickupDate(){
  if(!scheduleDateTimeInput.value) return null;
  const dt = new Date(scheduleDateTimeInput.value);
  return isNaN(dt.getTime()) ? null : dt;
}
function formatScheduledPickup(){
  const dt = getScheduledPickupDate();
  if(!dt) return '—';
  return dt.toLocaleString(undefined, {
    weekday:'short', day:'numeric', month:'short', year:'numeric',
    hour:'numeric', minute:'2-digit'
  });
}
function validateSchedule(){
  if(!isScheduled) return true;
  const dt = getScheduledPickupDate();
  if(!dt){ alert('Please pick a pickup date and time.'); return false; }
  if(dt.getTime() < Date.now() + 15 * 60 * 1000){
    alert('Please choose a time at least 15 minutes from now.');
    return false;
  }
  return true;
}

scheduleNowBtn.onclick=()=>{
  isScheduled=false;
  scheduleNowBtn.classList.add('active');
  scheduleLaterBtn.classList.remove('active');
  scheduleTimeRow.classList.remove('show');
};
scheduleLaterBtn.onclick=()=>{
  isScheduled=true;
  scheduleLaterBtn.classList.add('active');
  scheduleNowBtn.classList.remove('active');
  initScheduleDefaults();
  scheduleTimeRow.classList.add('show');
};

document.getElementById('paymentGrid').addEventListener('click', async (e)=>{
  const opt = e.target.closest('.pay-option');
  if(!opt) return;
  document.querySelectorAll('.pay-option').forEach(o=>o.classList.remove('selected'));
  opt.classList.add('selected');
  selectedPayment = { key:opt.dataset.pay, name:opt.querySelector('.pay-name').textContent };
  savePaymentPref();
  if(authToken){
    try{
      await api('/api/account', {
        method:'PATCH',
        body: JSON.stringify({ payment_preference: selectedPayment })
      });
    }catch(_){ /* optional */ }
  }
});
document.querySelectorAll('.pay-option').forEach(o=>{
  o.classList.toggle('selected', o.dataset.pay===selectedPayment.key);
});

document.getElementById('promoBtn').onclick= async ()=>{
  const code = document.getElementById('promoInput').value.trim().toUpperCase();
  const applied = document.getElementById('promoApplied');
  if(!code){
    promoCode = '';
    applied.classList.remove('show');
    updateEstimate();
    return;
  }
  try{
    const res = await api('/api/promos/validate', { method:'POST', body: JSON.stringify({ code }) });
    applied.classList.add('show');
    if(res.valid){
      promoCode = res.code;
      applied.style.color = '';
      applied.textContent = `✓ ${res.code} applied — ${res.message}`;
    } else {
      promoCode = '';
      applied.style.color = 'var(--warn)';
      applied.textContent = `✕ ${res.message || 'Invalid code'}`;
    }
    updateEstimate();
  }catch(e){
    applied.classList.add('show');
    applied.style.color = 'var(--warn)';
    applied.textContent = '✕ ' + e.message;
  }
};

function updateEstimate(){
  const hasItem = selectedServices.some(s => s.unit === 'item');
  const hasKg = selectedServices.some(s => s.unit === 'kg');
  const unitHint = hasKg && hasItem ? 'qty' : (hasItem ? 'item' : 'kg');
  document.getElementById('weightVal').textContent = unitHint === 'qty' ? `${weight}` : `${weight} ${unitHint}`;

  clearTimeout(quoteTimer);
  quoteTimer = setTimeout(fetchQuote, 200);
}

async function fetchQuote(){
  if(!selectedServices.length) return;
  try{
    const quote = await api('/api/pricing/quote', {
      method:'POST',
      body: JSON.stringify({
        services: selectedServices.map(s => ({ type:s.type, qty: weight })),
        promo_code: promoCode || null
      })
    });
    lastQuote = quote;
    document.getElementById('estBase').textContent = fmtNaira(quote.base_fee);
    document.getElementById('estServiceLabel').textContent = quote.label || formatServicesLabel();
    document.getElementById('estService').textContent = fmtNaira(quote.service_cost);
    document.getElementById('estFee').textContent = fmtNaira(quote.platform_fee);
    document.getElementById('promoRow').style.display = quote.discount > 0 ? 'flex' : 'none';
    document.getElementById('estPromo').textContent = '-' + fmtNaira(quote.discount);
    document.getElementById('estTotal').textContent = fmtNaira(quote.total);
  }catch(e){
    console.warn('quote failed', e);
  }
}

const steps = ['stepForm','stepMatching','stepMatched','stepTrip','stepRating'];
function showStep(id){
  steps.forEach(s=>document.getElementById(s).classList.toggle('hidden', s!==id));
}

let nearbyOffers = [];

function partnerInitials(name){
  return String(name||'').split(' ').map(w=>w[0]).filter(Boolean).slice(0,2).join('');
}
function etaForOffer(offer){
  if(isScheduled) return formatScheduledPickup();
  return `${offer.eta_minutes || 8} min`;
}

requestBtn.onclick = async ()=>{
  if(!userLoc){ alert('Please set a pickup location first.'); return; }
  if(!validateSchedule()) return;
  showStep('stepMatching');
  document.getElementById('liveBadge').classList.add('show');
  document.getElementById('liveText').textContent='Finding partners…';

  try{
    const data = await api(`/api/partners/nearby?lat=${userLoc.lat}&lng=${userLoc.lng}&radius_km=${NEARBY_RADIUS_KM}&limit=8`);
    nearbyOffers = (data.offers || []).map(o => ({
      id: o.partner.id,
      name: o.partner.name,
      city: o.partner.city,
      area: o.partner.area,
      address: o.partner.address,
      lat: o.partner.lat,
      lng: o.partner.lng,
      rating: o.partner.rating,
      phone: o.partner.phone,
      dist: o.distance_km,
      eta_minutes: o.eta_minutes
    }));
    matchedProvider = null;
    showOffers();
  }catch(e){
    alert('Could not find nearby partners: ' + e.message);
    showStep('stepForm');
  }
};

function showOffers(){
  const list = document.getElementById('offerList');
  const confirmBtn = document.getElementById('confirmBtn');
  list.innerHTML = '';
  confirmBtn.disabled = true;
  confirmBtn.textContent = 'Select a partner first';
  document.getElementById('matchEta').textContent = '—';
  document.getElementById('matchTotal').textContent = document.getElementById('estTotal').textContent;
  document.getElementById('liveText').textContent = nearbyOffers.length ? 'Choose a partner' : 'No partners nearby';

  Object.values(storeMarkers).forEach(m => m.setIcon(ICON_STORE));

  if(!nearbyOffers.length){
    list.innerHTML = `<div class="history-empty">No partners within ${NEARBY_RADIUS_KM} km of your pickup. Try another location, or browse the map for cities we cover.</div>`;
    showStep('stepMatched');
    return;
  }

  const pts = nearbyOffers.map(p=>[p.lat,p.lng]);
  pts.push([userLoc.lat, userLoc.lng]);
  map.flyToBounds(L.latLngBounds(pts), {padding:[50,50], duration:0.7});

  nearbyOffers.forEach(p=>{
    const row = document.createElement('button');
    row.type = 'button';
    row.className = 'offer-row';
    row.innerHTML = `
      <div class="provider-avatar">${partnerInitials(p.name)}</div>
      <div class="offer-main">
        <h4>${p.name}</h4>
        <div class="offer-meta">
          <span class="stars">★ ${Number(p.rating).toFixed(1)}</span>
          <span>${p.city} · ${p.area}</span>
        </div>
      </div>
      <div class="offer-eta">
        <div class="mins">${etaForOffer(p)}</div>
        <div class="km">${Number(p.dist).toFixed(1)} km</div>
      </div>`;
    row.onclick = ()=> selectOffer(p, row);
    list.appendChild(row);
  });

  currentOrderSnapshot = {
    serviceLabel: formatServicesLabel(),
    total: document.getElementById('estTotal').textContent,
    payment: selectedPayment.name
  };
  showStep('stepMatched');
}

function selectOffer(p, rowEl){
  if(matchedProvider && storeMarkers[matchedProvider.id]) storeMarkers[matchedProvider.id].setIcon(ICON_STORE);
  matchedProvider = p;
  document.querySelectorAll('.offer-row').forEach(r=> r.classList.remove('selected'));
  rowEl.classList.add('selected');
  if(storeMarkers[p.id]) storeMarkers[p.id].setIcon(ICON_STORE_ACTIVE);
  map.flyToBounds(L.latLngBounds([[userLoc.lat,userLoc.lng],[p.lat,p.lng]]), {padding:[60,60], duration:0.5});
  document.getElementById('matchEta').textContent = etaForOffer(p);
  const confirmBtn = document.getElementById('confirmBtn');
  confirmBtn.disabled = false;
  confirmBtn.textContent = 'Confirm pickup';
}

document.getElementById('cancelMatchBtn').onclick = ()=> resetToForm();
document.getElementById('confirmBtn').onclick = ()=>{
  if(!matchedProvider){ alert('Please select a partner first.'); return; }
  startTrip();
};

function stopPolling(){
  if(pollTimer){ clearInterval(pollTimer); pollTimer = null; }
}

async function resetToForm(){
  stopPolling();
  if(currentOrderId){
    try{ await api('/api/orders/' + currentOrderId + '/cancellations', { method:'POST', body:'{}' }); }catch(_){}
  }
  if(matchedProvider && storeMarkers[matchedProvider.id]) storeMarkers[matchedProvider.id].setIcon(ICON_STORE);
  Object.values(storeMarkers).forEach(m => m.setIcon(ICON_STORE));
  cancelAnimationFrame(animFrame);
  if(riderMarker){ map.removeLayer(riderMarker); riderMarker=null; }
  if(routeLine){ map.removeLayer(routeLine); routeLine=null; }
  document.getElementById('liveBadge').classList.remove('show');
  document.getElementById('chatPanel').classList.remove('show');
  document.getElementById('chatLog').innerHTML='';
  mapFab.classList.remove('live');
  matchedProvider=null;
  nearbyOffers=[];
  currentOrderSnapshot=null;
  setActiveOrderId(null);
  showStep('stepForm');
}
document.getElementById('cancelTripBtn').onclick = ()=> resetToForm();

function buildStepperUI(){
  const track = document.getElementById('stepperTrack');
  track.innerHTML='';
  STATUSES.forEach((s,i)=>{
    const item = document.createElement('div');
    item.className='step-item';
    item.id = 'step-'+s.key;
    item.innerHTML = `<div class="step-dot"></div>
      <div class="step-text"><div class="label">${s.label}</div><div class="time" id="time-${s.key}"></div></div>`;
    track.appendChild(item);
  });
}

function updateStepperUI(statusKey, timeline){
  const idx = STATUSES.findIndex(s => s.key === statusKey);
  currentStatusIdx = idx < 0 ? 0 : idx;
  STATUSES.forEach((s,i)=>{
    const item = document.getElementById('step-'+s.key);
    if(!item) return;
    item.classList.remove('done','current');
    const dot = item.querySelector('.step-dot');
    if(i < currentStatusIdx){ item.classList.add('done'); dot.textContent='✓'; }
    else if(i === currentStatusIdx){ item.classList.add('current'); dot.textContent=''; }
    else { dot.textContent=''; }
  });
  const label = (STATUSES[currentStatusIdx] && STATUSES[currentStatusIdx].label) || statusKey;
  document.getElementById('tripStatusBig').textContent = label;
  (timeline || []).forEach(t=>{
    const el = document.getElementById('time-'+t.key);
    if(el && t.at){
      el.textContent = new Date(t.at).toLocaleTimeString('en-NG',{hour:'2-digit',minute:'2-digit'});
    }
  });
}

async function startTrip(){
  const p = matchedProvider;
  buildStepperUI();
  document.getElementById('tripAvatar').textContent = partnerInitials(p.name);
  document.getElementById('tripName').textContent = p.name;
  document.getElementById('tripStars').textContent = `★ ${Number(p.rating).toFixed(1)}`;
  document.getElementById('liveText').textContent='Creating order…';
  mapFab.classList.add('live');
  showStep('stepTrip');

  const payload = {
    partner_id: p.id,
    pickup: {
      lat: userLoc.lat,
      lng: userLoc.lng,
      address: userLoc.address || addrInput.value || 'Pickup'
    },
    services: selectedServices.map(s => ({ type:s.type, qty: weight })),
    payment_method: selectedPayment.key,
    promo_code: promoCode || null,
    scheduled_at: isScheduled && getScheduledPickupDate() ? getScheduledPickupDate().toISOString() : null
  };

  try{
    const created = await api('/api/orders', { method:'POST', body: JSON.stringify(payload) });
    setActiveOrderId(created.id);
    // Prefer detail endpoint for snake_case consistency
    const detail = await api('/api/orders/' + currentOrderId);
    applyOrderDetail(detail);

    if(selectedPayment.key === 'card' && detail.pricing){
      try{
        const email = (currentUser && currentUser.email)
          ? currentUser.email
          : (currentUser && currentUser.phone)
            ? `${currentUser.phone.replace(/\D/g,'')}@skywash.customer`
            : undefined;
        const pay = await api('/api/payments/checkouts', {
          method:'POST',
          body: JSON.stringify({
            order_id: currentOrderId,
            amount: detail.pricing.total,
            email: email || undefined
          })
        });
        if(pay.authorization_url && !pay.demo){
          document.getElementById('liveText').textContent = 'Redirecting to Paystack…';
          // Same-tab checkout so Paystack callback can restore this trip UI
          location.assign(pay.authorization_url);
          return;
        } else if(pay.authorization_url){
          console.info('Paystack stub', pay);
        }
      }catch(err){
        console.warn('payment init failed', err);
        alert('Payment setup failed: ' + err.message);
      }
    }

    document.getElementById('liveText').textContent='Trip live';
    lastPolledStatus = detail.status;
    stopPolling();
    pollTimer = setInterval(pollOrder, 1500);
    try{
      const msgs = await api('/api/orders/' + currentOrderId + '/messages');
      renderChatMessages(msgs.messages || []);
      document.getElementById('chatPanel').classList.add('show');
    }catch(_){}
  }catch(e){
    alert('Could not create order: ' + e.message);
    showStep('stepMatched');
  }
}

async function resumeTripFromOrder(orderId, opts = {}){
  if(!orderId) return false;
  const detail = await api('/api/orders/' + orderId);
  if(detail.status === 'cancelled' || detail.status === 'rated'){
    setActiveOrderId(null);
    return false;
  }

  setActiveOrderId(orderId);
  currentOrderSnapshot = detail;
  lastPolledStatus = detail.status;
  const partner = detail.partner || {};
  matchedProvider = {
    id: partner.id,
    name: partner.name || detail.provider_name || 'Partner',
    rating: partner.rating != null ? partner.rating : 0,
    lat: partner.lat,
    lng: partner.lng
  };
  if(matchedProvider.id && storeMarkers[matchedProvider.id]){
    storeMarkers[matchedProvider.id].setIcon(ICON_STORE_ACTIVE);
  }

  buildStepperUI();
  document.getElementById('tripAvatar').textContent = partnerInitials(matchedProvider.name);
  document.getElementById('tripName').textContent = matchedProvider.name;
  document.getElementById('tripStars').textContent = matchedProvider.rating
    ? `★ ${Number(matchedProvider.rating).toFixed(1)}`
    : '★ —';
  document.getElementById('liveBadge').classList.add('show');
  mapFab.classList.add('live');
  document.getElementById('liveText').textContent = opts.paid
    ? 'Payment received — trip live'
    : (detail.status_label || 'Trip live');

  if(typeof enterApp === 'function') enterApp();
  activateTab('book');
  showStep('stepTrip');
  applyOrderDetail(detail);

  stopPolling();
  if(detail.status !== 'delivered' && detail.status !== 'rated'){
    pollTimer = setInterval(pollOrder, 1500);
  }

  // Always load assist thread on trip restore
  try{
    const data = await api('/api/orders/' + currentOrderId + '/messages');
    renderChatMessages(data.messages || []);
    if(data.order) syncTripFromOrder(data.order);
  }catch(_){}
  if(opts.openChat !== false){
    document.getElementById('chatPanel').classList.add('show');
  }
  if(detail.status === 'delivered'){
    setTimeout(showRating, 400);
    return true;
  }
  return true;
}

async function pollOrder(){
  if(!currentOrderId) return;
  try{
    const detail = await api('/api/orders/' + currentOrderId);
    const prev = lastPolledStatus;
    syncTripFromOrder(detail);
    if(detail.status !== prev){
      try{
        const data = await api('/api/orders/' + currentOrderId + '/messages');
        renderChatMessages(data.messages || []);
      }catch(_){}
    }
  }catch(e){
    console.warn('poll failed', e);
  }
}

function syncTripFromOrder(detail){
  if(!detail || !detail.id) return;
  const statusChanged = detail.status !== lastPolledStatus;
  applyOrderDetail(detail);
  document.getElementById('liveText').textContent = detail.status_label || detail.status || 'Trip live';
  lastPolledStatus = detail.status;

  if(statusChanged && (detail.status === 'enroute' || detail.status === 'delivering')){
    openMobileMap();
  }

  if(detail.status === 'delivered'){
    stopPolling();
    setTimeout(showRating, 600);
  } else if(detail.status === 'rated' || detail.status === 'cancelled'){
    stopPolling();
    setActiveOrderId(null);
  }
}

function applyOrderDetail(detail){
  currentOrderSnapshot = detail;
  if(detail.pickup && detail.pickup.lat != null){
    userLoc = {
      lat: detail.pickup.lat,
      lng: detail.pickup.lng,
      address: detail.pickup.address || (userLoc && userLoc.address) || ''
    };
  }

  updateStepperUI(detail.status, detail.timeline);
  document.getElementById('tripEta').textContent = detail.eta_label || '—';
  document.getElementById('tripStatusBig').textContent = detail.status_label || detail.status;

  const partner = detail.partner || matchedProvider || {};
  if(partner.name) document.getElementById('tripName').textContent = partner.name;
  if(partner.rating != null) document.getElementById('tripStars').textContent = `★ ${Number(partner.rating).toFixed(1)}`;

  const loc = detail.partner_location;
  if(loc && (detail.status === 'enroute' || detail.status === 'delivering')){
    const destination = userLoc || (partner.lat != null ? partner : matchedProvider);
    animateRiderToward(loc, partner.lat != null ? partner : matchedProvider, destination);
  }
  if(detail.status === 'pickedup' || detail.status === 'washing'){
    if(riderMarker){ map.removeLayer(riderMarker); riderMarker=null; }
    if(routeLine){ map.removeLayer(routeLine); routeLine=null; }
    if(partner.lat != null && partner.lng != null){
      map.flyTo([partner.lat, partner.lng], 14, { duration: 0.5 });
    }
  }
  if(detail.status === 'delivered' && userLoc){
    if(riderMarker){ map.removeLayer(riderMarker); riderMarker=null; }
    riderMarker = L.marker([userLoc.lat, userLoc.lng], {icon:ICON_RIDER}).addTo(map);
  }
}

function animateRiderToward(fromPoint, partnerPoint, toPoint){
  if(!fromPoint || fromPoint.lat == null) return;
  const from = [fromPoint.lat, fromPoint.lng];
  const dest = toPoint && toPoint.lat != null
    ? [toPoint.lat, toPoint.lng]
    : (partnerPoint && partnerPoint.lat != null ? [partnerPoint.lat, partnerPoint.lng] : from);
  if(riderMarker) map.removeLayer(riderMarker);
  if(routeLine) map.removeLayer(routeLine);
  riderMarker = L.marker(from, {icon:ICON_RIDER}).addTo(map);
  routeLine = L.polyline([from, dest], {color:'#f5a623', weight:3, dashArray:'6,8', opacity:0.85}).addTo(map);
  try{
    map.flyToBounds(L.latLngBounds([from, dest]), {padding:[80,80], duration:0.35, maxZoom:15});
  }catch(_){
    map.setView(from, 14);
  }
}

let selectedRatingVal = 5;
function showRating(){
  document.getElementById('ratingProviderName').textContent = (matchedProvider && matchedProvider.name) || 'your provider';
  selectedRatingVal = 5;
  document.querySelectorAll('#starsInput button').forEach(b=>b.classList.add('filled'));
  showStep('stepRating');
}
document.getElementById('starsInput').addEventListener('click',(e)=>{
  const btn = e.target.closest('button');
  if(!btn) return;
  selectedRatingVal = parseInt(btn.dataset.star, 10);
  document.querySelectorAll('#starsInput button').forEach(b=>{
    b.classList.toggle('filled', parseInt(b.dataset.star, 10)<=selectedRatingVal);
  });
});
document.getElementById('doneBtn').onclick = async ()=>{
  if(currentOrderId){
    try{
      await api('/api/orders/' + currentOrderId + '/ratings', {
        method:'POST',
        body: JSON.stringify({ rating: selectedRatingVal })
      });
    }catch(e){
      alert('Could not save rating: ' + e.message);
    }
  }
  if(matchedProvider && storeMarkers[matchedProvider.id]) storeMarkers[matchedProvider.id].setIcon(ICON_STORE);
  document.getElementById('liveBadge').classList.remove('show');
  document.getElementById('chatPanel').classList.remove('show');
  document.getElementById('chatLog').innerHTML='';
  mapFab.classList.remove('live');
  matchedProvider=null;
  currentOrderSnapshot=null;
  setActiveOrderId(null);
  promoCode='';
  document.getElementById('promoApplied').classList.remove('show');
  document.getElementById('promoInput').value='';
  updateEstimate();
  showStep('stepForm');
};

const chatToggleBtn=document.getElementById('chatToggleBtn');
const chatPanel=document.getElementById('chatPanel');
const chatLog=document.getElementById('chatLog');

function chatWho(sender){
  if(sender === 'customer') return 'me';
  if(sender === 'system') return 'system';
  return 'them'; // assist | partner
}

function renderChatMessages(messages){
  chatLog.innerHTML = '';
  (messages || []).forEach(m => addChatMsg(m.text, chatWho(m.sender)));
}

async function refreshChatQuiet(){
  if(!currentOrderId) return;
  try{
    const data = await api('/api/orders/' + currentOrderId + '/messages');
    renderChatMessages(data.messages || []);
    if(data.order) syncTripFromOrder(data.order);
  }catch(_){}
}

chatToggleBtn.onclick= async ()=>{
  chatPanel.classList.toggle('show');
  if(chatPanel.classList.contains('show') && currentOrderId){
    await refreshChatQuiet();
  }
};
document.getElementById('chatQuick').addEventListener('click', async (e)=>{
  const btn = e.target.closest('button');
  if(!btn || !currentOrderId) return;
  try{
    const data = await api('/api/orders/' + currentOrderId + '/messages', {
      method:'POST',
      body: JSON.stringify({ text: btn.dataset.msg, sender: 'customer' })
    });
    renderChatMessages(data.messages || []);
    if(data.order) syncTripFromOrder(data.order);
    chatPanel.classList.add('show');
  }catch(err){
    alert('Chat failed: ' + err.message);
  }
});
function addChatMsg(text, who){
  const div = document.createElement('div');
  div.className = 'chat-msg ' + who;
  div.textContent = text;
  chatLog.appendChild(div);
  chatLog.scrollTop = chatLog.scrollHeight;
}

async function boot(){
  showStep('stepForm');
  initGoogleSignIn();
  try{
    await api('/api/health');
  }catch(e){
    const apiHint = API_BASE || '(same origin)';
    alert(`API not reachable at ${apiHint}. For local: run the backend and python3 serve.py. For Vercel: use https://sudsnear-deploy.vercel.app and ensure Railway CORS includes that origin.`);
    return;
  }

  const payParams = new URLSearchParams(location.search);
  const paymentRef = payParams.get('payment') === 'callback' ? payParams.get('reference') : null;
  let paymentVerified = null;

  try{
    const svc = await api('/api/services');
    renderServiceGrid(svc.services || []);
    updateEstimate();
  }catch(e){
    console.warn('services load failed', e);
    syncQuantityLabel();
    updateEstimate();
  }

  try{
    const data = await api('/api/partners');
    partners = data.partners || [];
    placePartnerMarkers(partners);
  }catch(e){
    console.warn('partners load failed', e);
  }

  await refreshMe();
  if(isLoggedIn()){
    enterApp();
  } else {
    showOnboarding('welcome');
    initGoogleSignIn();
  }

  // Paystack return: verify, then restore trip + Message provider UI
  if(paymentRef){
    try{
      paymentVerified = await api('/api/payments/verifications', {
        method:'POST',
        body: JSON.stringify({ reference: paymentRef })
      });
      history.replaceState({}, '', location.pathname);
      const orderId = paymentVerified.order_id
        || (paymentVerified.order && paymentVerified.order.id)
        || getStoredActiveOrderId();
      if(orderId){
        await resumeTripFromOrder(orderId, {
          paid: paymentVerified.status === 'success',
          openChat: true
        });
        if(paymentVerified.status === 'success'){
          document.getElementById('liveText').textContent =
            'Payment successful' + (paymentVerified.amount ? ` — ₦${paymentVerified.amount}` : '') + ' · trip in progress';
        }
      } else if(paymentVerified.status === 'success'){
        alert('Payment successful' + (paymentVerified.amount ? ` — ₦${paymentVerified.amount}` : '') + ', but the order could not be restored. Check My orders.');
      } else {
        alert('Payment status: ' + (paymentVerified.status || 'unknown'));
      }
      return;
    }catch(e){
      console.warn('payment verify failed', e);
      history.replaceState({}, '', location.pathname);
    }
  }

  // Resume in-progress trip after refresh (e.g. mid-pickup)
  const activeId = getStoredActiveOrderId();
  if(activeId && isLoggedIn()){
    try{
      await resumeTripFromOrder(activeId, { openChat: false });
    }catch(e){
      console.warn('resume trip failed', e);
      setActiveOrderId(null);
    }
  }
}

boot();
