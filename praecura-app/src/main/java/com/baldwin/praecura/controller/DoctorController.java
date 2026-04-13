package com.baldwin.praecura.controller;

import com.baldwin.praecura.dto.DoctorForm;
import com.baldwin.praecura.entity.Doctor;
import com.baldwin.praecura.service.MedicalServiceService;
import com.baldwin.praecura.service.DoctorService;
import com.baldwin.praecura.service.SpecialtyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/doctors")
public class DoctorController {

  private final DoctorService doctorService;
  private final SpecialtyService specialtyService;
  private final MedicalServiceService medicalServiceService;

  public DoctorController(DoctorService doctorService,
                          SpecialtyService specialtyService,
                          MedicalServiceService medicalServiceService) {
    this.doctorService = doctorService;
    this.specialtyService = specialtyService;
    this.medicalServiceService = medicalServiceService;
  }

  @GetMapping
  public String list(@RequestParam(value = "q", required = false) String q,
                     @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                     @RequestParam(value = "size", required = false, defaultValue = "10") int size,
                     @RequestParam(value = "range", required = false, defaultValue = "30") int range,
                     Model model) {

    PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(5, Math.min(size, 50)), Sort.by(Sort.Direction.DESC, "id"));
    Page<Doctor> doctors = doctorService.search(q, pageable);

    model.addAttribute("doctorsPage", doctors);
    model.addAttribute("q", q == null ? "" : q);
    model.addAttribute("size", pageable.getPageSize());
    model.addAttribute("range", range);
    java.util.List<Long> doctorIds = doctors.getContent().stream()
        .map(com.baldwin.praecura.entity.Doctor::getId)
        .toList();
    model.addAttribute("statsMap", doctorService.statsForDoctors(doctorIds, range));
    return "doctors/list";
  }

  @GetMapping("/new")
  public String newForm(Model model) {
    DoctorForm form = new DoctorForm();
    form.setBufferMinutes(5);
    form.setWorkStart(java.time.LocalTime.of(8, 0));
    form.setWorkEnd(java.time.LocalTime.of(17, 0));
    form.setWorkDays(java.util.List.of("MON", "TUE", "WED", "THU", "FRI"));
    model.addAttribute("form", form);
    model.addAttribute("specialties", specialtyService.listActive());
    model.addAttribute("services", medicalServiceService.listActiveForSelect());
    return "doctors/form";
  }

  @GetMapping("/{id}/edit")
  public String edit(@PathVariable Long id,
                     @RequestParam(value = "range", required = false, defaultValue = "30") int range,
                     Model model) {
    // IMPORTANT: The edit screen needs the doctor's relations initialized.
    // Otherwise, accessing d.getServices() will trigger a LazyInitializationException.
    Doctor d = doctorService.findByIdWithRelations(id);
    DoctorForm f = new DoctorForm();
    f.setId(d.getId());
    f.setFullName(d.getFullName());
    f.setSpecialty(d.getSpecialtyText());
    if (d.getSpecialty() != null) {
      f.setSpecialtyId(d.getSpecialty().getId());
    }
    f.setLicenseNo(d.getLicenseNo());
    
    f.setBufferMinutes(d.getBufferMinutes());
f.setPhone(d.getPhone());
    f.setWorkStart(d.getWorkStart());
    f.setWorkEnd(d.getWorkEnd());
    f.setWorkDays(parseWorkDays(d.getWorkDays()));
    if (d.getServices() != null && !d.getServices().isEmpty()) {
      f.setServiceIds(d.getServices().stream().map(s -> s.getId()).toList());
    }

    model.addAttribute("form", f);
    model.addAttribute("editMode", true);
    model.addAttribute("specialties", specialtyService.listActive());
    model.addAttribute("services", medicalServiceService.listActiveForSelect());
    model.addAttribute("stats", doctorService.stats(id, range));
    model.addAttribute("series", doctorService.doctorSeries(id, range));
    model.addAttribute("range", range);
    return "doctors/form";
  }

  @PostMapping
  public String save(@Valid @ModelAttribute("form") DoctorForm form,
                     BindingResult br,
                     RedirectAttributes ra,
                     Model model) {
    if (br.hasErrors()) {
      model.addAttribute("editMode", form.getId() != null);
      model.addAttribute("specialties", specialtyService.listActive());
      model.addAttribute("services", medicalServiceService.listActiveForSelect());
      return "doctors/form";
    }

    doctorService.saveOrUpdate(form);
    ra.addFlashAttribute("success", form.getId() == null ? "Médico creado" : "Médico actualizado");
    return "redirect:/doctors";
  }

  /**
   * Maneja la actualización cuando el formulario envía a /doctors/{id}.
   * (El template decide la acción basado en si el form tiene ID).
   */
  @PostMapping("/{id}")
  public String update(@PathVariable Long id,
                       @Valid @ModelAttribute("form") DoctorForm form,
                       BindingResult br,
                       RedirectAttributes ra,
                       Model model) {
    // Asegura coherencia entre la ruta y el payload
    if (form.getId() == null) {
      form.setId(id);
    } else if (!id.equals(form.getId())) {
      ra.addFlashAttribute("error", "No se pudo actualizar: ID inválido.");
      return "redirect:/doctors";
    }
    return save(form, br, ra, model);
  }

  @PostMapping("/{id}/deactivate")
  public String deactivate(@PathVariable Long id, RedirectAttributes ra) {
    doctorService.deactivate(id);
    ra.addFlashAttribute("success", "Médico desactivado");
    return "redirect:/doctors";
  }

  private java.util.List<String> parseWorkDays(String raw) {
    if (raw == null || raw.isBlank()) return java.util.List.of();
    String[] parts = raw.split(",");
    java.util.List<String> out = new java.util.ArrayList<>();
    for (String p : parts) {
      if (p == null) continue;
      String t = p.trim();
      if (!t.isEmpty()) out.add(t);
    }
    return out;
  }
}
