package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.AdmissionStatus;
import com.baldwin.praecura.entity.PatientAdmission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientAdmissionRepository extends JpaRepository<PatientAdmission, Long> {

  List<PatientAdmission> findAllByOrderByAdmittedAtDesc();

  List<PatientAdmission> findByStatusOrderByAdmittedAtDesc(AdmissionStatus status);

  List<PatientAdmission> findByPatientIdOrderByAdmittedAtDesc(Long patientId);

  long countByStatusIn(List<AdmissionStatus> statuses);
}
