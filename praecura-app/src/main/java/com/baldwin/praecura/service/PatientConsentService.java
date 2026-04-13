package com.baldwin.praecura.service;

import com.baldwin.praecura.config.RequestContext;
import com.baldwin.praecura.config.RequestContextHolder;
import com.baldwin.praecura.entity.ConsentType;
import com.baldwin.praecura.entity.Patient;
import com.baldwin.praecura.entity.PatientConsentLog;
import com.baldwin.praecura.repository.PatientConsentLogRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PatientConsentService {

  private final PatientConsentLogRepository patientConsentLogRepository;

  public PatientConsentService(PatientConsentLogRepository patientConsentLogRepository) {
    this.patientConsentLogRepository = patientConsentLogRepository;
  }

  public void record(Patient patient, ConsentType type, boolean granted, String source, String notes) {
    if (patient == null || patient.getId() == null) return;

    PatientConsentLog log = new PatientConsentLog();
    log.setPatient(patient);
    log.setConsentType(type);
    log.setGranted(granted);
    log.setSource(source);
    log.setNotes(notes);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
      log.setCapturedBy(auth.getName());
    } else {
      log.setCapturedBy("system");
    }

    RequestContext ctx = RequestContextHolder.get();
    if (ctx != null) {
      log.setIpAddress(ctx.getIpAddress());
      log.setUserAgent(ctx.getUserAgent());
    }

    patientConsentLogRepository.save(log);
  }

  public List<PatientConsentLog> recentForPatient(Long patientId, int limit) {
    return patientConsentLogRepository.findByPatientIdOrderByCapturedAtDesc(
        patientId, PageRequest.of(0, Math.min(Math.max(limit, 1), 50)));
  }
}
