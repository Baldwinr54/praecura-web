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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "invoices")
@Getter
@Setter
public class Invoice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "appointment_id")
  private Appointment appointment;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private InvoiceStatus status = InvoiceStatus.ISSUED;

  @Enumerated(EnumType.STRING)
  @Column(name = "invoice_type", nullable = false, length = 20)
  private InvoiceType invoiceType = InvoiceType.INVOICE;

  @Column(nullable = false, length = 3)
  private String currency = "DOP";

  @Column(name = "fiscal_name", length = 160)
  private String fiscalName;

  @Column(name = "fiscal_tax_id", length = 20)
  private String fiscalTaxId;

  @Column(name = "fiscal_address", length = 200)
  private String fiscalAddress;

  @Column(name = "ncf_type", length = 10)
  private String ncfType;

  @Column(name = "ncf", length = 20)
  private String ncf;

  @Enumerated(EnumType.STRING)
  @Column(name = "ncf_status", nullable = false, length = 20)
  private NcfStatus ncfStatus = NcfStatus.PENDING;

  @Column(name = "ncf_issued_at")
  private LocalDateTime ncfIssuedAt;

  @Column(precision = 12, scale = 2, nullable = false)
  private BigDecimal subtotal = BigDecimal.ZERO;

  @Column(precision = 12, scale = 2, nullable = false)
  private BigDecimal tax = BigDecimal.ZERO;

  @Column(precision = 12, scale = 2, nullable = false)
  private BigDecimal discount = BigDecimal.ZERO;

  @Column(precision = 12, scale = 2, nullable = false)
  private BigDecimal total = BigDecimal.ZERO;

  @Column(precision = 12, scale = 2, nullable = false)
  private BigDecimal balance = BigDecimal.ZERO;

  @Column(length = 500)
  private String notes;

  @Column(name = "created_by", length = 120)
  private String createdBy;

  @Column(name = "updated_by", length = 120)
  private String updatedBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  @Column(name = "issued_at")
  private LocalDateTime issuedAt;

  @Column(name = "paid_at")
  private LocalDateTime paidAt;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "credit_note_of")
  private Invoice creditNoteOf;

  @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
  private List<InvoiceItem> items = new ArrayList<>();

  @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
  private List<Payment> payments = new ArrayList<>();
}
