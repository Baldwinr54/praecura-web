package com.baldwin.praecura.controller;

import com.baldwin.praecura.entity.StockMovementType;
import com.baldwin.praecura.service.PharmacyService;
import com.baldwin.praecura.repository.PatientAdmissionRepository;
import com.baldwin.praecura.repository.PatientRepository;
import com.baldwin.praecura.repository.ClinicalEncounterRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/pharmacy")
public class PharmacyController {

  private final PharmacyService pharmacyService;
  private final PatientRepository patientRepository;
  private final PatientAdmissionRepository admissionRepository;
  private final ClinicalEncounterRepository encounterRepository;

  public PharmacyController(PharmacyService pharmacyService,
                            PatientRepository patientRepository,
                            PatientAdmissionRepository admissionRepository,
                            ClinicalEncounterRepository encounterRepository) {
    this.pharmacyService = pharmacyService;
    this.patientRepository = patientRepository;
    this.admissionRepository = admissionRepository;
    this.encounterRepository = encounterRepository;
  }

  @GetMapping
  public String index(Model model) {
    var items = pharmacyService.listItems();
    model.addAttribute("items", items);
    model.addAttribute("movements", pharmacyService.latestMovements());
    model.addAttribute("purchaseOrders", pharmacyService.listPurchaseOrders());
    model.addAttribute("dispensations", pharmacyService.listDispensations());
    model.addAttribute("lowStockItems", pharmacyService.lowStockItems());
    model.addAttribute("movementTypes", StockMovementType.values());

    java.util.Map<Long, BigDecimal> stockByItemId = new java.util.HashMap<>();
    for (var item : items) {
      stockByItemId.put(item.getId(), pharmacyService.currentStock(item.getId()));
    }
    model.addAttribute("stockByItemId", stockByItemId);

    model.addAttribute("patients", patientRepository.findAll());
    model.addAttribute("admissions", admissionRepository.findAll());
    model.addAttribute("encounters", encounterRepository.findAll());
    return "pharmacy/index";
  }

  @PostMapping("/items")
  public String createItem(@RequestParam(required = false) String sku,
                           @RequestParam String name,
                           @RequestParam(required = false) String category,
                           @RequestParam(required = false) String presentation,
                           @RequestParam(required = false) String unit,
                           @RequestParam(required = false) BigDecimal costPrice,
                           @RequestParam(required = false) BigDecimal salePrice,
                           @RequestParam(required = false) BigDecimal taxRate,
                           @RequestParam(required = false) BigDecimal minStock,
                           RedirectAttributes ra,
                           @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      pharmacyService.createItem(sku, name, category, presentation, unit, costPrice, salePrice, taxRate, minStock);
      ra.addFlashAttribute("success", "Ítem de inventario creado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/pharmacy");
  }

  @PostMapping("/movements")
  public String createMovement(@RequestParam Long itemId,
                               @RequestParam StockMovementType movementType,
                               @RequestParam BigDecimal quantity,
                               @RequestParam(required = false) BigDecimal unitCost,
                               @RequestParam(required = false) String referenceType,
                               @RequestParam(required = false) Long referenceId,
                               @RequestParam(required = false) String lotNumber,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiresAt,
                               @RequestParam(required = false) String notes,
                               Authentication authentication,
                               RedirectAttributes ra,
                               @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      pharmacyService.recordMovement(itemId, movementType, quantity, unitCost, referenceType, referenceId, lotNumber,
          expiresAt, notes, authentication);
      ra.addFlashAttribute("success", "Movimiento de stock registrado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/pharmacy");
  }

  @PostMapping("/purchase-orders")
  public String createPurchaseOrder(@RequestParam String supplierName,
                                    @RequestParam(required = false) String notes,
                                    Authentication authentication,
                                    RedirectAttributes ra,
                                    @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      var po = pharmacyService.createPurchaseOrder(supplierName, notes, authentication);
      ra.addFlashAttribute("success", "Orden de compra creada (#" + po.getId() + ").");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/pharmacy");
  }

  @PostMapping("/purchase-orders/{id}/items")
  public String addPurchaseOrderItem(@PathVariable Long id,
                                     @RequestParam Long itemId,
                                     @RequestParam BigDecimal quantity,
                                     @RequestParam BigDecimal unitCost,
                                     @RequestParam(required = false) BigDecimal taxRate,
                                     RedirectAttributes ra,
                                     @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      pharmacyService.addPurchaseOrderItem(id, itemId, quantity, unitCost, taxRate);
      ra.addFlashAttribute("success", "Renglón agregado a orden de compra.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/pharmacy");
  }

  @PostMapping("/purchase-orders/{id}/receive")
  public String receivePurchaseOrder(@PathVariable Long id,
                                     Authentication authentication,
                                     RedirectAttributes ra,
                                     @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      pharmacyService.receivePurchaseOrder(id, authentication);
      ra.addFlashAttribute("success", "Orden de compra recibida e inventario actualizado.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/pharmacy");
  }

  @PostMapping("/dispensations")
  public String createDispensation(@RequestParam Long patientId,
                                   @RequestParam(required = false) Long admissionId,
                                   @RequestParam(required = false) Long encounterId,
                                   @RequestParam(required = false) String notes,
                                   Authentication authentication,
                                   RedirectAttributes ra,
                                   @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      var disp = pharmacyService.createDispensation(patientId, admissionId, encounterId, notes, authentication);
      ra.addFlashAttribute("success", "Dispensación creada (#" + disp.getId() + ").");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/pharmacy");
  }

  @PostMapping("/dispensations/{id}/items")
  public String addDispensationItem(@PathVariable Long id,
                                    @RequestParam Long itemId,
                                    @RequestParam BigDecimal quantity,
                                    @RequestParam(required = false) BigDecimal unitPrice,
                                    @RequestParam(required = false) String notes,
                                    RedirectAttributes ra,
                                    @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      pharmacyService.addDispensationItem(id, itemId, quantity, unitPrice, notes);
      ra.addFlashAttribute("success", "Renglón agregado a dispensación.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/pharmacy");
  }

  @PostMapping("/dispensations/{id}/complete")
  public String completeDispensation(@PathVariable Long id,
                                     Authentication authentication,
                                     RedirectAttributes ra,
                                     @RequestHeader(value = "Referer", required = false) String referer) {
    try {
      pharmacyService.completeDispensation(id, authentication);
      ra.addFlashAttribute("success", "Dispensación completada.");
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
    }
    return redirectBack(referer, "/pharmacy");
  }

  private String redirectBack(String referer, String fallback) {
    if (referer == null || referer.isBlank()) return "redirect:" + fallback;
    if (referer.startsWith("/") && !referer.startsWith("//")) return "redirect:" + referer;
    return "redirect:" + fallback;
  }
}
