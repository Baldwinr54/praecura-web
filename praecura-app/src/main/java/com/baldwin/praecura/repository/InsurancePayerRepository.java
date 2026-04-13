package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.InsurancePayer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsurancePayerRepository extends JpaRepository<InsurancePayer, Long> {

  List<InsurancePayer> findAllByOrderByNameAsc();

  List<InsurancePayer> findByActiveTrueOrderByNameAsc();

  Optional<InsurancePayer> findByCodeIgnoreCase(String code);
}
