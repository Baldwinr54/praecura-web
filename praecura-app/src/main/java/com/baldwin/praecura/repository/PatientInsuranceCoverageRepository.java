package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.PatientInsuranceCoverage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientInsuranceCoverageRepository extends JpaRepository<PatientInsuranceCoverage, Long> {

  List<PatientInsuranceCoverage> findAllByOrderByCreatedAtDesc();

  List<PatientInsuranceCoverage> findByPatientIdOrderByCreatedAtDesc(Long patientId);

  List<PatientInsuranceCoverage> findByPatientIdAndActiveTrueOrderByCreatedAtDesc(Long patientId);
}
