package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.AdmissionStatus;
import com.baldwin.praecura.entity.BedStatus;
import com.baldwin.praecura.entity.InpatientBed;
import com.baldwin.praecura.entity.NursingNote;
import com.baldwin.praecura.entity.PatientAdmission;
import com.baldwin.praecura.entity.SurgerySchedule;
import com.baldwin.praecura.entity.SurgeryStatus;
import com.baldwin.praecura.repository.ClinicResourceRepository;
import com.baldwin.praecura.repository.ClinicSiteRepository;
import com.baldwin.praecura.repository.DoctorRepository;
import com.baldwin.praecura.repository.InpatientBedRepository;
import com.baldwin.praecura.repository.NursingNoteRepository;
import com.baldwin.praecura.repository.PatientAdmissionRepository;
import com.baldwin.praecura.repository.PatientRepository;
import com.baldwin.praecura.repository.SurgeryScheduleRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InpatientService {

  private final InpatientBedRepository bedRepository;
  private final PatientAdmissionRepository admissionRepository;
  private final SurgeryScheduleRepository surgeryRepository;
  private final NursingNoteRepository nursingNoteRepository;
  private final ClinicSiteRepository clinicSiteRepository;
  private final PatientRepository patientRepository;
  private final DoctorRepository doctorRepository;
  private final ClinicResourceRepository clinicResourceRepository;
  private final AuditService auditService;

  public InpatientService(InpatientBedRepository bedRepository,
                          PatientAdmissionRepository admissionRepository,
                          SurgeryScheduleRepository surgeryRepository,
                          NursingNoteRepository nursingNoteRepository,
                          ClinicSiteRepository clinicSiteRepository,
                          PatientRepository patientRepository,
                          DoctorRepository doctorRepository,
                          ClinicResourceRepository clinicResourceRepository,
                          AuditService auditService) {
    this.bedRepository = bedRepository;
    this.admissionRepository = admissionRepository;
    this.surgeryRepository = surgeryRepository;
    this.nursingNoteRepository = nursingNoteRepository;
    this.clinicSiteRepository = clinicSiteRepository;
    this.patientRepository = patientRepository;
    this.doctorRepository = doctorRepository;
    this.clinicResourceRepository = clinicResourceRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<InpatientBed> listBeds() {
    return bedRepository.findAllByOrderBySiteNameAscCodeAsc();
  }

  @Transactional(readOnly = true)
  public List<PatientAdmission> listAdmissions() {
    return admissionRepository.findAllByOrderByAdmittedAtDesc();
  }

  @Transactional(readOnly = true)
  public List<SurgerySchedule> listSurgeries() {
    return surgeryRepository.findAllByOrderByScheduledAtDesc();
  }

  @Transactional(readOnly = true)
  public List<NursingNote> listNursingNotes(Long admissionId) {
    if (admissionId == null) return List.of();
    return nursingNoteRepository.findByAdmissionIdOrderByRecordedAtDesc(admissionId);
  }

  @Transactional
  public InpatientBed createBed(Long siteId, String code, String ward, String bedType, String notes) {
    if (siteId == null || code == null || code.isBlank()) {
      throw new IllegalArgumentException("Sede y código de cama son obligatorios.");
    }

    InpatientBed bed = new InpatientBed();
    bed.setSite(clinicSiteRepository.findById(siteId)
        .orElseThrow(() -> new IllegalArgumentException("Sede no existe.")));
    bed.setCode(code.trim().toUpperCase());
    bed.setWard(trimToNull(ward));
    bed.setBedType(trimToNull(bedType) != null ? bedType.trim().toUpperCase() : "GENERAL");
    bed.setStatus(BedStatus.AVAILABLE);
    bed.setNotes(trimToNull(notes));
    bed.setCreatedAt(LocalDateTime.now());
    bed.setUpdatedAt(LocalDateTime.now());
    bedRepository.save(bed);

    auditService.log("INPATIENT_BED_CREATED", "InpatientBed", bed.getId(), "siteId=" + siteId + ", code=" + bed.getCode());
    return bed;
  }

  @Transactional
  public PatientAdmission admitPatient(Long patientId,
                                       Long bedId,
                                       Long doctorId,
                                       String reason,
                                       LocalDateTime expectedDischarge,
                                       Authentication authentication) {
    if (patientId == null) throw new IllegalArgumentException("Paciente requerido.");

    PatientAdmission admission = new PatientAdmission();
    admission.setPatient(patientRepository.findById(patientId)
        .orElseThrow(() -> new IllegalArgumentException("Paciente no existe.")));
    if (bedId != null) {
      InpatientBed bed = bedRepository.findById(bedId)
          .orElseThrow(() -> new IllegalArgumentException("Cama no existe."));
      bed.setStatus(BedStatus.OCCUPIED);
      bed.setUpdatedAt(LocalDateTime.now());
      bedRepository.save(bed);
      admission.setBed(bed);
    }
    if (doctorId != null) {
      admission.setDoctor(doctorRepository.findById(doctorId).orElse(null));
    }

    admission.setStatus(AdmissionStatus.ADMITTED);
    admission.setAdmittedAt(LocalDateTime.now());
    admission.setExpectedDischargeAt(expectedDischarge);
    admission.setAdmissionReason(trimToNull(reason));
    admission.setCreatedBy(username(authentication));
    admission.setUpdatedBy(username(authentication));
    admission.setCreatedAt(LocalDateTime.now());
    admission.setUpdatedAt(LocalDateTime.now());
    admissionRepository.save(admission);

    auditService.log("PATIENT_ADMITTED", "PatientAdmission", admission.getId(), "patientId=" + patientId);
    return admission;
  }

  @Transactional
  public void updateAdmissionStatus(Long admissionId, AdmissionStatus status, String dischargeSummary, Authentication authentication) {
    PatientAdmission admission = admissionRepository.findById(admissionId)
        .orElseThrow(() -> new IllegalArgumentException("Admisión no existe."));

    if (status != null) {
      admission.setStatus(status);
      if (status == AdmissionStatus.DISCHARGED) {
        admission.setDischargedAt(LocalDateTime.now());
        if (admission.getBed() != null) {
          InpatientBed bed = admission.getBed();
          bed.setStatus(BedStatus.CLEANING);
          bed.setUpdatedAt(LocalDateTime.now());
          bedRepository.save(bed);
        }
      }
    }

    if (dischargeSummary != null) {
      admission.setDischargeSummary(trimToNull(dischargeSummary));
    }
    admission.setUpdatedAt(LocalDateTime.now());
    admission.setUpdatedBy(username(authentication));
    admissionRepository.save(admission);

    auditService.log("PATIENT_ADMISSION_UPDATED", "PatientAdmission", admissionId, "status=" + admission.getStatus());
  }

  @Transactional
  public SurgerySchedule scheduleSurgery(Long patientId,
                                         Long admissionId,
                                         Long doctorId,
                                         Long siteId,
                                         Long resourceId,
                                         String procedureName,
                                         String anesthesiaType,
                                         LocalDateTime scheduledAt,
                                         Integer estimatedMinutes,
                                         String notes,
                                         Authentication authentication) {
    if (patientId == null || procedureName == null || procedureName.isBlank() || scheduledAt == null) {
      throw new IllegalArgumentException("Paciente, procedimiento y fecha/hora son obligatorios.");
    }

    SurgerySchedule surgery = new SurgerySchedule();
    surgery.setPatient(patientRepository.findById(patientId)
        .orElseThrow(() -> new IllegalArgumentException("Paciente no existe.")));
    surgery.setAdmission(admissionId != null ? admissionRepository.findById(admissionId).orElse(null) : null);
    surgery.setDoctor(doctorId != null ? doctorRepository.findById(doctorId).orElse(null) : null);
    surgery.setSite(siteId != null ? clinicSiteRepository.findById(siteId).orElse(null) : null);
    surgery.setResource(resourceId != null ? clinicResourceRepository.findById(resourceId).orElse(null) : null);
    surgery.setProcedureName(procedureName.trim());
    surgery.setAnesthesiaType(trimToNull(anesthesiaType));
    surgery.setScheduledAt(scheduledAt);
    surgery.setEstimatedMinutes(estimatedMinutes != null && estimatedMinutes > 0 ? estimatedMinutes : 60);
    surgery.setNotes(trimToNull(notes));
    surgery.setStatus(SurgeryStatus.SCHEDULED);
    surgery.setCreatedBy(username(authentication));
    surgery.setCreatedAt(LocalDateTime.now());
    surgery.setUpdatedAt(LocalDateTime.now());
    surgeryRepository.save(surgery);

    auditService.log("SURGERY_SCHEDULED", "SurgerySchedule", surgery.getId(), "patientId=" + patientId);
    return surgery;
  }

  @Transactional
  public void updateSurgeryStatus(Long surgeryId, SurgeryStatus status, String notes) {
    SurgerySchedule surgery = surgeryRepository.findById(surgeryId)
        .orElseThrow(() -> new IllegalArgumentException("Cirugía no existe."));

    if (status != null) {
      surgery.setStatus(status);
      if (status == SurgeryStatus.IN_PROGRESS) {
        surgery.setStartedAt(LocalDateTime.now());
      }
      if (status == SurgeryStatus.COMPLETED) {
        if (surgery.getStartedAt() == null) {
          surgery.setStartedAt(LocalDateTime.now());
        }
        surgery.setEndedAt(LocalDateTime.now());
      }
    }
    if (notes != null) {
      surgery.setNotes(trimToNull(notes));
    }
    surgery.setUpdatedAt(LocalDateTime.now());
    surgeryRepository.save(surgery);

    auditService.log("SURGERY_UPDATED", "SurgerySchedule", surgeryId, "status=" + surgery.getStatus());
  }

  @Transactional
  public NursingNote addNursingNote(Long admissionId,
                                    String shift,
                                    String notes,
                                    String vitalsSnapshot,
                                    String medicationAdministered,
                                    boolean adverseEvent,
                                    Authentication authentication) {
    if (admissionId == null) throw new IllegalArgumentException("Admisión requerida.");
    String body = trimToNull(notes);
    if (body == null) throw new IllegalArgumentException("La nota de enfermería es obligatoria.");

    NursingNote note = new NursingNote();
    note.setAdmission(admissionRepository.findById(admissionId)
        .orElseThrow(() -> new IllegalArgumentException("Admisión no existe.")));
    note.setShift(trimToNull(shift) != null ? shift.trim().toUpperCase() : "AM");
    note.setNotes(body);
    note.setVitalsSnapshot(trimToNull(vitalsSnapshot));
    note.setMedicationAdministered(trimToNull(medicationAdministered));
    note.setAdverseEvent(adverseEvent);
    note.setRecordedBy(username(authentication));
    note.setRecordedAt(LocalDateTime.now());
    note.setCreatedAt(LocalDateTime.now());
    nursingNoteRepository.save(note);

    auditService.log("NURSING_NOTE_CREATED", "NursingNote", note.getId(), "admissionId=" + admissionId);
    return note;
  }

  private String username(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) return null;
    return authentication.getName();
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String t = value.trim();
    return t.isBlank() ? null : t;
  }
}
