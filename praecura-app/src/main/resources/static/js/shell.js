(() => {
  'use strict';

  const sidebar = document.querySelector('.sidebar');
  const sidebarScroll = document.querySelector('.sidebar-scroll');
  const toggleBtn = document.getElementById('sidebarToggle');
  const searchInput = document.getElementById('globalSearch');
  const searchResults = document.getElementById('globalSearchResults');
  const favoritesWrap = document.querySelector('.favorites');
  const favoritesNav = document.getElementById('favoritesNav');
  const shortcutsBtn = document.getElementById('shortcutsBtn');
  const breadcrumbsEl = document.getElementById('breadcrumbs');

  const STORAGE = {
    sidebarCompact: 'praecura.sidebar.compact',
    favorites: 'praecura.favorites',
    sidebarScroll: 'praecura.sidebar.scroll',
  };

  function safeParse(json, fallback) {
    try { return JSON.parse(json); } catch { return fallback; }
  }

  function setCompact(on) {
    if (!sidebar) return;
    sidebar.classList.toggle('compact', !!on);
    localStorage.setItem(STORAGE.sidebarCompact, on ? '1' : '0');
  }

  function isCompact() {
    return localStorage.getItem(STORAGE.sidebarCompact) === '1';
  }

  function loadFavorites() {
    return safeParse(localStorage.getItem(STORAGE.favorites) || '[]', []);
  }

  function saveFavorites(keys) {
    localStorage.setItem(STORAGE.favorites, JSON.stringify(keys));
  }

  function navLinkByKey(key) {
    return document.querySelector(`.nav-link[data-nav="${CSS.escape(key)}"]`);
  }

  function renderFavorites() {
    if (!favoritesWrap || !favoritesNav) return;

    const favs = loadFavorites();
    favoritesNav.innerHTML = '';

    const visible = favs
      .map(k => ({ k, link: navLinkByKey(k) }))
      .filter(x => x.link);

    favoritesWrap.hidden = visible.length === 0;
    if (visible.length === 0) return;

    for (const item of visible) {
      const clone = item.link.cloneNode(true);
      // Remove nested favorite buttons inside favorites
      const favBtn = clone.querySelector('button.fav');
      if (favBtn) favBtn.remove();
      // Keep only chevron
      const actions = clone.querySelector('.nav-actions');
      if (actions) {
        const chev = actions.querySelector('.chev');
        actions.innerHTML = '';
        if (chev) actions.appendChild(chev);
      }
      favoritesNav.appendChild(clone);
    }

    // Update stars in main menu
    document.querySelectorAll('.nav-link[data-nav] .fav').forEach(btn => {
      const key = btn.closest('.nav-link')?.getAttribute('data-nav');
      const starred = favs.includes(key);
      btn.classList.toggle('on', starred);
      const icon = btn.querySelector('i');
      if (icon) icon.className = starred ? 'bi bi-star-fill' : 'bi bi-star';
      btn.setAttribute('aria-pressed', starred ? 'true' : 'false');
    });
  }

  function toggleFavorite(key) {
    const favs = loadFavorites();
    const idx = favs.indexOf(key);
    if (idx >= 0) favs.splice(idx, 1);
    else favs.push(key);
    saveFavorites(favs);
    renderFavorites();
  }

  function initFavorites() {
    document.querySelectorAll('.nav-link[data-nav] .fav').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        const key = btn.closest('.nav-link')?.getAttribute('data-nav');
        if (key) toggleFavorite(key);
      });
    });
    renderFavorites();
  }

  function getNavItems() {
    const items = [];
    document.querySelectorAll('.nav-link[data-nav]').forEach(a => {
      const key = a.getAttribute('data-nav') || '';
      const label = (a.querySelector('strong')?.textContent || '').trim();
      const href = a.getAttribute('href') || '';
      const labels = (a.getAttribute('data-labels') || '').toLowerCase();
      items.push({ key, label, href, labels, el: a });

      // Tooltip for compact mode
      if (!a.getAttribute('title') && label) a.setAttribute('title', label);
    });
    return items;
  }

  function renderSearchResults(matches) {
    if (!searchResults) return;
    searchResults.innerHTML = '';

    if (matches.length === 0) {
      searchResults.hidden = true;
      return;
    }

    matches.slice(0, 7).forEach(m => {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'search-item';
      btn.innerHTML = `
        <div class="search-item-title">${m.label}</div>
        <div class="search-item-sub">${m.href}</div>
      `;
      btn.addEventListener('click', () => {
        window.location.href = m.href;
      });
      searchResults.appendChild(btn);
    });

    searchResults.hidden = false;
  }

  function initGlobalSearch() {
    if (!searchInput || !searchResults) return;

    const navItems = getNavItems();

    function search(q) {
      const query = (q || '').trim().toLowerCase();
      if (!query) {
        renderSearchResults([]);
        return;
      }

      const matches = navItems.filter(n =>
        n.label.toLowerCase().includes(query) ||
        n.href.toLowerCase().includes(query) ||
        (n.labels && n.labels.includes(query))
      );

      renderSearchResults(matches);
    }

    searchInput.addEventListener('input', () => search(searchInput.value));

    searchInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        const first = searchResults.querySelector('.search-item');
        if (first) first.click();
      }
      if (e.key === 'Escape') {
        searchInput.value = '';
        renderSearchResults([]);
        searchInput.blur();
      }
    });

    // click outside closes
    document.addEventListener('click', (e) => {
      if (!searchResults.contains(e.target) && e.target !== searchInput) {
        renderSearchResults([]);
      }
    });
  }

  function restoreSidebarScroll() {
    if (!sidebarScroll) return;
    const saved = parseInt(localStorage.getItem(STORAGE.sidebarScroll) || '0', 10);
    if (!Number.isNaN(saved)) {
      sidebarScroll.scrollTop = saved;
    }
  }

  function initSidebarScrollPersist() {
    if (!sidebarScroll) return;
    let raf = null;
    sidebarScroll.addEventListener('scroll', () => {
      if (raf) return;
      raf = window.requestAnimationFrame(() => {
        localStorage.setItem(STORAGE.sidebarScroll, String(sidebarScroll.scrollTop));
        raf = null;
      });
    });

    document.querySelectorAll('.nav-link[data-nav]').forEach((link) => {
      link.addEventListener('click', () => {
        localStorage.setItem(STORAGE.sidebarScroll, String(sidebarScroll.scrollTop));
      });
    });
  }

  function initShortcuts() {
    const modalEl = document.getElementById('shortcutsModal');
    const modal = (modalEl && window.bootstrap?.Modal) ? new window.bootstrap.Modal(modalEl) : null;

    shortcutsBtn?.addEventListener('click', () => modal?.show());

    document.addEventListener('keydown', (e) => {
      // Ignore typing in inputs
      const tag = (e.target && e.target.tagName) ? e.target.tagName.toLowerCase() : '';
      if (tag === 'input' || tag === 'textarea' || (e.target && e.target.isContentEditable)) return;

      const key = e.key;

      // "/" focuses search
      if (key === '/') {
        e.preventDefault();
        searchInput?.focus();
        return;
      }

      // Shift+? opens shortcut modal
      if (key === '?' && e.shiftKey) {
        e.preventDefault();
        modal?.show();
        return;
      }

      if (key === 'n' || key === 'N') {
        window.location.href = '/appointments/new';
        return;
      }

      if (key === 'g' || key === 'G') {
        window.location.href = '/agenda';
        return;
      }

      if (key === 'r' || key === 'R') {
        window.location.href = '/reception';
        return;
      }

      if (key === 'h' || key === 'H') {
        window.location.href = '/';
      }
    });
  }

  function initBreadcrumbs() {
    if (!breadcrumbsEl) return;

    const path = window.location.pathname || '/';
    const parts = path.split('/').filter(Boolean);

    const crumbs = [{ label: 'Inicio', href: '/' }];

    function titleForSegment(seg) {
      const map = {
        patients: 'Pacientes',
        doctors: 'Médicos',
        specialties: 'Especialidades',
        services: 'Servicios',
        sites: 'Sedes',
        resources: 'Recursos',
        appointments: 'Citas',
        agenda: 'Agenda',
        reception: 'Recepción',
        reports: 'Reportes',
        executive: 'BI ejecutivo',
        alerts: 'Alertas',
        closings: 'Cierre diario',
        billing: 'Facturación',
        insurance: 'ARS y seguros',
        inpatient: 'Hospitalización',
        pharmacy: 'Farmacia e inventario',
        'clinical-workflow': 'Clínico avanzado',
        charges: 'Cargos clínicos',
        cash: 'Caja',
        'accounts-receivable': 'Cuentas por cobrar',
        fiscal: 'NCF',
        ecf: 'e-CF DGII',
        admin: 'Administración',
        access: 'Permisos y aprobaciones',
        users: 'Usuarios',
        owner: 'Panel de dueño',
        branding: 'Marca y factura',
        clinical: 'Historia clínica',
        analytics: 'Analítica',
        compliance: 'Cumplimiento',
        revenue: 'Ingresos',
        productivity: 'Productividad',
        'productivity-services': 'Prod. por servicios',
        benchmark: 'Benchmark',
        'benchmark-monthly': 'Benchmark mensual',
        hours: 'Horas',
        days: 'Días',
        new: 'Nuevo',
        edit: 'Editar',
        day: 'Día',
      };
      if (map[seg]) return map[seg];
      if (/^\d+$/.test(seg)) return `#${seg}`;
      return seg;
    }

    let acc = '';
    for (const seg of parts) {
      acc += '/' + seg;
      crumbs.push({ label: titleForSegment(seg), href: acc });
    }

    if (crumbs.length <= 1) {
      breadcrumbsEl.hidden = true;
      return;
    }

    breadcrumbsEl.innerHTML = '';
    crumbs.forEach((c, idx) => {
      const a = document.createElement('a');
      a.className = 'crumb';
      a.href = c.href;
      a.textContent = c.label;
      if (idx === crumbs.length - 1) a.setAttribute('aria-current', 'page');
      breadcrumbsEl.appendChild(a);

      if (idx !== crumbs.length - 1) {
        const sep = document.createElement('span');
        sep.className = 'crumb-sep';
        sep.textContent = '›';
        breadcrumbsEl.appendChild(sep);
      }
    });

    breadcrumbsEl.hidden = false;
  }

  // Init
  if (toggleBtn) {
    toggleBtn.addEventListener('click', () => setCompact(!sidebar?.classList.contains('compact')));
  }
  setCompact(isCompact());

  initFavorites();
  initGlobalSearch();
  initShortcuts();
  initBreadcrumbs();
  restoreSidebarScroll();
  initSidebarScrollPersist();
})();
