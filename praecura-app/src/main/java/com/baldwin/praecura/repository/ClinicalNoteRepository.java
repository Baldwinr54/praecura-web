package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.ClinicalNote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, Long> {
  List<ClinicalNote> findByPatientIdOrderByRecordedAtDesc(Long patientId);
  long countByPatientId(Long patientId);
}
