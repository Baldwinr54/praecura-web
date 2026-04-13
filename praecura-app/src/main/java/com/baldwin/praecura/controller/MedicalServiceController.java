package com.baldwin.praecura.controller;

import com.baldwin.praecura.dto.MedicalServiceForm;
import com.baldwin.praecura.entity.MedicalService;
import com.baldwin.praecura.service.MedicalServiceService;
import jakarta.validation.Valid;
import java.beans.PropertyEditorSupport;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/services")
public class MedicalServiceController {

  private final MedicalServiceService medicalServiceService;

  public MedicalServiceController(MedicalServiceService medicalServiceService) {
    this.medicalServiceService = medicalServiceService;
  }

  @InitBinder("form")
  public void initBinder(WebDataBinder binder) {
    // Accept flexible inputs like "59", "59.59" or "59:59" and store only minutes.
    binder.registerCustomEditor(int.class, "durationMinutes", new PropertyEditorSupport() {
      @Override
      public void setAsText(String text) {
        setValue(parseMinutesToInt(text));
      }
    });
  }

  private static int parseMinutesToInt(String raw) {
    if (raw == null) return 0;
    String v = raw.trim();
    if (v.isEmpty()) return 0;

    // Keep digits and common separators only.
    v = v.replace(',', '.');
    v = v.replaceAll("[^0-9:.]", "");

    String minutesPart = v;
    if (v.contains(":")) minutesPart = v.substring(0, v.indexOf(':'));
    if (minutesPart.contains(".")) minutesPart = minutesPart.substring(0, minutesPart.indexOf('.'));
    if (minutesPart.isEmpty()) return 0;

    try {
      int minutes = Integer.parseInt(minutesPart);
      return Math.max(minutes, 0);
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  @GetMapping
  public String list(@RequestParam(value = "q", required = false) String q,
                     @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                     @RequestParam(value = "size", required = false, defaultValue = "10") int size,
                     Model model) {

    PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(5, Math.min(size, 50)), Sort.by(Sort.Direction.DESC, "id"));
    Page<MedicalService> services = medicalServiceService.search(q, pageable);

    model.addAttribute("servicesPage", services);
    model.addAttribute("q", q == null ? "" : q);
    model.addAttribute("size", pageable.getPageSize());
    return "services/list";
  }

  @GetMapping("/new")
  public String newForm(Model model) {
    model.addAttribute("form", new MedicalServiceForm());
    return "services/form";
  }

  // Backward compatible route: some screens may point to /services/{id} instead of /services/{id}/edit.
  @GetMapping("/{id}")
  public String legacyEdit(@PathVariable Long id) {
    return "redirect:/services/" + id + "/edit";
  }

  @GetMapping("/{id}/edit")
  public String edit(@PathVariable Long id, Model model, RedirectAttributes ra) {
    final MedicalService s;
    try {
      s = medicalServiceService.findById(id);
    } catch (RuntimeException ex) {
      ra.addFlashAttribute("error", "Servicio no encontrado");
      return "redirect:/services";
    }
    MedicalServiceForm f = new MedicalServiceForm();
    f.setId(s.getId());
    f.setName(s.getName());
    f.setDurationMinutes(s.getDurationMinutes());
    f.setPrice(s.getPrice());

    model.addAttribute("form", f);
    model.addAttribute("editMode", true);
    return "services/form";
  }

  @PostMapping
  public String save(@Valid @ModelAttribute("form") MedicalServiceForm form,
                     BindingResult br,
                     RedirectAttributes ra,
                     Model model) {
    if (br.hasErrors()) {
      model.addAttribute("editMode", form.getId() != null);
      return "services/form";
    }

    medicalServiceService.saveOrUpdate(form);
    ra.addFlashAttribute("success", form.getId() == null ? "Servicio creado" : "Servicio actualizado");
    return "redirect:/services";
  }

  /**
   * Update route used by the edit form (action="/services/{id}").
   * Without this mapping, the browser submits a POST to /services/{id} and Spring returns 405.
   */
  @PostMapping("/{id}")
  public String update(@PathVariable Long id,
                       @Valid @ModelAttribute("form") MedicalServiceForm form,
                       BindingResult br,
                       RedirectAttributes ra,
                       Model model) {
    // Ensure the id from the path is the source of truth.
    form.setId(id);
    return save(form, br, ra, model);
  }

  @PostMapping("/{id}/deactivate")
  public String deactivate(@PathVariable Long id, RedirectAttributes ra) {
    medicalServiceService.deactivate(id);
    ra.addFlashAttribute("success", "Servicio desactivado");
    return "redirect:/services";
  }
}
