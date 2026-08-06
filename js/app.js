const LAUNDRIES = [
  // Lagos
  {name:"WashRyte Laundry Service", city:"Lagos", area:"Lekki", address:"The Lennox Mall, Phase 1 Admiralty Wy, Lekki Phase 1, Lagos", lat:6.4390679, lng:3.4555214, rating:4.1, phone:"+2348059303818"},
  {name:"The Ultimate Standard Laundry And Cleaning", city:"Lagos", area:"Ikoyi", address:"Falomo Roundabout, Bourdillon Rd, Ikoyi, Lagos", lat:6.444105, lng:3.4279112, rating:5.0, phone:"+2347019248933"},
  {name:"Get It Right Laundry Services", city:"Lagos", area:"Ikoyi", address:"33 Turnbull Rd, Ikoyi, Lagos", lat:6.4563677, lng:3.4470903, rating:5.0, phone:"+2348083698659"},
  {name:"Kenza Laundry & Dry Cleaning Services", city:"Lagos", area:"Ikoyi", address:"109A Awolowo Rd, Ikoyi, Lagos", lat:6.4441325, lng:3.4207871, rating:5.0, phone:"+2347069178360"},
  {name:"Payless Laundry Services", city:"Lagos", area:"Victoria Island", address:"11 Sir Samuel Manuwa St, Victoria Island, Lagos", lat:6.4369464, lng:3.4355656, rating:3.1, phone:"+2348097208338"},
  {name:"Ace Wash N Dry", city:"Lagos", area:"Ikeja", address:"Kudirat Abiola Way, Oregun, Ikeja, Lagos", lat:6.6031369, lng:3.362752, rating:4.8, phone:"+2349057184682"},
  {name:"Washyard Laundromat | Allen", city:"Lagos", area:"Ikeja", address:"47 Allen Ave, Allen, Ikeja, Lagos", lat:6.6011041, lng:3.3521018, rating:4.5, phone:"+2347025650057"},
  {name:"LaunderLand Dry Cleaners", city:"Lagos", area:"Ikeja", address:"15 Toyin St, Allen, Ikeja, Lagos", lat:6.5965116, lng:3.3488582, rating:4.8, phone:"+2348144174436"},
  {name:"True Wash Laundromat Akoka", city:"Lagos", area:"Yaba", address:"5/7 St Finbarr's College Rd, Akoka, Lagos", lat:6.5244543, lng:3.3855239, rating:5.0, phone:"+2348037905707"},
  {name:"Aroaic Laundry & Dry Cleaning Services", city:"Lagos", area:"Ikeja", address:"14/16 Ladipo Kuku St, Allen, Ikeja, Lagos", lat:6.5999076, lng:3.3521886, rating:4.0, phone:"+2348034546161"},
  {name:"Dee Clean Laundry Lekki", city:"Lagos", area:"Lekki", address:"1 Kayode Otitoju St, Lekki Phase 1, Lagos", lat:6.4505182, lng:3.4707559, rating:4.9, phone:"+2349117266758"},
  {name:"Laundry Care Lekki", city:"Lagos", area:"Lekki", address:"1A Kayode Otitoju St, Eti-Osa, Lagos", lat:6.450511, lng:3.4704056, rating:5.0, phone:"+2349072564972"},
  {name:"Wasche Point Laundry Service & Dry Cleaner", city:"Lagos", area:"Lekki", address:"Plot 12 Emma Abimbola Cole, off Fola Osibo Rd, Lekki Phase I, Lagos", lat:6.4424118, lng:3.4782121, rating:4.4, phone:"+2348188882013"},
  {name:"LaundrybyTIMESIGNATURE", city:"Lagos", area:"Lekki", address:"6B Admiralty Rd, Lekki Phase 1, Lagos", lat:6.4571233, lng:3.4709205, rating:4.8, phone:"+23412919486"},
  {name:"GozzyCee Laundry Services", city:"Lagos", area:"Surulere", address:"60 Sanya St, Surulere, Lagos", lat:6.4871993, lng:3.3324632, rating:5.0, phone:"+2348160843136"},
  {name:"Alpha's Touch Laundry Service", city:"Lagos", area:"Surulere", address:"4 Tafawa Balewa Cres, off Adeniran Ogunsanya, Surulere, Lagos", lat:6.4951617, lng:3.3568973, rating:4.9, phone:"+2348055472703"},
  {name:"Quickwash Laundromat", city:"Lagos", area:"Surulere", address:"62 Adeniran Ogunsanya St, Surulere, Lagos", lat:6.4944357, lng:3.3568915, rating:4.9, phone:"+2347076130688"},
  {name:"D-way Laundry", city:"Lagos", area:"Surulere", address:"2 Tayo-Oyefeko St, off Shaki Crescent, Surulere, Lagos", lat:6.4972416, lng:3.3389073, rating:5.0, phone:"+2349082933639"},
  {name:"Snowyclean Laundromat", city:"Lagos", area:"Surulere", address:"Adeniran Ogunsanya Mall (ShopRite), Surulere, Lagos", lat:6.4909473, lng:3.3568883, rating:3.8, phone:null},
  {name:"Renee Laundromat and Dry Cleaning", city:"Lagos", area:"Yaba", address:"44 Olonode St, Alagomeji, Yaba, Lagos", lat:6.4987637, lng:3.3779721, rating:5.0, phone:"+2348126328691"},
  {name:"EzWashnDry Laundromat", city:"Lagos", area:"Yaba", address:"E-Centre (Ozone Cinemas), Commercial Ave, Sabo Yaba, Lagos", lat:6.5062713, lng:3.3743661, rating:4.4, phone:"+2348148728762"},
  {name:"Astra Cleaners Ltd", city:"Lagos", area:"Yaba", address:"300 Herbert Macaulay Wy, Yaba, Lagos", lat:6.5047348, lng:3.3780282, rating:4.9, phone:"+2349090030003"},
  {name:"Skywhite Drycleaners, Laundry & Cleaning", city:"Lagos", area:"Yaba", address:"2 Ogabi St, Abule Ijesha Rd, Yaba, Lagos", lat:6.5219455, lng:3.379573, rating:5.0, phone:"+2348025653564"},
  {name:"Your Laundry Guy", city:"Lagos", area:"Yaba", address:"Yaba-Onike Rd, Yaba, Lagos", lat:6.5058386, lng:3.3779722, rating:4.0, phone:"+2348120931602"},
  // Abuja
  {name:"Capital Wash Hub", city:"Abuja", area:"Wuse", address:"Plot 2147 Aminu Kano Cres, Wuse II, Abuja", lat:9.0765, lng:7.3986, rating:4.7, phone:"+2348011110001"},
  {name:"Maitama Fresh Laundry", city:"Abuja", area:"Maitama", address:"Aguiyi Ironsi St, Maitama, Abuja", lat:9.0882, lng:7.4951, rating:4.9, phone:"+2348011110002"},
  {name:"Garki Clean Express", city:"Abuja", area:"Garki", address:"Area 3, Garki, Abuja", lat:9.0354, lng:7.4832, rating:4.5, phone:"+2348011110003"},
  {name:"Asokoro Press & Fold", city:"Abuja", area:"Asokoro", address:"Yakubu Gowon Cres, Asokoro, Abuja", lat:9.0418, lng:7.5146, rating:4.8, phone:"+2348011110004"},
  // Port Harcourt
  {name:"Garden City Laundry", city:"Port Harcourt", area:"GRA", address:"Tombia St, GRA Phase 2, Port Harcourt", lat:4.8241, lng:7.0336, rating:4.6, phone:"+2348022220001"},
  {name:"Trans Amadi Wash Co", city:"Port Harcourt", area:"Trans Amadi", address:"Trans Amadi Industrial Layout, Port Harcourt", lat:4.8156, lng:7.0498, rating:4.4, phone:"+2348022220002"},
  {name:"Rumuola Quick Clean", city:"Port Harcourt", area:"Rumuola", address:"Rumuola Rd, Port Harcourt", lat:4.8472, lng:7.0169, rating:4.8, phone:"+2348022220003"},
];

