package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.FiscalSequence;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface FiscalSequenceRepository extends JpaRepository<FiscalSequence, Long> {

  Optional<FiscalSequence> findByTypeCodeIgnoreCase(String typeCode);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select f from FiscalSequence f where lower(f.typeCode) = lower(:typeCode) and f.active = true")
  Optional<FiscalSequence> findActiveForUpdate(@Param("typeCode") String typeCode);

  List<FiscalSequence> findByActiveTrueOrderByTypeCodeAsc();
}
