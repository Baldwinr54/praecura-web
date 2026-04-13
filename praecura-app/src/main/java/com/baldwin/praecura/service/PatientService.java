package com.baldwin.praecura.service;

import com.baldwin.praecura.dto.PatientForm;
import com.baldwin.praecura.entity.Patient;
import com.baldwin.praecura.repository.PatientRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientService {

  private final PatientRepository patientRepository;
  private final AuditService auditService;
  private final PatientConsentService patientConsentService;

  public PatientService(PatientRepository patientRepository,
                        AuditService auditService,
                        PatientConsentService patientConsentService) {
    this.patientRepository = patientRepository;
    this.auditService = auditService;
    this.patientConsentService = patientConsentService;
  }

  public Page<Patient> search(String q, Pageable pageable) {
    if (q == null || q.trim().isEmpty()) {
      return patientRepository.findActive(pageable);
    }
    return patientRepository.searchActive(q.trim(), pageable);
  }

  public Page<Patient> searchAdvanced(String q,
                                     String name,
                                     String cedula,
                                     String phone,
                                     String email,
                                     Boolean missingPhone,
                                     Boolean missingEmail,
                                     boolean includeInactive,
                                     Pageable pageable) {
    return patientRepository.searchAdvanced(
        trimToNull(q),
        trimToNull(name),
        normalizeRdCedula(cedula),
        normalizeRdPhone(phone),
        normalizeEmail(email),
        missingPhone,
        missingEmail,
        includeInactive,
        pageable
    );
  }

  public Patient findById(Long id) {
    return patientRepository.findById(id).orElseThrow();
  }

  @Cacheable(value = "selectPatients", sync = true)
  public List<Patient> listActiveForSelect() {
    return patientRepository.findActiveForSelect();
  }

  @CacheEvict(value = {"selectPatients"}, allEntries = true)
  public Patient saveOrUpdate(PatientForm form) {
    boolean isNew = (form.getId() == null);
    Patient p = isNew ? new Patient() : findById(form.getId());

    boolean wasConsentSms = isNew ? false : p.isConsentSms();
    boolean wasConsentEmail = isNew ? false : p.isConsentEmail();
    boolean wasConsentWhatsapp = isNew ? false : p.isConsentWhatsapp();

    String fullName = (safe(form.getFirstName()) + " " + safe(form.getLastName())).trim();
    p.setFullName(fullName);

    p.setPhone(normalizeRdPhone(form.getPhone()));
    p.setCedula(normalizeRdCedula(form.getCedula()));
    p.setEmail(normalizeEmail(form.getEmail()));
    p.setConsentSms(form.isConsentSms());
    p.setConsentEmail(form.isConsentEmail());
    p.setConsentWhatsapp(form.isConsentWhatsapp());
    p.setPreferredChannel(form.getPreferredChannel());
    p.setFlags(trimToNull(form.getFlags()));
    p.setNotes(trimToNull(form.getNotes()));
    p.setBillingName(trimToNull(form.getBillingName()));
    p.setBillingTaxId(trimToNull(form.getBillingTaxId()));
    p.setBillingAddress(trimToNull(form.getBillingAddress()));

    // Validación de contacto mínimo
    boolean hasPhone = p.getPhone() != null && !p.getPhone().isBlank();
    boolean hasEmail = p.getEmail() != null && !p.getEmail().isBlank();
    if (!hasPhone && !hasEmail) {
      throw new IllegalArgumentException("El paciente debe tener al menos teléfono o correo.");
    }

    if (isNew) {
      p.setActive(true);
    }

    // Dedupe (activos): cédula y email
    if (p.getCedula() != null && patientRepository.existsCedulaOther(p.getCedula(), p.getId())) {
      throw new IllegalArgumentException("Ya existe un paciente activo con esa cédula.");
    }
    if (p.getEmail() != null && patientRepository.existsEmailOther(p.getEmail(), p.getId())) {
      throw new IllegalArgumentException("Ya existe un paciente activo con ese correo.");
    }

    Patient saved = patientRepository.save(p);
    auditService.log(isNew ? "CREATE" : "UPDATE", "Patient", saved.getId(), saved.getFullName());

    if (wasConsentSms != saved.isConsentSms()) {
      patientConsentService.record(saved, com.baldwin.praecura.entity.ConsentType.SMS, saved.isConsentSms(),
          "UI", "Cambio de consentimiento SMS");
    }
    if (wasConsentEmail != saved.isConsentEmail()) {
      patientConsentService.record(saved, com.baldwin.praecura.entity.ConsentType.EMAIL, saved.isConsentEmail(),
          "UI", "Cambio de consentimiento Email");
    }
    if (wasConsentWhatsapp != saved.isConsentWhatsapp()) {
      patientConsentService.record(saved, com.baldwin.praecura.entity.ConsentType.WHATSAPP, saved.isConsentWhatsapp(),
          "UI", "Cambio de consentimiento WhatsApp");
    }
    return saved;
  }

  @CacheEvict(value = {"selectPatients"}, allEntries = true)
  public void deactivate(Long id) {
    Patient p = findById(id);
    p.setActive(false);
    patientRepository.save(p);
    auditService.log("DEACTIVATE", "Patient", id, p.getFullName());
  }

  private String safe(String s) {
    return s == null ? "" : s;
  }

  private String trimToNull(String v) {
    if (v == null) return null;
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }

  private String normalizeEmail(String v) {
    String t = trimToNull(v);
    if (t == null) return null;
    return t.toLowerCase();
  }

  private String normalizeRdPhone(String v) {
    String t = trimToNull(v);
    if (t == null) return null;

    String digits = t.replaceAll("\\D", "");
    if (digits.isEmpty()) return null;

    if (digits.length() == 10) {
      return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6, 10);
    }
    return t;
  }

  private String normalizeRdCedula(String v) {
    String t = trimToNull(v);
    if (t == null) return null;

    String digits = t.replaceAll("\\D", "");
    if (digits.isEmpty()) return null;

    if (digits.length() == 11) {
      return digits.substring(0, 3) + "-" + digits.substring(3, 10) + "-" + digits.substring(10, 11);
    }
    return t;
  }
}
