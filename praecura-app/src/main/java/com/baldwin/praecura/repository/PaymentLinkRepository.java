package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.PaymentLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLinkRepository extends JpaRepository<PaymentLink, Long> {
  List<PaymentLink> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

  Optional<PaymentLink> findBySessionId(String sessionId);
}