const NEARBY_RADIUS_KM = 40;

const STATUSES = [
  {key:'confirmed', label:'Request confirmed', dur:2500},
  {key:'enroute',   label:'Partner heading to you', dur:5000},
  {key:'pickedup',  label:'Picked up from you', dur:2500},
  {key:'washing',   label:'Washing at the laundromat', dur:5000},
  {key:'delivering',label:'Out for delivery', dur:5000},
  {key:'delivered', label:'Delivered', dur:0},
];

let userLoc = null;
let selectedServices = [
  { type:'wash', rate:500, unit:'kg', label:'Wash & Fold' }
];
let weight = 3;
let matchedProvider = null;
let currentStatusIdx = 0;
let tripTimer = null;
let animFrame = null;
let selectedPayment = { key:'card', name:'Debit / Credit Card' };
let isScheduled = false;
let promoDiscount = 0;
let orderHistory = [];
let currentOrderSnapshot = null;
const PROVIDER_REPLIES = [
  "Got it, thanks!",
  "On it 👍",
  "No problem, will do.",
  "Sure thing, see you shortly."
];

// ---- Persistence (localStorage) ----
function saveHistory(){
  try{ localStorage.setItem('skywash_orders', JSON.stringify(orderHistory)); }catch(e){}
}
function loadHistory(){
  try{
    const raw = localStorage.getItem('skywash_orders');
    orderHistory = raw ? JSON.parse(raw) : [];
  }catch(e){ orderHistory = []; }
}
function savePaymentPref(){
  try{ localStorage.setItem('skywash_payment', JSON.stringify(selectedPayment)); }catch(e){}
}
function loadPaymentPref(){
  try{
    const raw = localStorage.getItem('skywash_payment');
    if(!raw) return;
    selectedPayment = JSON.parse(raw);
  }catch(e){}
}
loadHistory();
loadPaymentPref();

