package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.ClinicalOrder;
import com.baldwin.praecura.entity.ClinicalOrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalOrderRepository extends JpaRepository<ClinicalOrder, Long> {

  List<ClinicalOrder> findByEncounterIdOrderByCreatedAtDesc(Long encounterId);

  List<ClinicalOrder> findByStatusOrderByDueAtAsc(ClinicalOrderStatus status);

  List<ClinicalOrder> findByStatusAndDueAtBeforeOrderByDueAtAsc(ClinicalOrderStatus status, LocalDateTime dt);
}
