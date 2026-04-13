package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.StockMovement;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

  List<StockMovement> findTop100ByOrderByCreatedAtDesc();

  List<StockMovement> findByItemIdOrderByCreatedAtDesc(Long itemId);

  @Query("select coalesce(sum(case when m.movementType in (com.baldwin.praecura.entity.StockMovementType.PURCHASE_IN, com.baldwin.praecura.entity.StockMovementType.ADJUSTMENT_IN, com.baldwin.praecura.entity.StockMovementType.RETURN_IN, com.baldwin.praecura.entity.StockMovementType.TRANSFER_IN) then m.quantity else -m.quantity end), 0) from StockMovement m where m.item.id = :itemId")
  java.math.BigDecimal currentStock(Long itemId);

  List<StockMovement> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
}
