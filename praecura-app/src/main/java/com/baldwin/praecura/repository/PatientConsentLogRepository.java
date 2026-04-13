package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.PatientConsentLog;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientConsentLogRepository extends JpaRepository<PatientConsentLog, Long> {
  List<PatientConsentLog> findByPatientIdOrderByCapturedAtDesc(Long patientId, Pageable pageable);
}
