package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.ApprovalStatus;
import com.baldwin.praecura.entity.CriticalActionApproval;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CriticalActionApprovalRepository extends JpaRepository<CriticalActionApproval, Long> {

  List<CriticalActionApproval> findTop100ByStatusOrderByRequestedAtDesc(ApprovalStatus status);

  List<CriticalActionApproval> findTop100ByOrderByRequestedAtDesc();

  Optional<CriticalActionApproval> findFirstByActionCodeAndEntityTypeAndEntityIdAndStatusOrderByRequestedAtDesc(
      String actionCode,
      String entityType,
      Long entityId,
      ApprovalStatus status
  );

  List<CriticalActionApproval> findByRequestedByUsernameOrderByRequestedAtDesc(String username);
}
