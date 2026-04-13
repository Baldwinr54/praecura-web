package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.Appointment;
import com.baldwin.praecura.entity.ClinicalEncounter;
import com.baldwin.praecura.entity.ClinicalEncounterStatus;
import com.baldwin.praecura.entity.ClinicalOrder;
import com.baldwin.praecura.entity.ClinicalOrderPriority;
import com.baldwin.praecura.entity.ClinicalOrderStatus;
import com.baldwin.praecura.entity.ClinicalOrderType;
import com.baldwin.praecura.entity.EncounterDiagnosis;
import com.baldwin.praecura.repository.AppointmentRepository;
import com.baldwin.praecura.repository.ClinicalEncounterRepository;
import com.baldwin.praecura.repository.ClinicalOrderRepository;
import com.baldwin.praecura.repository.EncounterDiagnosisRepository;
import com.baldwin.praecura.repository.PatientRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClinicalWorkflowService {

  private final ClinicalEncounterRepository encounterRepository;
  private final EncounterDiagnosisRepository diagnosisRepository;
  private final ClinicalOrderRepository orderRepository;
  private final PatientRepository patientRepository;
  private final AppointmentRepository appointmentRepository;
  private final AuditService auditService;

  public ClinicalWorkflowService(ClinicalEncounterRepository encounterRepository,
                                 EncounterDiagnosisRepository diagnosisRepository,
                                 ClinicalOrderRepository orderRepository,
                                 PatientRepository patientRepository,
                                 AppointmentRepository appointmentRepository,
                                 AuditService auditService) {
    this.encounterRepository = encounterRepository;
    this.diagnosisRepository = diagnosisRepository;
    this.orderRepository = orderRepository;
    this.patientRepository = patientRepository;
    this.appointmentRepository = appointmentRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<ClinicalEncounter> listEncounters(Long patientId) {
    if (patientId != null) {
      return encounterRepository.findByPatientIdOrderByEncounterAtDesc(patientId);
    }
    return encounterRepository.findAllByOrderByEncounterAtDesc();
  }

  @Transactional(readOnly = true)
  public List<EncounterDiagnosis> listDiagnoses(Long encounterId) {
    if (encounterId == null) return List.of();
    return diagnosisRepository.findByEncounterIdOrderByPrimaryDiagnosisDescCreatedAtAsc(encounterId);
  }

  @Transactional(readOnly = true)
  public List<ClinicalOrder> listOrders(Long encounterId) {
    if (encounterId == null) return List.of();
    return orderRepository.findByEncounterIdOrderByCreatedAtDesc(encounterId);
  }

  @Transactional
  public ClinicalEncounter createEncounter(Long patientId,
                                           Long appointmentId,
                                           String chiefComplaint,
                                           String subjective,
                                           String objective,
                                           String assessment,
                                           String plan,
                                           Authentication authentication) {
    if (patientId == null) throw new IllegalArgumentException("Debes seleccionar paciente.");

    ClinicalEncounter encounter = new ClinicalEncounter();
    encounter.setPatient(patientRepository.findById(patientId)
        .orElseThrow(() -> new IllegalArgumentException("Paciente no existe.")));

    if (appointmentId != null) {
      Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
      encounter.setAppointment(appointment);
      if (appointment != null) {
        encounter.setDoctor(appointment.getDoctor());
      }
    }

    encounter.setStatus(ClinicalEncounterStatus.OPEN);
    encounter.setEncounterAt(LocalDateTime.now());
    encounter.setChiefComplaint(trimToNull(chiefComplaint));
    encounter.setSubjective(trimToNull(subjective));
    encounter.setObjective(trimToNull(objective));
    encounter.setAssessment(trimToNull(assessment));
    encounter.setPlan(trimToNull(plan));
    encounter.setCreatedBy(username(authentication));
    encounter.setUpdatedBy(username(authentication));
    encounter.setCreatedAt(LocalDateTime.now());
    encounter.setUpdatedAt(LocalDateTime.now());
    encounterRepository.save(encounter);

    auditService.log("CLINICAL_ENCOUNTER_CREATED", "ClinicalEncounter", encounter.getId(),
        "patientId=" + patientId + ", appointmentId=" + appointmentId);
    return encounter;
  }

  @Transactional
  public ClinicalEncounter updateSoap(Long encounterId,
                                      String chiefComplaint,
                                      String subjective,
                                      String objective,
                                      String assessment,
                                      String plan,
                                      ClinicalEncounterStatus status,
                                      Authentication authentication) {
    ClinicalEncounter encounter = encounterRepository.findById(encounterId)
        .orElseThrow(() -> new IllegalArgumentException("Encuentro no existe."));

    encounter.setChiefComplaint(trimToNull(chiefComplaint));
    encounter.setSubjective(trimToNull(subjective));
    encounter.setObjective(trimToNull(objective));
    encounter.setAssessment(trimToNull(assessment));
    encounter.setPlan(trimToNull(plan));
    encounter.setStatus(status != null ? status : encounter.getStatus());
    encounter.setUpdatedBy(username(authentication));
    encounter.setUpdatedAt(LocalDateTime.now());
    encounterRepository.save(encounter);

    auditService.log("CLINICAL_SOAP_UPDATED", "ClinicalEncounter", encounterId,
        "status=" + encounter.getStatus());
    return encounter;
  }

  @Transactional
  public EncounterDiagnosis addDiagnosis(Long encounterId,
                                         String icd10Code,
                                         String description,
                                         boolean primaryDiagnosis,
                                         String notes) {
    if (encounterId == null) throw new IllegalArgumentException("Encuentro requerido.");
    String desc = trimToNull(description);
    if (desc == null) throw new IllegalArgumentException("Debes indicar descripción del diagnóstico.");

    ClinicalEncounter encounter = encounterRepository.findById(encounterId)
        .orElseThrow(() -> new IllegalArgumentException("Encuentro no existe."));

    if (primaryDiagnosis) {
      List<EncounterDiagnosis> existing = diagnosisRepository.findByEncounterIdOrderByPrimaryDiagnosisDescCreatedAtAsc(encounterId);
      for (EncounterDiagnosis d : existing) {
        if (d.isPrimaryDiagnosis()) {
          d.setPrimaryDiagnosis(false);
          diagnosisRepository.save(d);
        }
      }
    }

    EncounterDiagnosis diagnosis = new EncounterDiagnosis();
    diagnosis.setEncounter(encounter);
    diagnosis.setIcd10Code(trimToNull(icd10Code));
    diagnosis.setDescription(desc);
    diagnosis.setPrimaryDiagnosis(primaryDiagnosis);
    diagnosis.setNotes(trimToNull(notes));
    diagnosis.setCreatedAt(LocalDateTime.now());
    diagnosisRepository.save(diagnosis);

    auditService.log("CLINICAL_DIAGNOSIS_CREATED", "EncounterDiagnosis", diagnosis.getId(),
        "encounterId=" + encounterId + ", code=" + diagnosis.getIcd10Code());
    return diagnosis;
  }

  @Transactional
  public ClinicalOrder addOrder(Long encounterId,
                                ClinicalOrderType type,
                                ClinicalOrderPriority priority,
                                String orderName,
                                String instructions,
                                LocalDateTime dueAt,
                                BigDecimal costEstimate) {
    if (encounterId == null) throw new IllegalArgumentException("Encuentro requerido.");
    if (type == null) throw new IllegalArgumentException("Tipo de orden requerido.");
    String name = trimToNull(orderName);
    if (name == null) throw new IllegalArgumentException("Nombre de orden requerido.");

    ClinicalEncounter encounter = encounterRepository.findById(encounterId)
        .orElseThrow(() -> new IllegalArgumentException("Encuentro no existe."));

    ClinicalOrder order = new ClinicalOrder();
    order.setEncounter(encounter);
    order.setOrderType(type);
    order.setPriority(priority != null ? priority : ClinicalOrderPriority.ROUTINE);
    order.setStatus(ClinicalOrderStatus.ORDERED);
    order.setOrderName(name);
    order.setInstructions(trimToNull(instructions));
    order.setDueAt(dueAt);
    order.setCostEstimate(amount(costEstimate));
    order.setCreatedAt(LocalDateTime.now());
    orderRepository.save(order);

    auditService.log("CLINICAL_ORDER_CREATED", "ClinicalOrder", order.getId(),
        "encounterId=" + encounterId + ", type=" + type);
    return order;
  }

  @Transactional
  public void updateOrderStatus(Long orderId, ClinicalOrderStatus status, String resultSummary) {
    ClinicalOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new IllegalArgumentException("Orden clínica no existe."));

    if (status != null) {
      order.setStatus(status);
      if (status == ClinicalOrderStatus.COMPLETED) {
        order.setCompletedAt(LocalDateTime.now());
      }
    }
    if (resultSummary != null) {
      order.setResultSummary(trimToNull(resultSummary));
    }
    orderRepository.save(order);

    auditService.log("CLINICAL_ORDER_UPDATED", "ClinicalOrder", orderId,
        "status=" + order.getStatus());
  }

  private String username(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) return null;
    return authentication.getName();
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isBlank() ? null : trimmed;
  }

  private BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP).max(BigDecimal.ZERO);
  }
}
