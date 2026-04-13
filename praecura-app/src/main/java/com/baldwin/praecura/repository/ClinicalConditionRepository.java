package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.ClinicalCondition;
import com.baldwin.praecura.entity.ConditionStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalConditionRepository extends JpaRepository<ClinicalCondition, Long> {
  List<ClinicalCondition> findByPatientIdOrderByCreatedAtDesc(Long patientId);
  long countByPatientIdAndStatus(Long patientId, ConditionStatus status);
}
