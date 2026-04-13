package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.InsuranceClaim;
import com.baldwin.praecura.entity.InsuranceClaimStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, Long> {

  Optional<InsuranceClaim> findByInvoiceId(Long invoiceId);

  List<InsuranceClaim> findAllByOrderByCreatedAtDesc();

  List<InsuranceClaim> findByStatusOrderByUpdatedAtDesc(InsuranceClaimStatus status);

  @Query("select c from InsuranceClaim c where c.createdAt >= :from and c.createdAt < :to order by c.createdAt desc")
  List<InsuranceClaim> findByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
