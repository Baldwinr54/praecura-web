package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.NursingNote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NursingNoteRepository extends JpaRepository<NursingNote, Long> {

  List<NursingNote> findByAdmissionIdOrderByRecordedAtDesc(Long admissionId);
}