const map = L.map('map', { zoomControl:true }).setView([9.0820, 8.6753], 6); // Nigeria overview until location is known
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
  maxZoom:19, attribution:'&copy; OpenStreetMap contributors'
}).addTo(map);

// Center on the user when possible (any city)
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

let storeMarkers = {};
LAUNDRIES.forEach((l,i)=>{
  const m = L.marker([l.lat,l.lng], {icon:ICON_STORE}).addTo(map);
  m.bindPopup(`<p class="popup-title">${l.name}</p><p class="popup-addr">${l.address}</p>`);
  storeMarkers[i]=m;
});

let userMarker=null, riderMarker=null, routeLine=null;

function haversine(lat1,lng1,lat2,lng2){
  const R=6371, dLat=(lat2-lat1)*Math.PI/180, dLng=(lng2-lng1)*Math.PI/180;
  const a=Math.sin(dLat/2)**2+Math.cos(lat1*Math.PI/180)*Math.cos(lat2*Math.PI/180)*Math.sin(dLng/2)**2;
  return R*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
}
function fmtNaira(n){ return '₦' + Math.round(n).toLocaleString('en-NG'); }

const panelBooking=document.getElementById('panelBooking'), panelBrowse=document.getElementById('panelBrowse'), panelHistory=document.getElementById('panelHistory');
const layoutEl = document.querySelector('.layout');
const mapFab = document.getElementById('mapFab');
const mapCloseBtn = document.getElementById('mapCloseBtn');

function activateTab(tabName){
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.toggle('active', b.dataset.tab===tabName));
  [panelBooking,panelBrowse,panelHistory].forEach(p=>p.classList.add('hidden'));
  if(tabName==='book') panelBooking.classList.remove('hidden');
  if(tabName==='browse'){ panelBrowse.classList.remove('hidden'); renderBrowse(); }
  if(tabName==='history'){ panelHistory.classList.remove('hidden'); renderHistory(); }

  // on mobile, "browse" opens the full-screen map directly since that IS the map view
  if(tabName==='browse'){
    openMobileMap();
  } else {
    closeMobileMap();
  }
}
document.querySelectorAll('.nav-btn').forEach(btn=>{
  btn.addEventListener('click', ()=> activateTab(btn.dataset.tab));
});

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
  // return to whichever sidebar panel was last relevant — default back to book
  const active = document.querySelector('.nav-btn.active');
  if(active && active.dataset.tab==='browse'){ activateTab('book'); }
});

