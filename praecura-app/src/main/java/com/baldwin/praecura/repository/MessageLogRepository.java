package com.baldwin.praecura.repository;

import com.baldwin.praecura.entity.MessageChannel;
import com.baldwin.praecura.entity.MessageLog;
import com.baldwin.praecura.entity.MessageStatus;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {

  @Query("select m from MessageLog m where " +
      "(:channel is null or m.channel = :channel) and " +
      "(:status is null or m.status = :status) and " +
      "m.createdAt >= :fromDt and m.createdAt < :toDt " +
      "order by m.createdAt desc")
  Page<MessageLog> search(@Param("channel") MessageChannel channel,
                          @Param("status") MessageStatus status,
                          @Param("fromDt") LocalDateTime fromDt,
                          @Param("toDt") LocalDateTime toDt,
                          Pageable pageable);
}
