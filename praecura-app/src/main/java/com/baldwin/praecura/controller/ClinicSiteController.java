package com.baldwin.praecura.controller;

import com.baldwin.praecura.dto.ClinicSiteForm;
import com.baldwin.praecura.entity.ClinicSite;
import com.baldwin.praecura.service.ClinicSiteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/sites")
public class ClinicSiteController {

  private final ClinicSiteService clinicSiteService;

  public ClinicSiteController(ClinicSiteService clinicSiteService) {
    this.clinicSiteService = clinicSiteService;
  }

  @GetMapping
  public String list(@RequestParam(value = "q", required = false) String q,
                     @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                     @RequestParam(value = "size", required = false, defaultValue = "10") int size,
                     Model model) {

    PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(5, Math.min(size, 50)), Sort.by(Sort.Direction.ASC, "name"));
    Page<ClinicSite> sites = clinicSiteService.search(q, pageable);

    model.addAttribute("sitesPage", sites);
    model.addAttribute("q", q == null ? "" : q);
    model.addAttribute("size", pageable.getPageSize());
    return "sites/list";
  }

  @GetMapping("/new")
  public String newForm(Model model) {
    model.addAttribute("form", new ClinicSiteForm());
    return "sites/form";
  }

  @GetMapping("/{id}")
  public String legacyEdit(@PathVariable Long id) {
    return "redirect:/sites/" + id + "/edit";
  }

  @GetMapping("/{id}/edit")
  public String edit(@PathVariable Long id, Model model, RedirectAttributes ra) {
    final ClinicSite s;
    try {
      s = clinicSiteService.findById(id);
    } catch (RuntimeException ex) {
      ra.addFlashAttribute("error", "Sede no encontrada");
      return "redirect:/sites";
    }

    ClinicSiteForm f = new ClinicSiteForm();
    f.setId(s.getId());
    f.setName(s.getName());
    f.setCode(s.getCode());
    f.setAddress(s.getAddress());
    f.setPhone(s.getPhone());
    f.setNotes(s.getNotes());

    model.addAttribute("form", f);
    model.addAttribute("editMode", true);
    return "sites/form";
  }

  @PostMapping
  public String save(@Valid @ModelAttribute("form") ClinicSiteForm form,
                     BindingResult br,
                     RedirectAttributes ra,
                     Model model) {
    if (br.hasErrors()) {
      model.addAttribute("editMode", form.getId() != null);
      return "sites/form";
    }

    clinicSiteService.saveOrUpdate(form);
    ra.addFlashAttribute("success", form.getId() == null ? "Sede creada" : "Sede actualizada");
    return "redirect:/sites";
  }

  @PostMapping("/{id}")
  public String update(@PathVariable Long id,
                       @Valid @ModelAttribute("form") ClinicSiteForm form,
                       BindingResult br,
                       RedirectAttributes ra,
                       Model model) {
    form.setId(id);
    return save(form, br, ra, model);
  }

  @PostMapping("/{id}/deactivate")
  public String deactivate(@PathVariable Long id, RedirectAttributes ra) {
    clinicSiteService.deactivate(id);
    ra.addFlashAttribute("success", "Sede desactivada");
    return "redirect:/sites";
  }
}
