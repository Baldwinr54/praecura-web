package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.AllergyStatus;
import com.baldwin.praecura.entity.ClinicalAllergy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalAllergyRepository extends JpaRepository<ClinicalAllergy, Long> {
  List<ClinicalAllergy> findByPatientIdOrderByCreatedAtDesc(Long patientId);
  long countByPatientIdAndStatus(Long patientId, AllergyStatus status);
}
