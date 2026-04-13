(() => {
  'use strict';

  const form = document.querySelector('form[data-appointment-form]');
  if (!form) return;

  const doctorSel = document.getElementById('doctorId');
  const serviceSel = document.getElementById('serviceId');
  const siteSel = document.getElementById('siteId');
  const resourceSel = document.getElementById('resourceId');
  const durationInput = document.getElementById('durationMinutes');
  const durationAuto = document.getElementById('durationAuto');
  const scheduledAtInput = document.querySelector('input[name="scheduledAt"]');
  const hint = document.getElementById('serviceHint');
  const doctorScheduleHint = document.getElementById('doctorScheduleHint');

  const availabilityBox = document.getElementById('availabilityBox');
  const availabilityMsg = document.getElementById('availabilityMessage');
  const applySuggestionBtn = document.getElementById('applySuggestion');
  const suggestedAtLabel = document.getElementById('suggestedAtLabel');
  const submitBtn = form.querySelector('button[type="submit"]');

  const appointmentId = form.getAttribute('data-appointment-id') || '';

  if (!doctorSel || !serviceSel || !durationInput || !durationAuto) return;

  // Si el usuario toca la duración, desactivamos el modo auto
  durationInput.addEventListener('input', () => { durationAuto.value = 'false'; });

  function showAllServices() {
    Array.from(serviceSel.options).forEach(opt => {
      opt.hidden = false;
      opt.disabled = false;
    });
    if (hint) hint.textContent = 'Requerido. La duración se ajusta automáticamente (puedes cambiarla).';
  }

  function applyServiceFilter(allowedIds) {
    const allowed = new Set((allowedIds || []).map(String));
    if (allowed.size === 0) { showAllServices(); return; }

    Array.from(serviceSel.options).forEach(opt => {
      if (!opt.value) return; // opción vacía
      const ok = allowed.has(String(opt.value));
      opt.hidden = !ok;
      opt.disabled = !ok;
    });

    if (serviceSel.value && !allowed.has(String(serviceSel.value))) {
      serviceSel.value = '';
      durationAuto.value = 'true';
    }
    if (hint) hint.textContent = 'Mostrando servicios que ofrece el médico seleccionado.';
  }

  function formatWorkDays(raw) {
    if (!raw) return '';
    const map = {
      MON: 'Lun',
      TUE: 'Mar',
      WED: 'Mié',
      THU: 'Jue',
      FRI: 'Vie',
      SAT: 'Sáb',
      SUN: 'Dom'
    };
    return raw.split(',')
      .map(x => x && x.trim().toUpperCase())
      .filter(Boolean)
      .map(x => map[x] || x)
      .join(', ');
  }

  function updateDoctorScheduleHint() {
    if (!doctorScheduleHint) return;
    const opt = doctorSel?.selectedOptions && doctorSel.selectedOptions[0];
    if (!opt || !opt.value) {
      doctorScheduleHint.hidden = true;
      doctorScheduleHint.textContent = '';
      return;
    }
    const days = formatWorkDays(opt.getAttribute('data-work-days'));
    const start = opt.getAttribute('data-work-start') || '';
    const end = opt.getAttribute('data-work-end') || '';
    if (days || (start && end)) {
      const range = (start && end) ? `${start}–${end}` : '';
      doctorScheduleHint.textContent = `Horario del médico: ${days}${range ? ' · ' + range : ''}.`;
      doctorScheduleHint.hidden = false;
    } else {
      doctorScheduleHint.hidden = true;
      doctorScheduleHint.textContent = '';
    }
  }

  function filterResourcesBySite(siteId) {
    if (!resourceSel) return;
    const target = siteId ? String(siteId) : '';

    Array.from(resourceSel.options).forEach(opt => {
      if (!opt.value) return;
      const optSite = opt.getAttribute('data-site') || '';
      const ok = !target || optSite === target;
      opt.hidden = !ok;
      opt.disabled = !ok;
    });

    if (resourceSel.value) {
      const selectedOpt = resourceSel.selectedOptions && resourceSel.selectedOptions[0];
      const selectedSite = selectedOpt ? (selectedOpt.getAttribute('data-site') || '') : '';
      if (target && selectedSite !== target) {
        resourceSel.value = '';
      }
    }
  }

  if (siteSel) {
    siteSel.addEventListener('change', () => {
      filterResourcesBySite(siteSel.value);
    });
  }

  if (resourceSel && siteSel) {
    resourceSel.addEventListener('change', () => {
      const opt = resourceSel.selectedOptions && resourceSel.selectedOptions[0];
      const resSite = opt ? (opt.getAttribute('data-site') || '') : '';
      if (resSite && !siteSel.value) {
        siteSel.value = resSite;
        filterResourcesBySite(siteSel.value);
      }
    });
  }

  async function loadDoctorServices(doctorId) {
    if (!doctorId) return [];
    try {
      const res = await fetch(`/api/doctors/${doctorId}/services`, { headers: { 'Accept': 'application/json' } });
      if (!res.ok) return [];
      const data = await res.json();
      if (!Array.isArray(data)) return [];
      return data.map(x => x.id).filter(Boolean);
    } catch (e) {
      return [];
    }
  }

  function syncDurationFromService() {
    const opt = serviceSel.selectedOptions && serviceSel.selectedOptions.length ? serviceSel.selectedOptions[0] : null;
    if (!opt || !opt.value) return;
    const dur = opt.getAttribute('data-duration');
    if (!dur) return;
    durationAuto.value = 'true';
    durationInput.value = dur;
  }

  doctorSel.addEventListener('change', async () => {
    const ids = await loadDoctorServices(doctorSel.value);
    applyServiceFilter(ids);
    updateDoctorScheduleHint();
    debouncedAvailabilityCheck();
  });

  serviceSel.addEventListener('change', () => {
    if (serviceSel.value) syncDurationFromService();
    debouncedAvailabilityCheck();
  });

  scheduledAtInput?.addEventListener('change', debouncedAvailabilityCheck);
  durationInput?.addEventListener('change', debouncedAvailabilityCheck);

  function toInputValue(dtStr) {
    if (!dtStr) return '';
    return dtStr.substring(0, 16);
  }

  function setAvailabilityState(ok, message, suggestedAt) {
    if (!availabilityBox || !availabilityMsg) return;
    availabilityBox.hidden = false;
    availabilityBox.classList.remove('success', 'warning', 'danger');
    availabilityBox.classList.add(ok ? 'success' : 'warning');
    availabilityMsg.textContent = message || '';
    setScheduleValidity(ok, message);

    if (suggestedAt) {
      suggestedAtLabel.textContent = suggestedAt.replace('T', ' ').substring(0, 16);
      applySuggestionBtn.hidden = false;
      applySuggestionBtn.onclick = () => {
        if (scheduledAtInput) scheduledAtInput.value = toInputValue(suggestedAt);
        debouncedAvailabilityCheck();
      };
    } else {
      applySuggestionBtn.hidden = true;
      suggestedAtLabel.textContent = '';
    }
  }

  function setAvailabilityError(message) {
    if (!availabilityBox || !availabilityMsg) return;
    availabilityBox.hidden = false;
    availabilityBox.classList.remove('success', 'warning', 'danger');
    availabilityBox.classList.add('danger');
    availabilityMsg.textContent = message || 'No se pudo validar la disponibilidad.';
    setScheduleValidity(false, message || 'No se pudo validar la disponibilidad.');
    applySuggestionBtn.hidden = true;
    suggestedAtLabel.textContent = '';
  }

  function setScheduleValidity(ok, message) {
    if (!scheduledAtInput) return;
    if (ok) {
      scheduledAtInput.setCustomValidity('');
    } else {
      scheduledAtInput.setCustomValidity(message || 'Horario no disponible.');
    }
    if (submitBtn) submitBtn.disabled = !ok;
  }

  async function checkAvailability() {
    if (!doctorSel.value || !scheduledAtInput?.value) {
      if (availabilityBox) availabilityBox.hidden = true;
      if (availabilityMsg) availabilityMsg.textContent = '';
      if (applySuggestionBtn) applySuggestionBtn.hidden = true;
      if (suggestedAtLabel) suggestedAtLabel.textContent = '';
      if (submitBtn) submitBtn.disabled = false;
      if (scheduledAtInput) scheduledAtInput.setCustomValidity('');
      return;
    }

    const params = new URLSearchParams();
    params.append('doctorId', doctorSel.value);
    params.append('scheduledAt', scheduledAtInput.value);
    params.append('durationMinutes', durationInput.value || '30');
    if (appointmentId) params.append('appointmentId', appointmentId);
    if (resourceSel && resourceSel.value) params.append('resourceId', resourceSel.value);

    try {
      const res = await fetch(`/api/appointments/check?${params.toString()}`, { headers: { 'Accept': 'application/json' } });
      if (!res.ok) {
        setAvailabilityError('No se pudo validar la disponibilidad.');
        return;
      }
      const data = await res.json();
      setAvailabilityState(!!data.ok, data.message, data.suggestedAt || null);
    } catch (e) {
      setAvailabilityError('No se pudo validar la disponibilidad.');
    }
  }

  function debounce(fn, wait) {
    let t;
    return () => {
      clearTimeout(t);
      t = setTimeout(fn, wait);
    };
  }

  const debouncedAvailabilityCheck = debounce(checkAvailability, 250);

  // Inicialización
  if (doctorSel.value) {
    doctorSel.dispatchEvent(new Event('change'));
  }
  if (siteSel) {
    filterResourcesBySite(siteSel.value);
  }
})();
