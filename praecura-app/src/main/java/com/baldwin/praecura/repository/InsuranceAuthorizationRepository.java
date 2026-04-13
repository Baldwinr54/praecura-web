package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.InsuranceAuthorization;
import com.baldwin.praecura.entity.InsuranceAuthorizationStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceAuthorizationRepository extends JpaRepository<InsuranceAuthorization, Long> {

  List<InsuranceAuthorization> findAllByOrderByRequestedAtDesc();

  List<InsuranceAuthorization> findByCoveragePatientIdOrderByRequestedAtDesc(Long patientId);

  List<InsuranceAuthorization> findByStatusAndExpiresAtBefore(InsuranceAuthorizationStatus status, LocalDateTime dt);

  List<InsuranceAuthorization> findByStatusOrderByRequestedAtDesc(InsuranceAuthorizationStatus status, Pageable pageable);
}
