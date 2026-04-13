package com.baldwin.praecura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "critical_action_approvals")
@Getter
@Setter
public class CriticalActionApproval {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "action_code", nullable = false, length = 80)
  private String actionCode;

  @Column(name = "entity_type", nullable = false, length = 80)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private Long entityId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ApprovalStatus status = ApprovalStatus.PENDING;

  @Column(length = 500)
  private String reason;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "requested_by_user_id", nullable = false)
  private AppUser requestedBy;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "approved_by_user_id")
  private AppUser approvedBy;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "used_by_user_id")
  private AppUser usedBy;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt = LocalDateTime.now();

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @Column(name = "used_at")
  private LocalDateTime usedAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();
}
