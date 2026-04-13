(function () {
  "use strict";

  // Botón de tema (Claro / Oscuro)
  // Persistido en localStorage.
  const KEY_LEGACY = "praecura-theme-mode"; // legacy (light|dark|system)
  const KEY = "praecura-theme";            // current (light|dark)

  function normalizeTheme(value) {
    if (!value) return null;
    const v = String(value).toLowerCase();
    if (v === "dark" || v === "light") return v;
    return null; // legacy "system" treated as light
  }

  function getInitialTheme() {
    const saved = normalizeTheme(localStorage.getItem(KEY));
    if (saved) return saved;

    const legacy = localStorage.getItem(KEY_LEGACY);
    const legacyNorm = normalizeTheme(legacy);
    if (legacyNorm) return legacyNorm;

    return "light";
  }

  function updateToggleUI(currentTheme) {
    const btn = document.getElementById("themeToggle");
    if (!btn) return;

    // Icono: muestra la acción (en oscuro, sol para pasar a claro; en claro, luna para pasar a oscuro)
    const icon = btn.querySelector("i.bi");
    if (icon) {
      if (currentTheme === "dark") {
        icon.classList.remove("bi-moon-stars-fill");
        icon.classList.add("bi-sun-fill");
      } else {
        icon.classList.remove("bi-sun-fill");
        icon.classList.add("bi-moon-stars-fill");
      }
    }

    btn.setAttribute(
      "aria-label",
      currentTheme === "dark" ? "Cambiar a modo claro" : "Cambiar a modo oscuro"
    );
    btn.setAttribute(
      "title",
      currentTheme === "dark" ? "Cambiar a modo claro" : "Cambiar a modo oscuro"
    );
  }

  function setTheme(theme) {
    const t = theme === "dark" ? "dark" : "light";
    document.documentElement.setAttribute("data-theme", t);
    try {
      localStorage.setItem(KEY, t);
    } catch (_) {
      // ignore
    }
    updateToggleUI(t);
  }

  function toggleTheme() {
    const current = document.documentElement.getAttribute("data-theme") || "light";
    setTheme(current === "dark" ? "light" : "dark");
  }

  // Init
  const initial = getInitialTheme();
  setTheme(initial);

  // Migrate away from legacy key
  try {
    if (localStorage.getItem(KEY_LEGACY) !== null) {
      localStorage.removeItem(KEY_LEGACY);
    }
  } catch (_) {
    // ignore
  }

  // Wire up button
  document.addEventListener("DOMContentLoaded", function () {
    const btn = document.getElementById("themeToggle");
    if (!btn) return;

    btn.addEventListener("click", function () {
      toggleTheme();
    });
  });
})();
