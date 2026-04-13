package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.InsurancePlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsurancePlanRepository extends JpaRepository<InsurancePlan, Long> {

  List<InsurancePlan> findAllByOrderByNameAsc();

  List<InsurancePlan> findByPayerIdOrderByNameAsc(Long payerId);

  List<InsurancePlan> findByActiveTrueOrderByNameAsc();

  Optional<InsurancePlan> findByPayerIdAndPlanCodeIgnoreCase(Long payerId, String planCode);
}
