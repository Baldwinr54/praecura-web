package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.EncounterDiagnosis;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EncounterDiagnosisRepository extends JpaRepository<EncounterDiagnosis, Long> {

  List<EncounterDiagnosis> findByEncounterIdOrderByPrimaryDiagnosisDescCreatedAtAsc(Long encounterId);
}
