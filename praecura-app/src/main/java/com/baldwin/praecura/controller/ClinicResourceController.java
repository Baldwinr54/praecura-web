package com.baldwin.praecura.controller;

import com.baldwin.praecura.dto.ClinicResourceForm;
import com.baldwin.praecura.entity.ClinicResource;
import com.baldwin.praecura.entity.ResourceType;
import com.baldwin.praecura.service.ClinicResourceService;
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
@RequestMapping("/resources")
public class ClinicResourceController {

  private final ClinicResourceService clinicResourceService;
  private final ClinicSiteService clinicSiteService;

  public ClinicResourceController(ClinicResourceService clinicResourceService,
                                  ClinicSiteService clinicSiteService) {
    this.clinicResourceService = clinicResourceService;
    this.clinicSiteService = clinicSiteService;
  }

  @GetMapping
  public String list(@RequestParam(value = "q", required = false) String q,
                     @RequestParam(value = "siteId", required = false) Long siteId,
                     @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                     @RequestParam(value = "size", required = false, defaultValue = "10") int size,
                     Model model) {

    PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(5, Math.min(size, 50)), Sort.by(Sort.Direction.ASC, "name"));
    Page<ClinicResource> resources = clinicResourceService.search(q, siteId, pageable);

    model.addAttribute("resourcesPage", resources);
    model.addAttribute("q", q == null ? "" : q);
    model.addAttribute("siteId", siteId);
    model.addAttribute("size", pageable.getPageSize());
    model.addAttribute("sites", clinicSiteService.listActiveForSelect());
    return "resources/list";
  }

  @GetMapping("/new")
  public String newForm(Model model) {
    model.addAttribute("form", new ClinicResourceForm());
    loadLists(model);
    return "resources/form";
  }

  @GetMapping("/{id}")
  public String legacyEdit(@PathVariable Long id) {
    return "redirect:/resources/" + id + "/edit";
  }

  @GetMapping("/{id}/edit")
  public String edit(@PathVariable Long id, Model model, RedirectAttributes ra) {
    final ClinicResource r;
    try {
      r = clinicResourceService.findById(id);
    } catch (RuntimeException ex) {
      ra.addFlashAttribute("error", "Recurso no encontrado");
      return "redirect:/resources";
    }

    ClinicResourceForm f = new ClinicResourceForm();
    f.setId(r.getId());
    f.setName(r.getName());
    f.setType(r.getType());
    f.setSiteId(r.getSite() != null ? r.getSite().getId() : null);
    f.setNotes(r.getNotes());

    model.addAttribute("form", f);
    model.addAttribute("editMode", true);
    loadLists(model);
    return "resources/form";
  }

  @PostMapping
  public String save(@Valid @ModelAttribute("form") ClinicResourceForm form,
                     BindingResult br,
                     RedirectAttributes ra,
                     Model model) {
    if (br.hasErrors()) {
      model.addAttribute("editMode", form.getId() != null);
      loadLists(model);
      return "resources/form";
    }

    try {
      clinicResourceService.saveOrUpdate(form);
      ra.addFlashAttribute("success", form.getId() == null ? "Recurso creado" : "Recurso actualizado");
      return "redirect:/resources";
    } catch (IllegalArgumentException ex) {
      br.reject("error.resource", ex.getMessage());
      model.addAttribute("editMode", form.getId() != null);
      loadLists(model);
      return "resources/form";
    }
  }

  @PostMapping("/{id}")
  public String update(@PathVariable Long id,
                       @Valid @ModelAttribute("form") ClinicResourceForm form,
                       BindingResult br,
                       RedirectAttributes ra,
                       Model model) {
    form.setId(id);
    return save(form, br, ra, model);
  }

  @PostMapping("/{id}/deactivate")
  public String deactivate(@PathVariable Long id, RedirectAttributes ra) {
    clinicResourceService.deactivate(id);
    ra.addFlashAttribute("success", "Recurso desactivado");
    return "redirect:/resources";
  }

  private void loadLists(Model model) {
    model.addAttribute("sites", clinicSiteService.listActiveForSelect());
    model.addAttribute("types", ResourceType.values());
  }
}
