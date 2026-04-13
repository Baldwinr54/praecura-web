(() => {
  'use strict';

  const stack = document.getElementById('toastStack');
  const source = document.getElementById('toastSource');
  if (!stack || !source) return;

  const TYPE_CLASS = {
    success: 'toast-success',
    error: 'toast-error',
    info: 'toast-info',
    warning: 'toast-warning',
  };

  const DEFAULT_TIMEOUT_MS = 4500;

  function escapeHtml(input) {
    const str = String(input ?? '');
    return str
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }

  function makeToast(type, message) {
    const klass = TYPE_CLASS[type] || TYPE_CLASS.info;

    const toast = document.createElement('div');
    toast.className = `toast-item ${klass}`;
    toast.setAttribute('role', 'status');

    const icon = document.createElement('div');
    icon.className = 'toast-icon';
    icon.innerHTML = type === 'success' ? '<i class="bi bi-check2-circle"></i>'
      : type === 'error' ? '<i class="bi bi-exclamation-octagon"></i>'
      : type === 'warning' ? '<i class="bi bi-exclamation-triangle"></i>'
      : '<i class="bi bi-info-circle"></i>';

    const body = document.createElement('div');
    body.className = 'toast-body';
    body.innerHTML = escapeHtml(message);

    const close = document.createElement('button');
    close.type = 'button';
    close.className = 'toast-close';
    close.setAttribute('aria-label', 'Cerrar');
    close.innerHTML = '<i class="bi bi-x"></i>';

    close.addEventListener('click', () => {
      toast.classList.add('hide');
      setTimeout(() => toast.remove(), 180);
    });

    toast.appendChild(icon);
    toast.appendChild(body);
    toast.appendChild(close);
    stack.appendChild(toast);

    // Animate in
    requestAnimationFrame(() => toast.classList.add('show'));

    // Auto-dismiss
    setTimeout(() => {
      if (!toast.isConnected) return;
      toast.classList.add('hide');
      setTimeout(() => toast.remove(), 180);
    }, DEFAULT_TIMEOUT_MS);
  }

  // Read flash messages rendered in layout
  const items = Array.from(source.querySelectorAll('span[data-type][data-message]'));
  items.forEach(el => {
    const type = (el.getAttribute('data-type') || 'info').trim();
    const message = (el.getAttribute('data-message') || '').trim();
    if (message) makeToast(type, message);
  });
})();
