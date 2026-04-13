// Input masks for Dominican formats
// - Phone: ###-###-#### (10 digits)
// - Cedula: ###-#######-# (11 digits)
// - RNC/Cedula fiscal: ###-#####-# (RNC 9) o ###-#######-# (Cedula 11)

(function () {
  "use strict";

  function digitsOnly(value) {
    return (value || "").replace(/\D+/g, "");
  }

  function formatPhone(digits) {
    // Max 10 digits
    digits = digits.slice(0, 10);
    const a = digits.slice(0, 3);
    const b = digits.slice(3, 6);
    const c = digits.slice(6, 10);
    if (digits.length <= 3) return a;
    if (digits.length <= 6) return `${a}-${b}`;
    return `${a}-${b}-${c}`;
  }

  function formatCedula(digits) {
    // Max 11 digits
    digits = digits.slice(0, 11);
    const a = digits.slice(0, 3);
    const b = digits.slice(3, 10);
    const c = digits.slice(10, 11);
    if (digits.length <= 3) return a;
    if (digits.length <= 10) return `${a}-${b}`;
    return `${a}-${b}-${c}`;
  }

  function formatTaxId(digits) {
    // RNC (9) o Cédula (11)
    digits = digits.slice(0, 11);
    if (digits.length <= 9) {
      const a = digits.slice(0, 3);
      const b = digits.slice(3, 8);
      const c = digits.slice(8, 9);
      if (digits.length <= 3) return a;
      if (digits.length <= 8) return `${a}-${b}`;
      return `${a}-${b}-${c}`;
    }
    return formatCedula(digits);
  }

  function setCaretToEnd(el) {
    try {
      const len = el.value.length;
      el.setSelectionRange(len, len);
    } catch (e) {
      // ignore
    }
  }

  function applyMask(el, formatter) {
    if (el.dataset.maskBound === "1") return;
    const handler = () => {
      const d = digitsOnly(el.value);
      el.value = formatter(d);
      setCaretToEnd(el);
    };
    el.addEventListener("input", handler);
    el.addEventListener("blur", handler);
    el.dataset.maskBound = "1";
  }

  function bindPhoneMask(el) {
    if (!el) return;
      el.setAttribute("inputmode", "numeric");
      el.setAttribute("maxlength", "12");
      applyMask(el, formatPhone);
  }

  function bindCedulaMask(el) {
    if (!el) return;
      el.setAttribute("inputmode", "numeric");
      el.setAttribute("maxlength", "13");
      applyMask(el, formatCedula);
  }

  function bindTaxIdMask(el) {
    if (!el) return;
    el.setAttribute("inputmode", "numeric");
    el.setAttribute("maxlength", "13");
    applyMask(el, formatTaxId);
  }

  function resolveTextHint(el) {
    const name = (el.getAttribute("name") || "").toLowerCase();
    const id = (el.getAttribute("id") || "").toLowerCase();
    const placeholder = (el.getAttribute("placeholder") || "").toLowerCase();
    return `${name} ${id} ${placeholder}`
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "");
  }

  function initMasks() {
    document.querySelectorAll("input[data-mask='phone'], input[data-mask='rd-phone']").forEach((el) => {
      bindPhoneMask(el);
    });

    document.querySelectorAll("input[data-mask='cedula'], input[data-mask='rd-cedula']").forEach((el) => {
      bindCedulaMask(el);
    });

    document.querySelectorAll("input[data-mask='rnc'], input[data-mask='tax-id'], input[data-mask='rd-tax-id']").forEach((el) => {
      bindTaxIdMask(el);
    });

    // Auto-bind defensivo para formularios que aún no declaran data-mask explícita.
    document.querySelectorAll("input:not([type='hidden'])").forEach((el) => {
      if (el.dataset.maskBound === "1") return;
      const hint = resolveTextHint(el);
      if (/(^|[^a-z])cedula([^a-z]|$)/.test(hint)) {
        bindCedulaMask(el);
        return;
      }
      if (/(^|[^a-z])(rnc|taxid|fiscaltaxid|billingtaxid)([^a-z]|$)/.test(hint)) {
        bindTaxIdMask(el);
        return;
      }
      if (/(^|[^a-z])(phone|telefono|tel|celular|movil|whatsapp)([^a-z]|$)/.test(hint)) {
        bindPhoneMask(el);
      }
    });
  }

  // Appointments: prevent selecting date/time in the past
  function initMinNow() {
    const els = document.querySelectorAll("input[data-min-now='true']");
    if (!els.length) return;

    const pad2 = (n) => String(n).padStart(2, "0");
    const now = new Date();
    // Truncate to minutes (HTML datetime-local works to minute precision)
    now.setSeconds(0, 0);
    const min = `${now.getFullYear()}-${pad2(now.getMonth() + 1)}-${pad2(now.getDate())}T${pad2(now.getHours())}:${pad2(now.getMinutes())}`;

    els.forEach((el) => {
      el.setAttribute("min", min);
      el.setAttribute("step", "60");
    });
  }

  document.addEventListener("DOMContentLoaded", () => {
    initMasks();
    initMinNow();
  });
})();
