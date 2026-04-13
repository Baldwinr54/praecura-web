package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.PurchaseOrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

  List<PurchaseOrderItem> findByPurchaseOrderIdOrderByIdAsc(Long purchaseOrderId);
}
