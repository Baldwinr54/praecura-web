package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.DispensationStatus;
import com.baldwin.praecura.entity.PharmacyDispensation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyDispensationRepository extends JpaRepository<PharmacyDispensation, Long> {

  List<PharmacyDispensation> findByStatusOrderByCreatedAtDesc(DispensationStatus status);

  List<PharmacyDispensation> findTop50ByOrderByCreatedAtDesc();
}
