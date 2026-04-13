package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.AppUser;
import com.baldwin.praecura.entity.ApprovalStatus;
import com.baldwin.praecura.entity.CriticalActionApproval;
import com.baldwin.praecura.entity.SystemPermission;
import com.baldwin.praecura.repository.CriticalActionApprovalRepository;
import com.baldwin.praecura.repository.SystemPermissionRepository;
import com.baldwin.praecura.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CriticalActionApprovalService {

  private final CriticalActionApprovalRepository approvalRepository;
  private final SystemPermissionRepository systemPermissionRepository;
  private final UserRepository userRepository;
  private final AuditService auditService;

  @Value("${praecura.security.dual-approval-enabled:true}")
  private boolean dualApprovalEnabled;

  public CriticalActionApprovalService(CriticalActionApprovalRepository approvalRepository,
                                       SystemPermissionRepository systemPermissionRepository,
                                       UserRepository userRepository,
                                       AuditService auditService) {
    this.approvalRepository = approvalRepository;
    this.systemPermissionRepository = systemPermissionRepository;
    this.userRepository = userRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<CriticalActionApproval> listPending() {
    return approvalRepository.findTop100ByStatusOrderByRequestedAtDesc(ApprovalStatus.PENDING);
  }

  @Transactional(readOnly = true)
  public List<CriticalActionApproval> latest() {
    return approvalRepository.findTop100ByOrderByRequestedAtDesc();
  }

  @Transactional
  public void ensureApprovedOrRequest(String actionCode,
                                      String entityType,
                                      Long entityId,
                                      String reason,
                                      Authentication authentication) {
    if (!dualApprovalEnabled) return;
    if (actionCode == null || actionCode.isBlank() || entityType == null || entityType.isBlank() || entityId == null) {
      return;
    }

    SystemPermission permission = systemPermissionRepository.findByCode(actionCode)
        .orElse(null);
    if (permission == null || !permission.isCritical()) {
      return;
    }

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalArgumentException("No autorizado para ejecutar acción crítica.");
    }

    AppUser actor = userRepository.findByUsername(authentication.getName())
        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado para aprobación crítica."));

    var approvedOpt = approvalRepository.findFirstByActionCodeAndEntityTypeAndEntityIdAndStatusOrderByRequestedAtDesc(
        actionCode, entityType, entityId, ApprovalStatus.APPROVED);
    if (approvedOpt.isPresent()) {
      CriticalActionApproval approved = approvedOpt.get();
      if (approved.getUsedAt() != null || approved.getStatus() == ApprovalStatus.USED) {
        throw new IllegalArgumentException("Esta aprobación crítica ya fue consumida; solicita una nueva.");
      }
      if (approved.getApprovedBy() != null && approved.getApprovedBy().getId().equals(actor.getId())) {
        throw new IllegalArgumentException("La persona que aprueba no puede ejecutar la acción crítica. Debe ejecutarla otro usuario.");
      }
      approved.setStatus(ApprovalStatus.USED);
      approved.setUsedBy(actor);
      approved.setUsedAt(LocalDateTime.now());
      approved.setUpdatedAt(LocalDateTime.now());
      approvalRepository.save(approved);
      auditService.log("CRITICAL_APPROVAL_CONSUMED", "CriticalActionApproval", approved.getId(),
          "action=" + actionCode + ", entity=" + entityType + ", entityId=" + entityId);
      return;
    }

    var pendingOpt = approvalRepository.findFirstByActionCodeAndEntityTypeAndEntityIdAndStatusOrderByRequestedAtDesc(
        actionCode, entityType, entityId, ApprovalStatus.PENDING);

    if (pendingOpt.isPresent()) {
      CriticalActionApproval pending = pendingOpt.get();
      if (pending.getRequestedBy() != null && pending.getRequestedBy().getId().equals(actor.getId())) {
        throw new IllegalArgumentException("La acción crítica está pendiente de segunda aprobación.");
      }
      throw new IllegalArgumentException("La acción crítica ya tiene una solicitud pendiente de aprobación.");
    }

    CriticalActionApproval request = new CriticalActionApproval();
    request.setActionCode(actionCode);
    request.setEntityType(entityType);
    request.setEntityId(entityId);
    request.setStatus(ApprovalStatus.PENDING);
    request.setReason(reason);
    request.setRequestedBy(actor);
    request.setRequestedAt(LocalDateTime.now());
    request.setUpdatedAt(LocalDateTime.now());
    approvalRepository.save(request);

    auditService.log("CRITICAL_APPROVAL_REQUESTED", "CriticalActionApproval", request.getId(),
        "action=" + actionCode + ", entity=" + entityType + ", entityId=" + entityId);

    throw new IllegalArgumentException("Acción crítica enviada a aprobación. Otro administrador debe aprobarla antes de ejecutar.");
  }

  @Transactional
  public void approve(Long id, boolean approved, String notes, Authentication authentication) {
    if (id == null) {
      throw new IllegalArgumentException("Solicitud de aprobación inválida.");
    }
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalArgumentException("No autorizado para aprobar.");
    }

    CriticalActionApproval request = approvalRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Solicitud no existe."));

    if (request.getStatus() != ApprovalStatus.PENDING) {
      throw new IllegalArgumentException("Solo puedes gestionar solicitudes pendientes.");
    }

    AppUser actor = userRepository.findByUsername(authentication.getName())
        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

    if (request.getRequestedBy() != null && request.getRequestedBy().getId().equals(actor.getId())) {
      throw new IllegalArgumentException("No puedes aprobar tu propia solicitud crítica.");
    }

    request.setStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
    request.setApprovedBy(actor);
    request.setApprovedAt(LocalDateTime.now());
    request.setUpdatedAt(LocalDateTime.now());
    if (notes != null && !notes.isBlank()) {
      request.setReason((request.getReason() != null ? request.getReason() + " | " : "") + notes.trim());
    }
    approvalRepository.save(request);

    auditService.log(approved ? "CRITICAL_APPROVAL_APPROVED" : "CRITICAL_APPROVAL_REJECTED",
        "CriticalActionApproval",
        request.getId(),
        "action=" + request.getActionCode() + ", entity=" + request.getEntityType() + ", entityId=" + request.getEntityId());
  }
}
