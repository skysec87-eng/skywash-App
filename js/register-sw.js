if('serviceWorker' in navigator){
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js?v=24').then((reg) => {
      // Force check for updates so schedule UI / app code isn't stuck on old cache
      reg.update().catch(()=>{});
    }).catch(()=>{});
  });
}
