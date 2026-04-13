package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.ClinicalEncounter;
import com.baldwin.praecura.entity.DispensationStatus;
import com.baldwin.praecura.entity.InventoryItem;
import com.baldwin.praecura.entity.PatientAdmission;
import com.baldwin.praecura.entity.PharmacyDispensation;
import com.baldwin.praecura.entity.PharmacyDispensationItem;
import com.baldwin.praecura.entity.PurchaseOrder;
import com.baldwin.praecura.entity.PurchaseOrderItem;
import com.baldwin.praecura.entity.PurchaseOrderStatus;
import com.baldwin.praecura.entity.StockMovement;
import com.baldwin.praecura.entity.StockMovementType;
import com.baldwin.praecura.repository.ClinicalEncounterRepository;
import com.baldwin.praecura.repository.InventoryItemRepository;
import com.baldwin.praecura.repository.PatientAdmissionRepository;
import com.baldwin.praecura.repository.PatientRepository;
import com.baldwin.praecura.repository.PharmacyDispensationItemRepository;
import com.baldwin.praecura.repository.PharmacyDispensationRepository;
import com.baldwin.praecura.repository.PurchaseOrderItemRepository;
import com.baldwin.praecura.repository.PurchaseOrderRepository;
import com.baldwin.praecura.repository.StockMovementRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PharmacyService {

  private final InventoryItemRepository inventoryItemRepository;
  private final StockMovementRepository stockMovementRepository;
  private final PurchaseOrderRepository purchaseOrderRepository;
  private final PurchaseOrderItemRepository purchaseOrderItemRepository;
  private final PharmacyDispensationRepository dispensationRepository;
  private final PharmacyDispensationItemRepository dispensationItemRepository;
  private final PatientRepository patientRepository;
  private final PatientAdmissionRepository admissionRepository;
  private final ClinicalEncounterRepository encounterRepository;
  private final AuditService auditService;

  public PharmacyService(InventoryItemRepository inventoryItemRepository,
                         StockMovementRepository stockMovementRepository,
                         PurchaseOrderRepository purchaseOrderRepository,
                         PurchaseOrderItemRepository purchaseOrderItemRepository,
                         PharmacyDispensationRepository dispensationRepository,
                         PharmacyDispensationItemRepository dispensationItemRepository,
                         PatientRepository patientRepository,
                         PatientAdmissionRepository admissionRepository,
                         ClinicalEncounterRepository encounterRepository,
                         AuditService auditService) {
    this.inventoryItemRepository = inventoryItemRepository;
    this.stockMovementRepository = stockMovementRepository;
    this.purchaseOrderRepository = purchaseOrderRepository;
    this.purchaseOrderItemRepository = purchaseOrderItemRepository;
    this.dispensationRepository = dispensationRepository;
    this.dispensationItemRepository = dispensationItemRepository;
    this.patientRepository = patientRepository;
    this.admissionRepository = admissionRepository;
    this.encounterRepository = encounterRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<InventoryItem> listItems() {
    return inventoryItemRepository.findAllByOrderByNameAsc();
  }

  @Transactional(readOnly = true)
  public List<PurchaseOrder> listPurchaseOrders() {
    return purchaseOrderRepository.findTop50ByOrderByOrderedAtDesc();
  }

  @Transactional(readOnly = true)
  public List<PharmacyDispensation> listDispensations() {
    return dispensationRepository.findTop50ByOrderByCreatedAtDesc();
  }

  @Transactional(readOnly = true)
  public List<StockMovement> latestMovements() {
    return stockMovementRepository.findTop100ByOrderByCreatedAtDesc();
  }

  @Transactional
  public InventoryItem createItem(String sku,
                                  String name,
                                  String category,
                                  String presentation,
                                  String unit,
                                  BigDecimal costPrice,
                                  BigDecimal salePrice,
                                  BigDecimal taxRate,
                                  BigDecimal minStock) {
    String itemName = trimToNull(name);
    if (itemName == null) {
      throw new IllegalArgumentException("Nombre de ítem es obligatorio.");
    }

    InventoryItem item = new InventoryItem();
    item.setSku(trimToNull(sku));
    item.setName(itemName);
    item.setCategory(trimToNull(category));
    item.setPresentation(trimToNull(presentation));
    item.setUnit(trimToNull(unit) != null ? unit.trim().toUpperCase() : "UNIDAD");
    item.setCostPrice(amount(costPrice));
    item.setSalePrice(amount(salePrice));
    item.setTaxRate(rate(taxRate));
    item.setMinStock(amount(minStock));
    item.setCreatedAt(LocalDateTime.now());
    item.setUpdatedAt(LocalDateTime.now());
    inventoryItemRepository.save(item);

    auditService.log("INVENTORY_ITEM_CREATED", "InventoryItem", item.getId(), "sku=" + item.getSku());
    return item;
  }

  @Transactional
  public StockMovement recordMovement(Long itemId,
                                      StockMovementType movementType,
                                      BigDecimal quantity,
                                      BigDecimal unitCost,
                                      String referenceType,
                                      Long referenceId,
                                      String lotNumber,
                                      LocalDate expiresAt,
                                      String notes,
                                      Authentication authentication) {
    if (itemId == null || movementType == null) {
      throw new IllegalArgumentException("Ítem y tipo de movimiento son obligatorios.");
    }
    BigDecimal qty = amount(quantity);
    if (qty.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cantidad debe ser mayor a 0.");
    }

    StockMovement movement = new StockMovement();
    movement.setItem(inventoryItemRepository.findById(itemId)
        .orElseThrow(() -> new IllegalArgumentException("Ítem no existe.")));
    movement.setMovementType(movementType);
    movement.setQuantity(qty);
    movement.setUnitCost(amount(unitCost));
    movement.setReferenceType(trimToNull(referenceType));
    movement.setReferenceId(referenceId);
    movement.setLotNumber(trimToNull(lotNumber));
    movement.setExpiresAt(expiresAt);
    movement.setNotes(trimToNull(notes));
    movement.setCreatedBy(username(authentication));
    movement.setCreatedAt(LocalDateTime.now());
    stockMovementRepository.save(movement);

    auditService.log("STOCK_MOVEMENT_CREATED", "StockMovement", movement.getId(),
        "itemId=" + itemId + ", type=" + movementType + ", qty=" + qty);
    return movement;
  }

  @Transactional
  public PurchaseOrder createPurchaseOrder(String supplierName,
                                           String notes,
                                           Authentication authentication) {
    String supplier = trimToNull(supplierName);
    if (supplier == null) {
      throw new IllegalArgumentException("Debes indicar suplidor.");
    }

    PurchaseOrder po = new PurchaseOrder();
    po.setSupplierName(supplier);
    po.setStatus(PurchaseOrderStatus.DRAFT);
    po.setOrderedAt(LocalDateTime.now());
    po.setNotes(trimToNull(notes));
    po.setCreatedBy(username(authentication));
    po.setUpdatedBy(username(authentication));
    po.setCreatedAt(LocalDateTime.now());
    po.setUpdatedAt(LocalDateTime.now());
    purchaseOrderRepository.save(po);

    auditService.log("PURCHASE_ORDER_CREATED", "PurchaseOrder", po.getId(), "supplier=" + supplier);
    return po;
  }

  @Transactional
  public PurchaseOrderItem addPurchaseOrderItem(Long purchaseOrderId,
                                                Long itemId,
                                                BigDecimal quantity,
                                                BigDecimal unitCost,
                                                BigDecimal taxRate) {
    if (purchaseOrderId == null || itemId == null) {
      throw new IllegalArgumentException("Orden de compra e ítem son obligatorios.");
    }

    PurchaseOrder po = purchaseOrderRepository.findById(purchaseOrderId)
        .orElseThrow(() -> new IllegalArgumentException("Orden no existe."));
    InventoryItem item = inventoryItemRepository.findById(itemId)
        .orElseThrow(() -> new IllegalArgumentException("Ítem no existe."));

    BigDecimal qty = amount(quantity);
    if (qty.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cantidad debe ser mayor a 0.");
    }

    BigDecimal price = amount(unitCost);
    BigDecimal rate = rate(taxRate);

    PurchaseOrderItem poi = new PurchaseOrderItem();
    poi.setPurchaseOrder(po);
    poi.setItem(item);
    poi.setQuantity(qty);
    poi.setUnitCost(price);
    poi.setTaxRate(rate);
    BigDecimal subtotal = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
    BigDecimal tax = subtotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    poi.setTotal(subtotal.add(tax));
    purchaseOrderItemRepository.save(poi);

    recalcPurchaseOrder(po.getId());
    auditService.log("PURCHASE_ORDER_ITEM_CREATED", "PurchaseOrderItem", poi.getId(), "purchaseOrderId=" + purchaseOrderId);
    return poi;
  }

  @Transactional
  public void receivePurchaseOrder(Long purchaseOrderId,
                                   Authentication authentication) {
    PurchaseOrder po = purchaseOrderRepository.findById(purchaseOrderId)
        .orElseThrow(() -> new IllegalArgumentException("Orden no existe."));

    List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderIdOrderByIdAsc(purchaseOrderId);
    if (items.isEmpty()) {
      throw new IllegalArgumentException("No puedes recibir una orden sin renglones.");
    }

    for (PurchaseOrderItem item : items) {
      item.setReceivedQuantity(item.getQuantity());
      purchaseOrderItemRepository.save(item);

      recordMovement(
          item.getItem().getId(),
          StockMovementType.PURCHASE_IN,
          item.getQuantity(),
          item.getUnitCost(),
          "PURCHASE_ORDER",
          purchaseOrderId,
          null,
          null,
          "Recepción OC #" + purchaseOrderId,
          authentication
      );
    }

    po.setStatus(PurchaseOrderStatus.RECEIVED);
    po.setReceivedAt(LocalDateTime.now());
    po.setUpdatedAt(LocalDateTime.now());
    po.setUpdatedBy(username(authentication));
    purchaseOrderRepository.save(po);

    auditService.log("PURCHASE_ORDER_RECEIVED", "PurchaseOrder", purchaseOrderId, "lines=" + items.size());
  }

  @Transactional
  public PharmacyDispensation createDispensation(Long patientId,
                                                 Long admissionId,
                                                 Long encounterId,
                                                 String notes,
                                                 Authentication authentication) {
    if (patientId == null) throw new IllegalArgumentException("Paciente requerido.");

    PharmacyDispensation disp = new PharmacyDispensation();
    disp.setPatient(patientRepository.findById(patientId)
        .orElseThrow(() -> new IllegalArgumentException("Paciente no existe.")));

    if (admissionId != null) {
      PatientAdmission admission = admissionRepository.findById(admissionId).orElse(null);
      disp.setAdmission(admission);
    }
    if (encounterId != null) {
      ClinicalEncounter encounter = encounterRepository.findById(encounterId).orElse(null);
      disp.setEncounter(encounter);
    }

    disp.setStatus(DispensationStatus.PENDING);
    disp.setNotes(trimToNull(notes));
    disp.setCreatedBy(username(authentication));
    disp.setCreatedAt(LocalDateTime.now());
    disp.setUpdatedAt(LocalDateTime.now());
    dispensationRepository.save(disp);

    auditService.log("PHARMACY_DISPENSATION_CREATED", "PharmacyDispensation", disp.getId(), "patientId=" + patientId);
    return disp;
  }

  @Transactional
  public PharmacyDispensationItem addDispensationItem(Long dispensationId,
                                                      Long itemId,
                                                      BigDecimal quantity,
                                                      BigDecimal unitPrice,
                                                      String notes) {
    if (dispensationId == null || itemId == null) {
      throw new IllegalArgumentException("Dispensación e ítem son obligatorios.");
    }
    BigDecimal qty = amount(quantity);
    if (qty.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cantidad debe ser mayor a 0.");
    }

    PharmacyDispensation disp = dispensationRepository.findById(dispensationId)
        .orElseThrow(() -> new IllegalArgumentException("Dispensación no existe."));

    InventoryItem item = inventoryItemRepository.findById(itemId)
        .orElseThrow(() -> new IllegalArgumentException("Ítem no existe."));

    PharmacyDispensationItem line = new PharmacyDispensationItem();
    line.setDispensation(disp);
    line.setItem(item);
    line.setQuantity(qty);
    line.setUnitPrice(amount(unitPrice));
    line.setTotal(line.getUnitPrice().multiply(qty).setScale(2, RoundingMode.HALF_UP));
    line.setNotes(trimToNull(notes));
    dispensationItemRepository.save(line);

    recalcDispensation(disp.getId());
    auditService.log("PHARMACY_DISPENSATION_ITEM_CREATED", "PharmacyDispensationItem", line.getId(), "dispensationId=" + dispensationId);
    return line;
  }

  @Transactional
  public void completeDispensation(Long dispensationId, Authentication authentication) {
    PharmacyDispensation disp = dispensationRepository.findById(dispensationId)
        .orElseThrow(() -> new IllegalArgumentException("Dispensación no existe."));

    List<PharmacyDispensationItem> lines = dispensationItemRepository.findByDispensationIdOrderByIdAsc(dispensationId);
    if (lines.isEmpty()) {
      throw new IllegalArgumentException("No puedes completar una dispensación sin renglones.");
    }

    for (PharmacyDispensationItem line : lines) {
      recordMovement(
          line.getItem().getId(),
          StockMovementType.DISPENSE_OUT,
          line.getQuantity(),
          line.getUnitPrice(),
          "PHARMACY_DISPENSATION",
          dispensationId,
          null,
          null,
          "Dispensación #" + dispensationId,
          authentication
      );
    }

    disp.setStatus(DispensationStatus.DISPENSED);
    disp.setDispensedAt(LocalDateTime.now());
    disp.setUpdatedAt(LocalDateTime.now());
    dispensationRepository.save(disp);

    auditService.log("PHARMACY_DISPENSATION_COMPLETED", "PharmacyDispensation", dispensationId, "lines=" + lines.size());
  }

  @Transactional(readOnly = true)
  public BigDecimal currentStock(Long itemId) {
    if (itemId == null) return BigDecimal.ZERO;
    BigDecimal value = stockMovementRepository.currentStock(itemId);
    return value != null ? value : BigDecimal.ZERO;
  }

  @Transactional(readOnly = true)
  public List<InventoryItem> lowStockItems() {
    List<InventoryItem> items = inventoryItemRepository.findByActiveTrueOrderByNameAsc();
    return items.stream()
        .filter(i -> currentStock(i.getId()).compareTo(i.getMinStock()) <= 0)
        .toList();
  }

  private void recalcPurchaseOrder(Long purchaseOrderId) {
    PurchaseOrder po = purchaseOrderRepository.findById(purchaseOrderId)
        .orElseThrow(() -> new IllegalArgumentException("Orden no existe."));
    List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderIdOrderByIdAsc(purchaseOrderId);

    BigDecimal subtotal = BigDecimal.ZERO;
    BigDecimal tax = BigDecimal.ZERO;
    for (PurchaseOrderItem i : items) {
      BigDecimal lineSubtotal = i.getQuantity().multiply(i.getUnitCost()).setScale(2, RoundingMode.HALF_UP);
      BigDecimal lineTax = lineSubtotal.multiply(i.getTaxRate()).setScale(2, RoundingMode.HALF_UP);
      subtotal = subtotal.add(lineSubtotal);
      tax = tax.add(lineTax);
    }

    po.setSubtotal(subtotal);
    po.setTax(tax);
    po.setTotal(subtotal.add(tax));
    po.setUpdatedAt(LocalDateTime.now());
    purchaseOrderRepository.save(po);
  }

  private void recalcDispensation(Long dispensationId) {
    PharmacyDispensation disp = dispensationRepository.findById(dispensationId)
        .orElseThrow(() -> new IllegalArgumentException("Dispensación no existe."));

    BigDecimal total = dispensationItemRepository.findByDispensationIdOrderByIdAsc(dispensationId).stream()
        .map(PharmacyDispensationItem::getTotal)
        .filter(java.util.Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    disp.setTotal(total.setScale(2, RoundingMode.HALF_UP));
    disp.setUpdatedAt(LocalDateTime.now());
    dispensationRepository.save(disp);
  }

  private String username(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) return null;
    return authentication.getName();
  }

  private BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP).max(BigDecimal.ZERO);
  }

  private BigDecimal rate(BigDecimal value) {
    BigDecimal r = value == null ? BigDecimal.ZERO : value.setScale(4, RoundingMode.HALF_UP);
    if (r.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
    return r;
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String t = value.trim();
    return t.isBlank() ? null : t;
  }
}
