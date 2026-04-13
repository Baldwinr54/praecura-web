package com.baldwin.praecura.controller;

import com.baldwin.praecura.service.AccessControlService;
import com.baldwin.praecura.service.CriticalActionApprovalService;
import com.baldwin.praecura.repository.UserRepository;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/access")
public class AccessControlController {

  private final AccessControlService accessControlService;
  private final CriticalActionApprovalService criticalActionApprovalService;
  private final UserRepository userRepository;

  public AccessControlController(AccessControlService accessControlService,
                                 CriticalActionApprovalService criticalActionApprovalService,
                                 UserRepository userRepository) {
    this.accessControlService = accessControlService;
    this.criticalActionApprovalService = criticalActionApprovalService;
    this.userRepository = userRepository;
  }

  @GetMapping
  public String index(@RequestParam(required = false) Long userId,
                      Model model) {
    var userPermissions = accessControlService.listUserPermissions(userId);
    Map<String, Boolean> grantedByCode = new HashMap<>();
    for (var up : userPermissions) {
      if (up.getPermission() != null && up.getPermission().getCode() != null) {
        grantedByCode.put(up.getPermission().getCode(), up.isGranted());
      }
    }

    model.addAttribute("users", userRepository.findAll());
    model.addAttribute("permissions", accessControlService.listPermissions());
    model.addAttribute("permissionMatrix", accessControlService.permissionMatrix());
    model.addAttribute("selectedUserId", userId);
    model.addAttribute("userPermissions", userPermissions);
    model.addAttribute("grantedByCode", grantedByCode);
    model.addAttribute("pendingApprovals", criticalActionApprovalService.listPending());
    model.addAttribute("approvalHistory", criticalActionApprovalService.latest());
    return "admin/access/index";
  }

  @PostMapping("/permissions")
  public String setPermission(@RequestParam Long userId,
                              @RequestParam String permissionCode,
                              @RequestParam boolean granted,
                              RedirectAttributes ra,
                              @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      accessControlService.setPermission(userId, permissionCode, granted);
      ra.addFlashAttribute("success", granted ? "Permiso otorgado." : "Permiso revocado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/admin/access?userId=" + userId);
  }

  @PostMapping("/approvals/{id}/approve")
  public String approve(@PathVariable Long id,
                        @RequestParam(required = false) String notes,
                        Authentication authentication,
                        RedirectAttributes ra,
                        @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      criticalActionApprovalService.approve(id, true, notes, authentication);
      ra.addFlashAttribute("success", "Solicitud crítica aprobada.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/admin/access");
  }

  @PostMapping("/approvals/{id}/reject")
  public String reject(@PathVariable Long id,
                       @RequestParam(required = false) String notes,
                       Authentication authentication,
                       RedirectAttributes ra,
                       @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      criticalActionApprovalService.approve(id, false, notes, authentication);
      ra.addFlashAttribute("warning", "Solicitud crítica rechazada.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/admin/access");
  }

  private String redirectBack(String referer, String fallback) {
    if (referer == null || referer.isBlank()) return "redirect:" + fallback;
    if (referer.startsWith("/") && !referer.startsWith("//")) return "redirect:" + referer;
    return "redirect:" + fallback;
  }
}
