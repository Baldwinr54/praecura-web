package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.InsuranceAuthorization;
import com.baldwin.praecura.entity.InsuranceAuthorizationStatus;
import com.baldwin.praecura.entity.InsuranceClaim;
import com.baldwin.praecura.entity.InsuranceClaimStatus;
import com.baldwin.praecura.entity.InsurancePayer;
import com.baldwin.praecura.entity.InsurancePlan;
import com.baldwin.praecura.entity.PatientInsuranceCoverage;
import com.baldwin.praecura.repository.InsuranceAuthorizationRepository;
import com.baldwin.praecura.repository.InsuranceClaimRepository;
import com.baldwin.praecura.repository.InsurancePayerRepository;
import com.baldwin.praecura.repository.InsurancePlanRepository;
import com.baldwin.praecura.repository.InvoiceRepository;
import com.baldwin.praecura.repository.PatientInsuranceCoverageRepository;
import com.baldwin.praecura.repository.PatientRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsuranceService {

  private final InsurancePayerRepository payerRepository;
  private final InsurancePlanRepository planRepository;
  private final PatientInsuranceCoverageRepository coverageRepository;
  private final InsuranceAuthorizationRepository authorizationRepository;
  private final InsuranceClaimRepository claimRepository;
  private final PatientRepository patientRepository;
  private final InvoiceRepository invoiceRepository;
  private final AuditService auditService;

  public InsuranceService(InsurancePayerRepository payerRepository,
                          InsurancePlanRepository planRepository,
                          PatientInsuranceCoverageRepository coverageRepository,
                          InsuranceAuthorizationRepository authorizationRepository,
                          InsuranceClaimRepository claimRepository,
                          PatientRepository patientRepository,
                          InvoiceRepository invoiceRepository,
                          AuditService auditService) {
    this.payerRepository = payerRepository;
    this.planRepository = planRepository;
    this.coverageRepository = coverageRepository;
    this.authorizationRepository = authorizationRepository;
    this.claimRepository = claimRepository;
    this.patientRepository = patientRepository;
    this.invoiceRepository = invoiceRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<InsurancePayer> listPayers() {
    return payerRepository.findAllByOrderByNameAsc();
  }

  @Transactional(readOnly = true)
  public List<InsurancePlan> listPlans() {
    return planRepository.findAllByOrderByNameAsc();
  }

  @Transactional(readOnly = true)
  public List<PatientInsuranceCoverage> listCoverages() {
    return coverageRepository.findAllByOrderByCreatedAtDesc();
  }

  @Transactional(readOnly = true)
  public List<InsuranceAuthorization> listAuthorizations() {
    return authorizationRepository.findAllByOrderByRequestedAtDesc();
  }

  @Transactional(readOnly = true)
  public List<InsuranceClaim> listClaims() {
    return claimRepository.findAllByOrderByCreatedAtDesc();
  }

  @Transactional
  public InsurancePayer createPayer(String code, String name, String rnc, String phone, String email, String notes) {
    String normalizedCode = trimToNull(code);
    String normalizedName = trimToNull(name);
    if (normalizedCode == null || normalizedName == null) {
      throw new IllegalArgumentException("Código y nombre de ARS son obligatorios.");
    }
    if (payerRepository.findByCodeIgnoreCase(normalizedCode).isPresent()) {
      throw new IllegalArgumentException("Ya existe una ARS con ese código.");
    }

    InsurancePayer payer = new InsurancePayer();
    payer.setCode(normalizedCode.toUpperCase());
    payer.setName(normalizedName);
    payer.setRnc(trimToNull(rnc));
    payer.setContactPhone(trimToNull(phone));
    payer.setContactEmail(trimToNull(email));
    payer.setNotes(trimToNull(notes));
    payer.setCreatedAt(LocalDateTime.now());
    payer.setUpdatedAt(LocalDateTime.now());
    payerRepository.save(payer);

    auditService.log("INSURANCE_PAYER_CREATED", "InsurancePayer", payer.getId(), "code=" + payer.getCode());
    return payer;
  }

  @Transactional
  public InsurancePlan createPlan(Long payerId,
                                  String planCode,
                                  String name,
                                  BigDecimal coveragePercent,
                                  BigDecimal copayPercent,
                                  BigDecimal deductible,
                                  boolean requiresAuthorization) {
    if (payerId == null) throw new IllegalArgumentException("Debes seleccionar ARS.");
    String normalizedPlanCode = trimToNull(planCode);
    String normalizedName = trimToNull(name);
    if (normalizedPlanCode == null || normalizedName == null) {
      throw new IllegalArgumentException("Código y nombre del plan son obligatorios.");
    }

    InsurancePayer payer = payerRepository.findById(payerId)
        .orElseThrow(() -> new IllegalArgumentException("ARS no existe."));

    if (planRepository.findByPayerIdAndPlanCodeIgnoreCase(payerId, normalizedPlanCode).isPresent()) {
      throw new IllegalArgumentException("Ya existe un plan con ese código para la ARS seleccionada.");
    }

    InsurancePlan plan = new InsurancePlan();
    plan.setPayer(payer);
    plan.setPlanCode(normalizedPlanCode.toUpperCase());
    plan.setName(normalizedName);
    plan.setCoveragePercent(percent(coveragePercent));
    plan.setCopayPercent(percent(copayPercent));
    plan.setDeductibleAmount(amount(deductible));
    plan.setRequiresAuthorization(requiresAuthorization);
    plan.setCreatedAt(LocalDateTime.now());
    plan.setUpdatedAt(LocalDateTime.now());
    planRepository.save(plan);

    auditService.log("INSURANCE_PLAN_CREATED", "InsurancePlan", plan.getId(), "payerId=" + payerId + ", code=" + plan.getPlanCode());
    return plan;
  }

  @Transactional
  public PatientInsuranceCoverage createCoverage(Long patientId,
                                                 Long planId,
                                                 String policyNumber,
                                                 String affiliateNumber,
                                                 java.time.LocalDate validFrom,
                                                 java.time.LocalDate validTo) {
    if (patientId == null || planId == null) {
      throw new IllegalArgumentException("Paciente y plan son obligatorios.");
    }

    PatientInsuranceCoverage coverage = new PatientInsuranceCoverage();
    coverage.setPatient(patientRepository.findById(patientId)
        .orElseThrow(() -> new IllegalArgumentException("Paciente no existe.")));
    coverage.setPlan(planRepository.findById(planId)
        .orElseThrow(() -> new IllegalArgumentException("Plan no existe.")));
    coverage.setPolicyNumber(trimToNull(policyNumber));
    coverage.setAffiliateNumber(trimToNull(affiliateNumber));
    coverage.setValidFrom(validFrom);
    coverage.setValidTo(validTo);
    coverage.setActive(true);
    coverage.setCreatedAt(LocalDateTime.now());
    coverage.setUpdatedAt(LocalDateTime.now());
    coverageRepository.save(coverage);

    auditService.log("INSURANCE_COVERAGE_CREATED", "PatientInsuranceCoverage", coverage.getId(),
        "patientId=" + patientId + ", planId=" + planId);
    return coverage;
  }

  @Transactional
  public InsuranceAuthorization createAuthorization(Long coverageId,
                                                    Long appointmentId,
                                                    String authorizationNumber,
                                                    BigDecimal requestedAmount,
                                                    BigDecimal approvedAmount,
                                                    InsuranceAuthorizationStatus status,
                                                    LocalDateTime expiresAt,
                                                    String notes) {
    if (coverageId == null) throw new IllegalArgumentException("Debes seleccionar cobertura.");

    InsuranceAuthorization auth = new InsuranceAuthorization();
    auth.setCoverage(coverageRepository.findById(coverageId)
        .orElseThrow(() -> new IllegalArgumentException("Cobertura no existe.")));
    if (appointmentId != null) {
      auth.setAppointment(
          invoiceRepository.findByAppointmentId(appointmentId)
              .map(com.baldwin.praecura.entity.Invoice::getAppointment)
              .orElse(null)
      );
    }
    auth.setAuthorizationNumber(trimToNull(authorizationNumber));
    auth.setRequestedAmount(amount(requestedAmount));
    auth.setApprovedAmount(amount(approvedAmount));
    auth.setStatus(status != null ? status : InsuranceAuthorizationStatus.REQUESTED);
    auth.setExpiresAt(expiresAt);
    auth.setNotes(trimToNull(notes));
    auth.setRequestedAt(LocalDateTime.now());
    auth.setUpdatedAt(LocalDateTime.now());
    authorizationRepository.save(auth);

    auditService.log("INSURANCE_AUTH_CREATED", "InsuranceAuthorization", auth.getId(),
        "coverageId=" + coverageId + ", status=" + auth.getStatus());
    return auth;
  }

  @Transactional
  public InsuranceClaim createOrUpdateClaim(Long invoiceId,
                                            Long coverageId,
                                            Long authorizationId,
                                            InsuranceClaimStatus status,
                                            BigDecimal claimedAmount,
                                            BigDecimal approvedAmount,
                                            BigDecimal deniedAmount,
                                            String denialReason,
                                            String notes) {
    if (invoiceId == null) {
      throw new IllegalArgumentException("Debes indicar la factura del reclamo.");
    }

    InsuranceClaim claim = claimRepository.findByInvoiceId(invoiceId).orElseGet(InsuranceClaim::new);
    claim.setInvoice(invoiceRepository.findById(invoiceId)
        .orElseThrow(() -> new IllegalArgumentException("Factura no existe.")));

    claim.setCoverage(coverageId != null ? coverageRepository.findById(coverageId).orElse(null) : null);
    claim.setAuthorization(authorizationId != null ? authorizationRepository.findById(authorizationId).orElse(null) : null);
    claim.setStatus(status != null ? status : InsuranceClaimStatus.DRAFT);
    claim.setClaimedAmount(amount(claimedAmount));
    claim.setApprovedAmount(amount(approvedAmount));
    claim.setDeniedAmount(amount(deniedAmount));
    claim.setDenialReason(trimToNull(denialReason));
    claim.setNotes(trimToNull(notes));
    claim.setUpdatedAt(LocalDateTime.now());

    if (claim.getId() == null) {
      claim.setCreatedAt(LocalDateTime.now());
    }
    if (claim.getStatus() == InsuranceClaimStatus.SUBMITTED && claim.getSubmittedAt() == null) {
      claim.setSubmittedAt(LocalDateTime.now());
    }
    if ((claim.getStatus() == InsuranceClaimStatus.APPROVED
        || claim.getStatus() == InsuranceClaimStatus.PARTIALLY_APPROVED
        || claim.getStatus() == InsuranceClaimStatus.DENIED
        || claim.getStatus() == InsuranceClaimStatus.CLOSED
        || claim.getStatus() == InsuranceClaimStatus.PAID)
        && claim.getResolvedAt() == null) {
      claim.setResolvedAt(LocalDateTime.now());
    }

    claimRepository.save(claim);

    auditService.log("INSURANCE_CLAIM_UPSERT", "InsuranceClaim", claim.getId(),
        "invoiceId=" + invoiceId + ", status=" + claim.getStatus());
    return claim;
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isBlank() ? null : trimmed;
  }

  private BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP).max(BigDecimal.ZERO);
  }

  private BigDecimal percent(BigDecimal value) {
    BigDecimal p = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    if (p.compareTo(BigDecimal.ZERO) < 0) p = BigDecimal.ZERO;
    if (p.compareTo(new BigDecimal("100")) > 0) p = new BigDecimal("100");
    return p;
  }
}
