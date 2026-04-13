package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.ReceivableCommitment;
import com.baldwin.praecura.entity.ReceivableCommitmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceivableCommitmentRepository extends JpaRepository<ReceivableCommitment, Long> {

  List<ReceivableCommitment> findByInvoiceIdOrderByPromisedDateAscCreatedAtAsc(Long invoiceId);

  List<ReceivableCommitment> findByInvoiceIdAndStatusOrderByPromisedDateAscCreatedAtAsc(Long invoiceId,
                                                                                          ReceivableCommitmentStatus status);

  long countByStatusAndPromisedDateBefore(ReceivableCommitmentStatus status, LocalDateTime promisedDate);

  @Query("select c from ReceivableCommitment c " +
      "where c.status = :status and c.promisedDate >= :fromDt and c.promisedDate <= :toDt " +
      "order by c.promisedDate asc")
  List<ReceivableCommitment> findPendingInWindow(@Param("status") ReceivableCommitmentStatus status,
                                                 @Param("fromDt") LocalDateTime fromDt,
                                                 @Param("toDt") LocalDateTime toDt,
                                                 Pageable pageable);
}