function renderHistory(){
  const list = document.getElementById('historyList');
  list.innerHTML='';
  if(orderHistory.length===0){
    list.innerHTML = `<div class="history-empty">No orders yet — your completed pickups will show up here.</div>`;
    return;
  }
  [...orderHistory].reverse().forEach(o=>{
    const card=document.createElement('div');
    card.className='history-card';
    card.innerHTML = `
      <div class="htop">
        <div><h4>${o.providerName}</h4><div class="hdate">${o.date} · ${o.serviceLabel}</div></div>
        <div class="hprice">${o.total}</div>
      </div>
      <div class="hmeta">
        <span>${o.payment}</span>
        <span class="hstars">${'★'.repeat(o.rating)}${'☆'.repeat(5-o.rating)}</span>
      </div>
      <button class="reorder-btn" data-idx="${orderHistory.indexOf(o)}">Book again</button>
    `;
    card.querySelector('.reorder-btn').onclick=()=>{
      activateTab('book'); showStep('stepForm');
    };
    list.appendChild(card);
  });
}

function renderBrowse(){
  const term = document.getElementById('browseSearch').value.toLowerCase();
  const list = document.getElementById('browseList');
  list.innerHTML='';
  LAUNDRIES.filter(l=>
    l.name.toLowerCase().includes(term) ||
    l.area.toLowerCase().includes(term) ||
    l.city.toLowerCase().includes(term)
  ).forEach((l)=>{
    const idx = LAUNDRIES.indexOf(l);
    const card=document.createElement('div');
    card.className='card';
    card.innerHTML=`<h3>${l.name}</h3><p class="addr">${l.address}</p>
      <div class="rowb"><span class="rating-chip">${l.rating.toFixed(1)} ★</span><span style="color:var(--ink-soft)">${l.city} · ${l.area}</span></div>`;
    card.onclick=()=>{ map.flyTo([l.lat,l.lng],15,{duration:0.6}); storeMarkers[idx].openPopup(); };
    list.appendChild(card);
  });
}
document.getElementById('browseSearch').addEventListener('input', renderBrowse);

const addrInput=document.getElementById('addrInput');
const geoBtn=document.getElementById('geoBtn');
const locConfirmed=document.getElementById('locConfirmed');
const requestBtn=document.getElementById('requestBtn');

geoBtn.onclick=()=>{
  if(!navigator.geolocation){ alert("Geolocation isn't available in this browser."); return; }
  geoBtn.style.opacity=0.5;
  navigator.geolocation.getCurrentPosition(pos=>{
    userLoc = { lat:pos.coords.latitude, lng:pos.coords.longitude };
    addrInput.value = `Current location (${userLoc.lat.toFixed(4)}, ${userLoc.lng.toFixed(4)})`;
    placeUserMarker();
    onLocationSet();
    geoBtn.style.opacity=1;
  }, err=>{
    alert("Couldn't get your location. Try typing an address instead.");
    geoBtn.style.opacity=1;
  }, {enableHighAccuracy:true, timeout:8000});
};

