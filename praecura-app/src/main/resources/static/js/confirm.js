// Simple, dependency-free confirmation handler for destructive actions.
// Usage: add `data-confirm="¿Seguro?"` to a <form> element.

(function () {
  function attach() {
    var forms = document.querySelectorAll('form[data-confirm]');
    forms.forEach(function (form) {
      // Avoid double-binding if the fragment is re-rendered.
      if (form.dataset.confirmBound === 'true') return;
      form.dataset.confirmBound = 'true';

      form.addEventListener('submit', function (e) {
        var msg = form.getAttribute('data-confirm') || '¿Está seguro?';
        if (!window.confirm(msg)) {
          e.preventDefault();
          e.stopPropagation();
          return;
        }

        var expected = form.getAttribute('data-confirm-text');
        if (expected) {
          var typed = window.prompt('Escribe "' + expected + '" para confirmar:');
          if (typed !== expected) {
            e.preventDefault();
            e.stopPropagation();
          }
        }
      });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', attach);
  } else {
    attach();
  }
})();
