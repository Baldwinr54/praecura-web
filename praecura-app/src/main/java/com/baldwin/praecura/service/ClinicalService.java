package com.baldwin.praecura.service;

import com.baldwin.praecura.dto.AllergyForm;
import com.baldwin.praecura.dto.ClinicalNoteForm;
import com.baldwin.praecura.dto.ConditionForm;
import com.baldwin.praecura.dto.MedicationForm;
import com.baldwin.praecura.dto.VitalForm;
import com.baldwin.praecura.entity.AllergyStatus;
import com.baldwin.praecura.entity.ClinicalAllergy;
import com.baldwin.praecura.entity.ClinicalCondition;
import com.baldwin.praecura.entity.ClinicalMedication;
import com.baldwin.praecura.entity.ClinicalNote;
import com.baldwin.praecura.entity.ClinicalVital;
import com.baldwin.praecura.entity.ConditionStatus;
import com.baldwin.praecura.entity.MedicationStatus;
import com.baldwin.praecura.entity.Patient;
import com.baldwin.praecura.repository.ClinicalAllergyRepository;
import com.baldwin.praecura.repository.ClinicalConditionRepository;
import com.baldwin.praecura.repository.ClinicalMedicationRepository;
import com.baldwin.praecura.repository.ClinicalNoteRepository;
import com.baldwin.praecura.repository.ClinicalVitalRepository;
import com.baldwin.praecura.repository.PatientRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClinicalService {

  private final PatientRepository patientRepository;
  private final ClinicalAllergyRepository allergyRepository;
  private final ClinicalConditionRepository conditionRepository;
  private final ClinicalMedicationRepository medicationRepository;
  private final ClinicalVitalRepository vitalRepository;
  private final ClinicalNoteRepository noteRepository;

  public ClinicalService(PatientRepository patientRepository,
                         ClinicalAllergyRepository allergyRepository,
                         ClinicalConditionRepository conditionRepository,
                         ClinicalMedicationRepository medicationRepository,
                         ClinicalVitalRepository vitalRepository,
                         ClinicalNoteRepository noteRepository) {
    this.patientRepository = patientRepository;
    this.allergyRepository = allergyRepository;
    this.conditionRepository = conditionRepository;
    this.medicationRepository = medicationRepository;
    this.vitalRepository = vitalRepository;
    this.noteRepository = noteRepository;
  }

  public Patient getPatient(Long id) {
    return patientRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado."));
  }

  public List<ClinicalAllergy> listAllergies(Long patientId) {
    return allergyRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
  }

  public List<ClinicalCondition> listConditions(Long patientId) {
    return conditionRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
  }

  public List<ClinicalMedication> listMedications(Long patientId) {
    return medicationRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
  }

  public List<ClinicalVital> listVitals(Long patientId) {
    return vitalRepository.findByPatientIdOrderByRecordedAtDesc(patientId);
  }

  public List<ClinicalNote> listNotes(Long patientId) {
    return noteRepository.findByPatientIdOrderByRecordedAtDesc(patientId);
  }

  public ClinicalSummary summary(Long patientId) {
    long activeAllergies = allergyRepository.countByPatientIdAndStatus(patientId, AllergyStatus.ACTIVE);
    long activeConditions = conditionRepository.countByPatientIdAndStatus(patientId, ConditionStatus.ACTIVE);
    long activeMeds = medicationRepository.countByPatientIdAndStatus(patientId, MedicationStatus.ACTIVE);
    long notes = noteRepository.countByPatientId(patientId);
    ClinicalVital lastVital = vitalRepository.findByPatientIdOrderByRecordedAtDesc(patientId)
        .stream()
        .findFirst()
        .orElse(null);
    return new ClinicalSummary(activeAllergies, activeConditions, activeMeds, notes, lastVital);
  }

  @Transactional
  public ClinicalAllergy addAllergy(Long patientId, AllergyForm form) {
    Patient p = getPatient(patientId);
    ClinicalAllergy a = new ClinicalAllergy();
    a.setPatient(p);
    a.setAllergen(form.getAllergen().trim());
    a.setReaction(trimToNull(form.getReaction()));
    a.setSeverity(form.getSeverity());
    a.setNotes(trimToNull(form.getNotes()));
    stampCreate(a);
    return allergyRepository.save(a);
  }

  @Transactional
  public ClinicalCondition addCondition(Long patientId, ConditionForm form) {
    Patient p = getPatient(patientId);
    ClinicalCondition c = new ClinicalCondition();
    c.setPatient(p);
    c.setName(form.getName().trim());
    c.setIcd10Code(trimToNull(form.getIcd10Code()));
    c.setOnsetDate(form.getOnsetDate());
    c.setNotes(trimToNull(form.getNotes()));
    stampCreate(c);
    return conditionRepository.save(c);
  }

  @Transactional
  public ClinicalMedication addMedication(Long patientId, MedicationForm form) {
    Patient p = getPatient(patientId);
    ClinicalMedication m = new ClinicalMedication();
    m.setPatient(p);
    m.setName(form.getName().trim());
    m.setDosage(trimToNull(form.getDosage()));
    m.setFrequency(trimToNull(form.getFrequency()));
    m.setStartDate(form.getStartDate());
    m.setEndDate(form.getEndDate());
    m.setStatus(form.getStatus() != null ? form.getStatus() : MedicationStatus.ACTIVE);
    m.setNotes(trimToNull(form.getNotes()));
    stampCreate(m);
    return medicationRepository.save(m);
  }

  @Transactional
  public ClinicalVital addVital(Long patientId, VitalForm form) {
    Patient p = getPatient(patientId);
    ClinicalVital v = new ClinicalVital();
    v.setPatient(p);
    v.setRecordedAt(form.getRecordedAt() != null ? form.getRecordedAt() : LocalDateTime.now());
    v.setWeightKg(form.getWeightKg());
    v.setHeightCm(form.getHeightCm());
    v.setTemperatureC(form.getTemperatureC());
    v.setHeartRate(form.getHeartRate());
    v.setRespiratoryRate(form.getRespiratoryRate());
    v.setBloodPressure(trimToNull(form.getBloodPressure()));
    v.setOxygenSaturation(form.getOxygenSaturation());
    v.setNotes(trimToNull(form.getNotes()));
    v.setCreatedBy(currentUsername());
    v.setCreatedAt(LocalDateTime.now());
    return vitalRepository.save(v);
  }

  @Transactional
  public ClinicalNote addNote(Long patientId, ClinicalNoteForm form) {
    Patient p = getPatient(patientId);
    ClinicalNote n = new ClinicalNote();
    n.setPatient(p);
    n.setTitle(form.getTitle().trim());
    n.setNote(form.getNote().trim());
    n.setRecordedAt(form.getRecordedAt() != null ? form.getRecordedAt() : LocalDateTime.now());
    stampCreate(n);
    return noteRepository.save(n);
  }

  @Transactional
  public void resolveAllergy(Long patientId, Long allergyId) {
    ClinicalAllergy a = allergyRepository.findById(allergyId)
        .orElseThrow(() -> new IllegalArgumentException("Alergia no encontrada."));
    ensurePatientMatch(patientId, a.getPatient().getId());
    a.setStatus(AllergyStatus.RESOLVED);
    stampUpdate(a);
    allergyRepository.save(a);
  }

  @Transactional
  public void resolveCondition(Long patientId, Long conditionId) {
    ClinicalCondition c = conditionRepository.findById(conditionId)
        .orElseThrow(() -> new IllegalArgumentException("Condición no encontrada."));
    ensurePatientMatch(patientId, c.getPatient().getId());
    c.setStatus(ConditionStatus.RESOLVED);
    stampUpdate(c);
    conditionRepository.save(c);
  }

  @Transactional
  public void stopMedication(Long patientId, Long medicationId) {
    ClinicalMedication m = medicationRepository.findById(medicationId)
        .orElseThrow(() -> new IllegalArgumentException("Medicamento no encontrado."));
    ensurePatientMatch(patientId, m.getPatient().getId());
    m.setStatus(MedicationStatus.STOPPED);
    m.setEndDate(m.getEndDate() != null ? m.getEndDate() : java.time.LocalDate.now());
    stampUpdate(m);
    medicationRepository.save(m);
  }

  @Transactional
  public void deleteVital(Long patientId, Long vitalId) {
    ClinicalVital v = vitalRepository.findById(vitalId)
        .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado."));
    ensurePatientMatch(patientId, v.getPatient().getId());
    vitalRepository.delete(v);
  }

  @Transactional
  public void deleteNote(Long patientId, Long noteId) {
    ClinicalNote n = noteRepository.findById(noteId)
        .orElseThrow(() -> new IllegalArgumentException("Nota no encontrada."));
    ensurePatientMatch(patientId, n.getPatient().getId());
    noteRepository.delete(n);
  }

  private void ensurePatientMatch(Long expected, Long actual) {
    if (expected == null || actual == null || !expected.equals(actual)) {
      throw new IllegalArgumentException("Paciente inválido para esta operación.");
    }
  }

  private void stampCreate(Object entity) {
    String user = currentUsername();
    LocalDateTime now = LocalDateTime.now();
    if (entity instanceof ClinicalAllergy a) {
      a.setCreatedBy(user);
      a.setUpdatedBy(user);
      a.setCreatedAt(now);
      a.setUpdatedAt(now);
    } else if (entity instanceof ClinicalCondition c) {
      c.setCreatedBy(user);
      c.setUpdatedBy(user);
      c.setCreatedAt(now);
      c.setUpdatedAt(now);
    } else if (entity instanceof ClinicalMedication m) {
      m.setCreatedBy(user);
      m.setUpdatedBy(user);
      m.setCreatedAt(now);
      m.setUpdatedAt(now);
    } else if (entity instanceof ClinicalNote n) {
      n.setCreatedBy(user);
      n.setUpdatedBy(user);
      n.setCreatedAt(now);
      n.setUpdatedAt(now);
    }
  }

  private void stampUpdate(Object entity) {
    String user = currentUsername();
    LocalDateTime now = LocalDateTime.now();
    if (entity instanceof ClinicalAllergy a) {
      a.setUpdatedBy(user);
      a.setUpdatedAt(now);
    } else if (entity instanceof ClinicalCondition c) {
      c.setUpdatedBy(user);
      c.setUpdatedAt(now);
    } else if (entity instanceof ClinicalMedication m) {
      m.setUpdatedBy(user);
      m.setUpdatedAt(now);
    } else if (entity instanceof ClinicalNote n) {
      n.setUpdatedBy(user);
      n.setUpdatedAt(now);
    }
  }

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()) {
      return auth.getName();
    }
    return null;
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public record ClinicalSummary(
      long activeAllergies,
      long activeConditions,
      long activeMedications,
      long notesCount,
      ClinicalVital lastVital
  ) {}
}
