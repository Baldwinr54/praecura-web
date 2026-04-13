(() => {
  'use strict';

  const form = document.querySelector('form[data-reschedule-form]');
  if (!form) return;

  const doctorSel = form.querySelector('select[name="doctorId"]');
  const scheduledAtInput = form.querySelector('input[name="scheduledAt"]');

  const availabilityBox = document.getElementById('availabilityBox');
  const availabilityMsg = document.getElementById('availabilityMessage');
  const applySuggestionBtn = document.getElementById('applySuggestion');
  const suggestedAtLabel = document.getElementById('suggestedAtLabel');

  const appointmentId = form.getAttribute('data-appointment-id') || '';
  const duration = form.getAttribute('data-duration') || '30';

  function toInputValue(dtStr) {
    if (!dtStr) return '';
    return dtStr.substring(0, 16);
  }

  function setAvailabilityState(ok, message, suggestedAt) {
    availabilityBox.hidden = false;
    availabilityBox.classList.remove('success', 'warning', 'danger');
    availabilityBox.classList.add(ok ? 'success' : 'warning');
    availabilityMsg.textContent = message || '';

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
    availabilityBox.hidden = false;
    availabilityBox.classList.remove('success', 'warning', 'danger');
    availabilityBox.classList.add('danger');
    availabilityMsg.textContent = message || 'No se pudo validar la disponibilidad.';
    applySuggestionBtn.hidden = true;
    suggestedAtLabel.textContent = '';
  }

  async function checkAvailability() {
    if (!doctorSel?.value || !scheduledAtInput?.value) return;

    const params = new URLSearchParams();
    params.append('doctorId', doctorSel.value);
    params.append('scheduledAt', scheduledAtInput.value);
    params.append('durationMinutes', duration);
    if (appointmentId) params.append('appointmentId', appointmentId);

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

  doctorSel?.addEventListener('change', debouncedAvailabilityCheck);
  scheduledAtInput?.addEventListener('change', debouncedAvailabilityCheck);

  if (doctorSel?.value && scheduledAtInput?.value) {
    debouncedAvailabilityCheck();
  }
})();
