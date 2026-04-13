package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.ClinicalMedication;
import com.baldwin.praecura.entity.MedicationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalMedicationRepository extends JpaRepository<ClinicalMedication, Long> {
  List<ClinicalMedication> findByPatientIdOrderByCreatedAtDesc(Long patientId);
  long countByPatientIdAndStatus(Long patientId, MedicationStatus status);
}