addrInput.addEventListener('change', ()=>{
  if(!userLoc && addrInput.value.trim()){
    // Approximate from current map center (works in any city until real geocoding exists)
    const c = map.getCenter();
    userLoc = { lat:c.lat + (Math.random()-0.5)*0.02, lng:c.lng + (Math.random()-0.5)*0.02 };
    placeUserMarker();
    onLocationSet();
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
  if(!selectedServices.length) return 'No service selected';
  return selectedServices.map(s => `${s.label} · ${weight}${s.unit}`).join(' + ');
}
function servicesCost(){
  return selectedServices.reduce((sum, s) => sum + weight * s.rate, 0);
}

document.getElementById('serviceGrid').addEventListener('click', (e)=>{
  const card = e.target.closest('.service-card');
  if(!card) return;
  e.preventDefault();
  // Multi-select checkboxes: tap toggles. Keep at least one on.
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

// schedule now/later
const scheduleNowBtn=document.getElementById('scheduleNowBtn'), scheduleLaterBtn=document.getElementById('scheduleLaterBtn');
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
  if(!dt){
    alert('Please pick a pickup date and time.');
    return false;
  }
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

// payment method
document.getElementById('paymentGrid').addEventListener('click',(e)=>{
  const opt = e.target.closest('.pay-option');
  if(!opt) return;
  document.querySelectorAll('.pay-option').forEach(o=>o.classList.remove('selected'));
  opt.classList.add('selected');
  selectedPayment = { key:opt.dataset.pay, name:opt.querySelector('.pay-name').textContent };
  savePaymentPref();
});
// reflect any saved payment preference in the UI
document.querySelectorAll('.pay-option').forEach(o=>{
  o.classList.toggle('selected', o.dataset.pay===selectedPayment.key);
});

// promo code
document.getElementById('promoBtn').onclick=()=>{
  const code = document.getElementById('promoInput').value.trim().toUpperCase();
  const applied = document.getElementById('promoApplied');
  if(code==='SKY10'){
    promoDiscount = 0.10;
    applied.classList.add('show');
    applied.textContent = '✓ SKY10 applied — 10% off';
  } else if(code.length>0){
    promoDiscount = 0;
    applied.classList.add('show');
    applied.style.color = 'var(--warn)';
    applied.textContent = '✕ Invalid code — try SKY10';
  } else {
    promoDiscount = 0;
    applied.classList.remove('show');
  }
  updateEstimate();
};

function updateEstimate(){
  const hasItem = selectedServices.some(s => s.unit === 'item');
  const hasKg = selectedServices.some(s => s.unit === 'kg');
  const unitHint = hasKg && hasItem ? 'qty' : (hasItem ? 'item' : 'kg');
  document.getElementById('weightVal').textContent = `${weight} ${unitHint === 'qty' ? '' : unitHint}`.trim() || `${weight}`;
  if(unitHint === 'qty') document.getElementById('weightVal').textContent = `${weight}`;

  const base = 500;
  const serviceCost = servicesCost();
  const fee = serviceCost * 0.10;
  const subtotal = base + serviceCost + fee;
  const discount = subtotal * promoDiscount;
  const total = subtotal - discount;

  document.getElementById('estBase').textContent = fmtNaira(base);
  document.getElementById('estServiceLabel').textContent = formatServicesLabel();
  document.getElementById('estService').textContent = fmtNaira(serviceCost);
  document.getElementById('estFee').textContent = fmtNaira(fee);
  document.getElementById('promoRow').style.display = discount>0 ? 'flex' : 'none';
  document.getElementById('estPromo').textContent = '-' + fmtNaira(discount);
  document.getElementById('estTotal').textContent = fmtNaira(total);
  return total;
}
syncQuantityLabel();
updateEstimate();

const steps = ['stepForm','stepMatching','stepMatched','stepTrip','stepRating'];
function showStep(id){
  steps.forEach(s=>document.getElementById(s).classList.toggle('hidden', s!==id));
}

let nearbyOffers = [];

function partnerInitials(name){
  return name.split(' ').map(w=>w[0]).filter(Boolean).slice(0,2).join('');
}
function etaForDist(dist){
  if(isScheduled) return formatScheduledPickup();
  return `${Math.max(8, Math.round(dist*4 + 6))} min`;
}

requestBtn.onclick=()=>{
  if(!userLoc){ alert('Please set a pickup location first.'); return; }
  if(!validateSchedule()) return;
  showStep('stepMatching');
  document.getElementById('liveBadge').classList.add('show');
  document.getElementById('liveText').textContent='Finding partners…';

  setTimeout(()=>{
    // Nearby partners only — same idea as Uber: your city, not a fixed city
    nearbyOffers = LAUNDRIES.map((l,i)=>({
      ...l, idx:i, dist:haversine(userLoc.lat,userLoc.lng,l.lat,l.lng)
    }))
      .filter(p => p.dist <= NEARBY_RADIUS_KM)
      .sort((a,b)=> a.dist - b.dist)
      .slice(0, 8);

    matchedProvider = null;
    showOffers();
  }, 1200);
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

  Object.keys(storeMarkers).forEach(i=> storeMarkers[i].setIcon(ICON_STORE));

  if(!nearbyOffers.length){
    list.innerHTML = `<div class="history-empty">No partners within ${NEARBY_RADIUS_KM} km of your pickup. Try another location, or browse the map for cities we cover.</div>`;
    document.getElementById('matchTotal').textContent = document.getElementById('estTotal').textContent;
    showStep('stepMatched');
    return;
  }

  if(nearbyOffers.length){
    const pts = nearbyOffers.map(p=>[p.lat,p.lng]);
    pts.push([userLoc.lat, userLoc.lng]);
    map.flyToBounds(L.latLngBounds(pts), {padding:[50,50], duration:0.7});
  }

  nearbyOffers.forEach((p)=>{
    const row = document.createElement('button');
    row.type = 'button';
    row.className = 'offer-row';
    row.innerHTML = `
      <div class="provider-avatar">${partnerInitials(p.name)}</div>
      <div class="offer-main">
        <h4>${p.name}</h4>
        <div class="offer-meta">
          <span class="stars">★ ${p.rating.toFixed(1)}</span>
          <span>${p.city} · ${p.area}</span>
        </div>
      </div>
      <div class="offer-eta">
        <div class="mins">${etaForDist(p.dist)}</div>
        <div class="km">${p.dist.toFixed(1)} km</div>
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
  if(matchedProvider) storeMarkers[matchedProvider.idx].setIcon(ICON_STORE);
  matchedProvider = p;
  document.querySelectorAll('.offer-row').forEach(r=> r.classList.remove('selected'));
  rowEl.classList.add('selected');
  storeMarkers[p.idx].setIcon(ICON_STORE_ACTIVE);
  map.flyToBounds(L.latLngBounds([[userLoc.lat,userLoc.lng],[p.lat,p.lng]]), {padding:[60,60], duration:0.5});

  document.getElementById('matchEta').textContent = etaForDist(p.dist);
  const confirmBtn = document.getElementById('confirmBtn');
  confirmBtn.disabled = false;
  confirmBtn.textContent = 'Confirm pickup';
}

document.getElementById('cancelMatchBtn').onclick=resetToForm;
document.getElementById('confirmBtn').onclick=()=>{
  if(!matchedProvider){ alert('Please select a partner first.'); return; }
  startTrip();
};

function resetToForm(){
  if(matchedProvider) storeMarkers[matchedProvider.idx].setIcon(ICON_STORE);
  Object.keys(storeMarkers).forEach(i=> storeMarkers[i].setIcon(ICON_STORE));
  clearTimeout(tripTimer);
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
  showStep('stepForm');
}
document.getElementById('cancelTripBtn').onclick=resetToForm;

function buildStepperUI(){
  const track = document.getElementById('stepperTrack');
  track.innerHTML='';
  STATUSES.forEach((s,i)=>{
    const item = document.createElement('div');
    item.className='step-item';
    item.id = 'step-'+s.key;
    item.innerHTML = `<div class="step-dot">${i<currentStatusIdx?'✓':''}</div>
      <div class="step-text"><div class="label">${s.label}</div><div class="time" id="time-${s.key}"></div></div>`;
    track.appendChild(item);
  });
}

function updateStepperUI(){
  STATUSES.forEach((s,i)=>{
    const item = document.getElementById('step-'+s.key);
    item.classList.remove('done','current');
    const dot = item.querySelector('.step-dot');
    if(i < currentStatusIdx){ item.classList.add('done'); dot.textContent='✓'; }
    else if(i === currentStatusIdx){ item.classList.add('current'); dot.textContent=''; }
    else { dot.textContent=''; }
  });
  document.getElementById('tripStatusBig').textContent = STATUSES[currentStatusIdx].label;
}

function startTrip(){
  currentStatusIdx = 0;
  buildStepperUI();
  updateStepperUI();

  const p = matchedProvider;
  document.getElementById('tripAvatar').textContent = partnerInitials(p.name);
  document.getElementById('tripName').textContent = p.name;
  document.getElementById('tripStars').textContent = `★ ${p.rating.toFixed(1)}`;
  document.getElementById('liveText').textContent='Trip in progress';
  mapFab.classList.add('live');

  showStep('stepTrip');
  advanceStatus();
}

function markTime(key){
  const el = document.getElementById('time-'+key);
  if(el) el.textContent = new Date().toLocaleTimeString('en-NG',{hour:'2-digit',minute:'2-digit'});
}

function advanceStatus(){
  const s = STATUSES[currentStatusIdx];
  markTime(s.key);
  updateStepperUI();

  const etaEl = document.getElementById('tripEta');
  if(s.key==='confirmed') etaEl.textContent = 'Preparing…';
  if(s.key==='enroute'){ etaEl.textContent = Math.round(s.dur/1000)+'s'; animateRider(matchedProvider, userLoc, s.dur); }
  if(s.key==='pickedup'){ etaEl.textContent = 'At pickup'; if(riderMarker){map.removeLayer(riderMarker); riderMarker=null;} if(routeLine){map.removeLayer(routeLine); routeLine=null;} }
  if(s.key==='washing'){ etaEl.textContent = Math.round(s.dur/1000)+'s'; }
  if(s.key==='delivering'){ etaEl.textContent = Math.round(s.dur/1000)+'s'; animateRider(userLoc, matchedProvider, s.dur); }
  if(s.key==='delivered'){
    etaEl.textContent='Arrived';
    document.getElementById('liveText').textContent='Delivered';
    if(riderMarker){ map.removeLayer(riderMarker); riderMarker=null; }
    setTimeout(showRating, 900);
    return;
  }

  tripTimer = setTimeout(()=>{
    currentStatusIdx++;
    advanceStatus();
  }, s.dur);
}

function animateRider(fromPoint, toPoint, dur){
  const from = [fromPoint.lat, fromPoint.lng];
  const to = [toPoint.lat, toPoint.lng];
  if(riderMarker) map.removeLayer(riderMarker);
  if(routeLine) map.removeLayer(routeLine);
  riderMarker = L.marker(from, {icon:ICON_RIDER}).addTo(map);
  routeLine = L.polyline([from, to], {color:'#f5a623', weight:3, dashArray:'6,8', opacity:0.85}).addTo(map);

  const start = performance.now();
  function step(now){
    const t = Math.min(1, (now-start)/dur);
    const lat = from[0] + (to[0]-from[0])*t;
    const lng = from[1] + (to[1]-from[1])*t;
    riderMarker.setLatLng([lat,lng]);
    if(t<1){ animFrame = requestAnimationFrame(step); }
  }
  animFrame = requestAnimationFrame(step);
  map.flyToBounds(L.latLngBounds([from,to]), {padding:[80,80], duration:0.6});
}

let selectedRatingVal = 5;
function showRating(){
  document.getElementById('ratingProviderName').textContent = matchedProvider.name;
  selectedRatingVal = 5;
  document.querySelectorAll('#starsInput button').forEach(b=>b.classList.add('filled'));
  showStep('stepRating');
}
document.getElementById('starsInput').addEventListener('click',(e)=>{
  const btn = e.target.closest('button');
  if(!btn) return;
  selectedRatingVal = parseInt(btn.dataset.star);
  document.querySelectorAll('#starsInput button').forEach(b=>{
    b.classList.toggle('filled', parseInt(b.dataset.star)<=selectedRatingVal);
  });
});
document.getElementById('doneBtn').onclick=()=>{
  if(currentOrderSnapshot && matchedProvider){
    orderHistory.push({
      providerName: matchedProvider.name,
      serviceLabel: currentOrderSnapshot.serviceLabel,
      total: currentOrderSnapshot.total,
      payment: currentOrderSnapshot.payment,
      rating: selectedRatingVal,
      date: new Date().toLocaleDateString('en-NG', {day:'numeric', month:'short', year:'numeric'})
    });
    saveHistory();
  }
  if(matchedProvider) storeMarkers[matchedProvider.idx].setIcon(ICON_STORE);
  document.getElementById('liveBadge').classList.remove('show');
  document.getElementById('chatPanel').classList.remove('show');
  document.getElementById('chatLog').innerHTML='';
  mapFab.classList.remove('live');
  matchedProvider=null;
  currentOrderSnapshot=null;
  promoDiscount=0;
  document.getElementById('promoApplied').classList.remove('show');
  document.getElementById('promoInput').value='';
  updateEstimate();
  showStep('stepForm');
};

// in-trip chat
const chatToggleBtn=document.getElementById('chatToggleBtn'), chatPanel=document.getElementById('chatPanel'), chatLog=document.getElementById('chatLog');
chatToggleBtn.onclick=()=>{ chatPanel.classList.toggle('show'); };
document.getElementById('chatQuick').addEventListener('click',(e)=>{
  const btn = e.target.closest('button');
  if(!btn) return;
  addChatMsg(btn.dataset.msg, 'me');
  setTimeout(()=>{
    const reply = PROVIDER_REPLIES[Math.floor(Math.random()*PROVIDER_REPLIES.length)];
    addChatMsg(reply, 'them');
  }, 900 + Math.random()*700);
});
function addChatMsg(text, who){
  const div = document.createElement('div');
  div.className = 'chat-msg ' + who;
  div.textContent = text;
  chatLog.appendChild(div);
  chatLog.scrollTop = chatLog.scrollHeight;
}

showStep('stepForm');
