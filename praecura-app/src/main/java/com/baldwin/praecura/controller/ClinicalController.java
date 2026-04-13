package com.baldwin.praecura.controller;

import com.baldwin.praecura.dto.AllergyForm;
import com.baldwin.praecura.dto.ClinicalNoteForm;
import com.baldwin.praecura.dto.ConditionForm;
import com.baldwin.praecura.dto.MedicationForm;
import com.baldwin.praecura.dto.VitalForm;
import com.baldwin.praecura.entity.AllergySeverity;
import com.baldwin.praecura.entity.MedicationStatus;
import com.baldwin.praecura.service.ClinicalService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/patients/{id}/clinical")
public class ClinicalController {

  private final ClinicalService clinicalService;

  public ClinicalController(ClinicalService clinicalService) {
    this.clinicalService = clinicalService;
  }

  @GetMapping
  public String view(@PathVariable Long id, Model model) {
    var patient = clinicalService.getPatient(id);
    model.addAttribute("patient", patient);

    model.addAttribute("allergies", clinicalService.listAllergies(id));
    model.addAttribute("conditions", clinicalService.listConditions(id));
    model.addAttribute("medications", clinicalService.listMedications(id));
    model.addAttribute("vitals", clinicalService.listVitals(id));
    model.addAttribute("notes", clinicalService.listNotes(id));

    model.addAttribute("allergyForm", new AllergyForm());
    model.addAttribute("conditionForm", new ConditionForm());
    model.addAttribute("medicationForm", new MedicationForm());
    model.addAttribute("vitalForm", new VitalForm());
    model.addAttribute("noteForm", new ClinicalNoteForm());
    model.addAttribute("allergySeverities", AllergySeverity.values());
    model.addAttribute("medicationStatuses", MedicationStatus.values());
    return "patients/clinical";
  }

  @PostMapping("/allergies")
  public String addAllergy(@PathVariable Long id,
                           @Valid AllergyForm allergyForm,
                           BindingResult br,
                           RedirectAttributes ra,
                           Model model) {
    if (br.hasErrors()) {
      return view(id, model);
    }
    try {
      clinicalService.addAllergy(id, allergyForm);
      ra.addFlashAttribute("success", "Alergia registrada.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/patients/" + id + "/clinical";
  }

  @PostMapping("/allergies/{allergyId}/resolve")
  public String resolveAllergy(@PathVariable Long id,
                               @PathVariable Long allergyId,
                               RedirectAttributes ra) {
    try {
      clinicalService.resolveAllergy(id, allergyId);
      ra.addFlashAttribute("success", "Alergia marcada como resuelta.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/patients/" + id + "/clinical";
  }

  @PostMapping("/conditions")
  public String addCondition(@PathVariable Long id,
                             @Valid ConditionForm conditionForm,
                             BindingResult br,
                             RedirectAttributes ra,
                             Model model) {
    if (br.hasErrors()) {
      return view(id, model);
    }
    try {
      clinicalService.addCondition(id, conditionForm);
      ra.addFlashAttribute("success", "Condición registrada.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/patients/" + id + "/clinical";
  }

  @PostMapping("/conditions/{conditionId}/resolve")
  public String resolveCondition(@PathVariable Long id,
                                 @PathVariable Long conditionId,
                                 RedirectAttributes ra) {
    try {
      clinicalService.resolveCondition(id, conditionId);
      ra.addFlashAttribute("success", "Condición marcada como resuelta.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/patients/" + id + "/clinical";
  }

  @PostMapping("/medications")
  public String addMedication(@PathVariable Long id,
                              @Valid MedicationForm medicationForm,
                              BindingResult br,
                              RedirectAttributes ra,
                              Model model) {
    if (br.hasErrors()) {
      return view(id, model);
    }
    try {
      clinicalService.addMedication(id, medicationForm);
      ra.addFlashAttribute("success", "Medicamento registrado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/patients/" + id + "/clinical";
  }

  @PostMapping("/medications/{medicationId}/stop")
  public String stopMedication(@PathVariable Long id,
                               @PathVariable Long medicationId,
                               RedirectAttributes ra) {
    try {
      clinicalService.stopMedication(id, medicationId);
      ra.addFlashAttribute("success", "Medicamento marcado como suspendido.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/patients/" + id + "/clinical";
  }

  @PostMapping("/vitals")
  public String addVital(@PathVariable Long id,
                         @Valid VitalForm vitalForm,
                         BindingResult br,
                         RedirectAttributes ra,
                         Model model) {
    if (br.hasErrors()) {
      return view(id, model);
    }
    try {
      clinicalService.addVital(id, vitalForm);
      ra.addFlashAttribute("success", "Signos vitales registrados.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/patients/" + id + "/clinical";
  }

  @PostMapping("/vitals/{vitalId}/delete")
  public String deleteVital(@PathVariable Long id,
                            @PathVariable Long vitalId,
                            RedirectAttributes ra) {
    try {
      clinicalService.deleteVital(id, vitalId);
      ra.addFlashAttribute("success", "Registro eliminado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/patients/" + id + "/clinical";
  }

  @PostMapping("/notes")
  public String addNote(@PathVariable Long id,
                        @Valid ClinicalNoteForm noteForm,
                        BindingResult br,
                        RedirectAttributes ra,
                        Model model) {
    if (br.hasErrors()) {
      return view(id, model);
    }
    try {
      clinicalService.addNote(id, noteForm);
      ra.addFlashAttribute("success", "Nota clínica registrada.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/patients/" + id + "/clinical";
  }

  @PostMapping("/notes/{noteId}/delete")
  public String deleteNote(@PathVariable Long id,
                           @PathVariable Long noteId,
                           RedirectAttributes ra) {
    try {
      clinicalService.deleteNote(id, noteId);
      ra.addFlashAttribute("success", "Nota eliminada.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/patients/" + id + "/clinical";
  }
}
