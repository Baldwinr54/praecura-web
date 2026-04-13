package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.ClinicalVital;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalVitalRepository extends JpaRepository<ClinicalVital, Long> {
  List<ClinicalVital> findByPatientIdOrderByRecordedAtDesc(Long patientId);
}
