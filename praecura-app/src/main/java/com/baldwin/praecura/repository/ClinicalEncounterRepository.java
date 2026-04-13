package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.ClinicalEncounter;
import com.baldwin.praecura.entity.ClinicalEncounterStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalEncounterRepository extends JpaRepository<ClinicalEncounter, Long> {

  List<ClinicalEncounter> findAllByOrderByEncounterAtDesc();

  List<ClinicalEncounter> findByPatientIdOrderByEncounterAtDesc(Long patientId);

  List<ClinicalEncounter> findByStatusOrderByEncounterAtDesc(ClinicalEncounterStatus status);

  Optional<ClinicalEncounter> findByAppointmentId(Long appointmentId);
}
