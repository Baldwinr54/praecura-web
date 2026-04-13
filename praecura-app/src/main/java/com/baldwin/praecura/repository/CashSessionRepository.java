package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.CashSession;
import com.baldwin.praecura.entity.CashSessionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashSessionRepository extends JpaRepository<CashSession, Long> {

  @Query("select c from CashSession c where c.status = 'OPEN' order by c.openedAt desc")
  Optional<CashSession> findLatestOpen();

  List<CashSession> findTop200ByOrderByOpenedAtDesc();

  @Query("select c from CashSession c where c.status = 'OPEN' and c.openedAt < :beforeDt order by c.openedAt asc")
  List<CashSession> findOpenBefore(@Param("beforeDt") LocalDateTime beforeDt);

  long countByStatus(CashSessionStatus status);
}
