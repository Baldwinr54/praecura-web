package com.baldwin.praecura.controller;

import com.baldwin.praecura.dto.SpecialtyForm;
import com.baldwin.praecura.entity.Specialty;
import com.baldwin.praecura.security.SecurityRoleUtils;
import com.baldwin.praecura.service.AuditService;
import com.baldwin.praecura.service.SpecialtyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/specialties")
public class SpecialtyController {

  private final SpecialtyService specialtyService;
  private final AuditService auditService;

  public SpecialtyController(SpecialtyService specialtyService, AuditService auditService) {
    this.specialtyService = specialtyService;
    this.auditService = auditService;
  }

  @GetMapping
  public String list(@RequestParam(value = "q", required = false) String q,
                     @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                     @RequestParam(value = "size", required = false, defaultValue = "10") int size,
                     Model model) {

    PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(5, Math.min(size, 50)), Sort.by(Sort.Direction.DESC, "id"));
    Page<Specialty> specialties = specialtyService.search(q, pageable);

    model.addAttribute("specialtiesPage", specialties);
    model.addAttribute("q", q == null ? "" : q);
    model.addAttribute("size", pageable.getPageSize());
    return "specialties/list";
  }

  @GetMapping("/new")
  public String newForm(Model model, Authentication auth, RedirectAttributes ra) {
    if (!isAdmin(auth)) {
      ra.addFlashAttribute("error", "No autorizado.");
      return "redirect:/specialties";
    }
    model.addAttribute("form", new SpecialtyForm());
    model.addAttribute("isEdit", false);
    return "specialties/form";
  }

  // Backward compatible route: some screens may point to /specialties/{id} instead of /specialties/{id}/edit.
  @GetMapping("/{id}")
  public String legacyEdit(@PathVariable Long id) {
    return "redirect:/specialties/" + id + "/edit";
  }

  @GetMapping("/{id}/edit")
  public String edit(@PathVariable Long id, Model model, RedirectAttributes ra, Authentication auth) {
    if (!isAdmin(auth)) {
      ra.addFlashAttribute("error", "No autorizado.");
      return "redirect:/specialties";
    }
    final Specialty s;
    try {
      s = specialtyService.findById(id);
    } catch (RuntimeException ex) {
      ra.addFlashAttribute("error", "Especialidad no encontrada");
      return "redirect:/specialties";
    }
    SpecialtyForm f = new SpecialtyForm();
    f.setId(s.getId());
    f.setName(s.getName());
    f.setActive(s.isActive());

    model.addAttribute("form", f);
    model.addAttribute("isEdit", true);
    return "specialties/form";
  }

  @PostMapping
  public String save(@Valid @ModelAttribute("form") SpecialtyForm form,
                     BindingResult br,
                     RedirectAttributes ra,
                     Model model,
                     Authentication auth) {

    if (!isAdmin(auth)) {
      ra.addFlashAttribute("error", "No autorizado.");
      return "redirect:/specialties";
    }

    if (br.hasErrors()) {
      model.addAttribute("isEdit", form.getId() != null);
      return "specialties/form";
    }

    boolean isCreate = form.getId() == null;
    final Specialty saved;
    try {
      saved = specialtyService.saveOrUpdate(form);
    } catch (IllegalArgumentException ex) {
      br.rejectValue("name", "dup", ex.getMessage());
      model.addAttribute("isEdit", form.getId() != null);
      return "specialties/form";
    }

    auditService.log(isCreate ? "CREATE_SPECIALTY" : "UPDATE_SPECIALTY",
        "Specialty",
        saved.getId(),
        isCreate
            ? ("Creó especialidad: " + saved.getName())
            : ("Actualizó especialidad: " + saved.getName()));

    ra.addFlashAttribute("success", isCreate ? "Especialidad creada" : "Especialidad actualizada");
    return "redirect:/specialties";
  }

  @PostMapping("/{id}/toggle")
  public String toggle(@PathVariable Long id, RedirectAttributes ra, Authentication auth) {
    if (!isAdmin(auth)) {
      ra.addFlashAttribute("error", "No autorizado.");
      return "redirect:/specialties";
    }
    try {
      Specialty updated = specialtyService.toggleActive(id);

      auditService.log("TOGGLE_SPECIALTY",
          "Specialty",
          updated.getId(),
          (updated.isActive() ? "Activó" : "Desactivó") + " especialidad: " + updated.getName());

      ra.addFlashAttribute("success", updated.isActive() ? "Especialidad activada" : "Especialidad desactivada");
    } catch (RuntimeException ex) {
      ra.addFlashAttribute("error", "No se pudo cambiar el estado");
    }
    return "redirect:/specialties";
  }

  private boolean isAdmin(Authentication auth) {
    return SecurityRoleUtils.hasAdminAuthority(auth);
  }
}
