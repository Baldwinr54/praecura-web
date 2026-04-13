package com.baldwin.praecura.service;

import com.baldwin.praecura.entity.CashSession;
import com.baldwin.praecura.entity.CashSessionStatus;
import com.baldwin.praecura.entity.Payment;
import com.baldwin.praecura.entity.PaymentChannel;
import com.baldwin.praecura.entity.PaymentMethod;
import com.baldwin.praecura.entity.PaymentStatus;
import com.baldwin.praecura.repository.CashSessionRepository;
import com.baldwin.praecura.repository.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashSessionService {

  private final CashSessionRepository cashSessionRepository;
  private final PaymentRepository paymentRepository;

  public CashSessionService(CashSessionRepository cashSessionRepository,
                            PaymentRepository paymentRepository) {
    this.cashSessionRepository = cashSessionRepository;
    this.paymentRepository = paymentRepository;
  }

  public Optional<CashSession> findActive() {
    return cashSessionRepository.findLatestOpen();
  }

  public CashSession getSession(Long id) {
    return cashSessionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Caja no encontrada."));
  }

  @Transactional
  public CashSession openSession(BigDecimal openingAmount, String notes) {
    if (openingAmount == null || openingAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("El monto de apertura debe ser mayor o igual a 0.");
    }
    if (cashSessionRepository.countByStatus(CashSessionStatus.OPEN) > 0) {
      throw new IllegalArgumentException("Ya existe una caja abierta.");
    }

    CashSession session = new CashSession();
    session.setOpeningAmount(openingAmount);
    session.setOpenedAt(LocalDateTime.now());
    session.setOpenedBy(currentUsername());
    session.setNotes(trimToNull(notes));
    session.setStatus(CashSessionStatus.OPEN);
    return cashSessionRepository.save(session);
  }

  @Transactional
  public CashSession closeSession(Long sessionId, BigDecimal closingAmount, String notes) {
    CashSession session = cashSessionRepository.findById(sessionId)
        .orElseThrow(() -> new IllegalArgumentException("Caja no encontrada."));

    if (session.getStatus() == CashSessionStatus.CLOSED) {
      return session;
    }

    if (closingAmount == null || closingAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("El monto de cierre debe ser mayor o igual a 0.");
    }

    CashSessionSummary summary = buildSummary(session);
    BigDecimal counted = scale(closingAmount);
    BigDecimal difference = counted.subtract(scale(summary.expected()));
    if (difference.compareTo(BigDecimal.ZERO) != 0 && trimToNull(notes) == null) {
      throw new IllegalArgumentException("Debes registrar una nota cuando existe diferencia de caja.");
    }

    session.setClosingAmount(counted);
    session.setClosedAt(LocalDateTime.now());
    session.setClosedBy(currentUsername());
    session.setNotes(mergeNotes(session.getNotes(), notes));
    session.setStatus(CashSessionStatus.CLOSED);
    return cashSessionRepository.save(session);
  }

  public List<CashSessionSnapshot> listRecentSnapshots(int limit) {
    int take = limit <= 0 ? 10 : Math.min(limit, 200);
    List<CashSession> sessions = cashSessionRepository.findTop200ByOrderByOpenedAtDesc();
    List<CashSessionSnapshot> out = new ArrayList<>();
    for (CashSession s : sessions) {
      if (out.size() >= take) break;
      CashSessionSummary summary = buildSummary(s);
      out.add(new CashSessionSnapshot(
          s.getId(),
          s.getStatus(),
          s.getOpenedAt(),
          s.getClosedAt(),
          s.getOpenedBy(),
          s.getClosedBy(),
          summary.openingAmount(),
          summary.cashIn(),
          summary.cashOut(),
          summary.expected(),
          s.getClosingAmount(),
          summary.difference(),
          s.getNotes()
      ));
    }
    return out;
  }

  public CashSessionSummary buildSummary(CashSession session) {
    if (session == null) {
      return new CashSessionSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
    BigDecimal cashIn = paymentRepository.sumByCashSession(session.getId(), PaymentMethod.CASH, PaymentStatus.CAPTURED);
    BigDecimal cashOut = paymentRepository.sumRefundedBySession(session.getId(), PaymentMethod.CASH);
    BigDecimal opening = session.getOpeningAmount() == null ? BigDecimal.ZERO : session.getOpeningAmount();
    BigDecimal expected = scale(opening.add(cashIn).subtract(cashOut));
    BigDecimal diff = (session.getClosingAmount() == null)
        ? BigDecimal.ZERO
        : scale(session.getClosingAmount().subtract(expected));
    return new CashSessionSummary(opening, cashIn, cashOut, expected, diff);
  }

  public CashSessionReconciliation buildReconciliation(CashSession session) {
    if (session == null) {
      LocalDateTime now = LocalDateTime.now();
      return new CashSessionReconciliation(now, now, List.of(), List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    LocalDateTime from = session.getOpenedAt() != null ? session.getOpenedAt() : LocalDateTime.now();
    LocalDateTime to = session.getClosedAt() != null ? session.getClosedAt() : LocalDateTime.now();
    if (to.isBefore(from)) {
      to = from;
    }
    LocalDateTime exclusiveTo = to.plusNanos(1);

    List<Payment> capturedPayments = paymentRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(from, exclusiveTo);
    List<Payment> refundPayments = paymentRepository.findByRefundedAtGreaterThanEqualAndRefundedAtLessThanOrderByRefundedAtAsc(from, exclusiveTo);

    Map<PaymentMethod, MutableMethodTotals> methodTotals = new EnumMap<>(PaymentMethod.class);
    Map<PaymentChannel, MutableChannelTotals> channelTotals = new EnumMap<>(PaymentChannel.class);
    for (PaymentMethod method : PaymentMethod.values()) {
      methodTotals.put(method, new MutableMethodTotals());
    }
    for (PaymentChannel channel : PaymentChannel.values()) {
      channelTotals.put(channel, new MutableChannelTotals());
    }

    BigDecimal totalCaptured = BigDecimal.ZERO;
    for (Payment payment : capturedPayments) {
      if (payment.getStatus() != PaymentStatus.CAPTURED && payment.getStatus() != PaymentStatus.REFUNDED) continue;
      PaymentMethod method = payment.getMethod() != null ? payment.getMethod() : PaymentMethod.OTHER;
      PaymentChannel channel = payment.getChannel() != null ? payment.getChannel() : PaymentChannel.OTHER;
      BigDecimal amount = scale(payment.getAmount());
      methodTotals.get(method).capturedCount++;
      methodTotals.get(method).capturedAmount = methodTotals.get(method).capturedAmount.add(amount);
      channelTotals.get(channel).count++;
      channelTotals.get(channel).amount = channelTotals.get(channel).amount.add(amount);
      totalCaptured = totalCaptured.add(amount);
    }

    BigDecimal totalRefunded = BigDecimal.ZERO;
    for (Payment payment : refundPayments) {
      BigDecimal refunded = scale(payment.getRefundedAmount());
      if (refunded.compareTo(BigDecimal.ZERO) <= 0) continue;
      PaymentMethod refundMethod = payment.getRefundMethod() != null
          ? payment.getRefundMethod()
          : (payment.getMethod() != null ? payment.getMethod() : PaymentMethod.OTHER);
      methodTotals.get(refundMethod).refundCount++;
      methodTotals.get(refundMethod).refundedAmount = methodTotals.get(refundMethod).refundedAmount.add(refunded);
      totalRefunded = totalRefunded.add(refunded);
    }

    List<CashMethodSummary> methods = new ArrayList<>();
    for (PaymentMethod method : PaymentMethod.values()) {
      MutableMethodTotals totals = methodTotals.get(method);
      BigDecimal captured = scale(totals.capturedAmount);
      BigDecimal refunded = scale(totals.refundedAmount);
      if (totals.capturedCount == 0 && totals.refundCount == 0
          && captured.compareTo(BigDecimal.ZERO) == 0 && refunded.compareTo(BigDecimal.ZERO) == 0) {
        continue;
      }
      methods.add(new CashMethodSummary(
          method,
          totals.capturedCount,
          captured,
          totals.refundCount,
          refunded,
          scale(captured.subtract(refunded))
      ));
    }

    List<CashChannelSummary> channels = new ArrayList<>();
    for (PaymentChannel channel : PaymentChannel.values()) {
      MutableChannelTotals totals = channelTotals.get(channel);
      BigDecimal amount = scale(totals.amount);
      if (totals.count == 0 && amount.compareTo(BigDecimal.ZERO) == 0) continue;
      channels.add(new CashChannelSummary(channel, totals.count, amount));
    }

    return new CashSessionReconciliation(
        from,
        to,
        methods,
        channels,
        scale(totalCaptured),
        scale(totalRefunded),
        scale(totalCaptured.subtract(totalRefunded))
    );
  }

  private BigDecimal scale(BigDecimal value) {
    if (value == null) return BigDecimal.ZERO;
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String mergeNotes(String openingNotes, String closingNotes) {
    String open = trimToNull(openingNotes);
    String close = trimToNull(closingNotes);
    if (open == null && close == null) return null;
    if (open == null) return close;
    if (close == null) return open;
    String merged = open + "\n[Cierre] " + close;
    if (merged.length() > 500) {
      return merged.substring(0, 500);
    }
    return merged;
  }

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()) {
      return auth.getName();
    }
    return "system";
  }

  public record CashSessionSummary(BigDecimal openingAmount,
                                   BigDecimal cashIn,
                                   BigDecimal cashOut,
                                   BigDecimal expected,
                                   BigDecimal difference) {
    public CashSessionSummary(BigDecimal openingAmount,
                              BigDecimal cashIn,
                              BigDecimal cashOut,
                              BigDecimal expected) {
      this(openingAmount, cashIn, cashOut, expected, BigDecimal.ZERO);
    }
  }

  public record CashSessionSnapshot(Long id,
                                    CashSessionStatus status,
                                    LocalDateTime openedAt,
                                    LocalDateTime closedAt,
                                    String openedBy,
                                    String closedBy,
                                    BigDecimal openingAmount,
                                    BigDecimal cashIn,
                                    BigDecimal cashOut,
                                    BigDecimal expected,
                                    BigDecimal closingAmount,
                                    BigDecimal difference,
                                    String notes) {}

  public record CashMethodSummary(PaymentMethod method,
                                  long capturedCount,
                                  BigDecimal capturedAmount,
                                  long refundCount,
                                  BigDecimal refundedAmount,
                                  BigDecimal netAmount) {}

  public record CashChannelSummary(PaymentChannel channel,
                                   long count,
                                   BigDecimal amount) {}

  public record CashSessionReconciliation(LocalDateTime from,
                                          LocalDateTime to,
                                          List<CashMethodSummary> methods,
                                          List<CashChannelSummary> channels,
                                          BigDecimal totalCaptured,
                                          BigDecimal totalRefunded,
                                          BigDecimal totalNet) {}

  private static class MutableMethodTotals {
    long capturedCount = 0;
    BigDecimal capturedAmount = BigDecimal.ZERO;
    long refundCount = 0;
    BigDecimal refundedAmount = BigDecimal.ZERO;
  }

  private static class MutableChannelTotals {
    long count = 0;
    BigDecimal amount = BigDecimal.ZERO;
  }
}
