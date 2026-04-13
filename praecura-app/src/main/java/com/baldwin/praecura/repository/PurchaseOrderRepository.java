package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.PurchaseOrder;
import com.baldwin.praecura.entity.PurchaseOrderStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

  List<PurchaseOrder> findByStatusOrderByOrderedAtDesc(PurchaseOrderStatus status);

  List<PurchaseOrder> findTop50ByOrderByOrderedAtDesc();
}
