package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.EcfStatus;
import com.baldwin.praecura.entity.ElectronicFiscalDocument;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ElectronicFiscalDocumentRepository extends JpaRepository<ElectronicFiscalDocument, Long> {

  Optional<ElectronicFiscalDocument> findByInvoiceId(Long invoiceId);

  Page<ElectronicFiscalDocument> findByStatusOrderByCreatedAtDesc(EcfStatus status, Pageable pageable);

  Page<ElectronicFiscalDocument> findAllByOrderByCreatedAtDesc(Pageable pageable);

  List<ElectronicFiscalDocument> findTop100ByOrderByCreatedAtDesc();

  List<ElectronicFiscalDocument> findTop200ByStatusInOrderByUpdatedAtAsc(List<EcfStatus> statuses);

  @Query("select e from ElectronicFiscalDocument e " +
      "where e.status in :statuses and (e.nextRetryAt is null or e.nextRetryAt <= :now) " +
      "order by e.updatedAt asc")
  List<ElectronicFiscalDocument> findRetryable(@Param("statuses") List<EcfStatus> statuses,
                                               @Param("now") LocalDateTime now,
                                               Pageable pageable);
}
