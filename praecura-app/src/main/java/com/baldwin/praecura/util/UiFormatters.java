package com.baldwin.praecura.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Small UI-focused format helpers for Thymeleaf templates.
 *
 * <p>These helpers intentionally keep logic minimal and deterministic, to avoid
 * complex expressions in the HTML templates.</p>
 */
public final class UiFormatters {

  private UiFormatters() {}

  /**
   * Formats a duration expressed in minutes.
   *
   * <ul>
   *   <li>&lt; 60 minutes: "{m} min"</li>
   *   <li>&ge; 60 minutes: "{h} h" or "{h} h {mm} min" (if remainder &gt; 0)</li>
   * </ul>
   */
  public static String formatDuration(int totalMinutes) {
    if (totalMinutes <= 0) {
      return "-";
    }

    if (totalMinutes < 60) {
      return totalMinutes + " min";
    }

    int hours = totalMinutes / 60;
    int minutes = totalMinutes % 60;

    if (minutes == 0) {
      return hours + " h";
    }

    return String.format("%d h %02d min", hours, minutes);
  }

  /** Formats money as $ with 2 decimals. */
  public static String formatMoneyUSD(BigDecimal amount) {
    if (amount == null) {
      return "$0.00";
    }
    BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);

    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
    return "$" + df.format(normalized);
  }

  /** Formats money as RD$ with 2 decimals (Dominican pesos). */
  public static String formatMoneyDOP(BigDecimal amount) {
    if (amount == null) {
      return "RD$ 0.00";
    }
    BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);

    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
    return "RD$ " + df.format(normalized);
  }

  /** Formats doctor availability as "Lun, Mar · 08:00–17:00". */
  public static String formatSchedule(String workDays, LocalTime start, LocalTime end) {
    String days = formatWorkDays(workDays);
    String time = "";
    if (start != null && end != null) {
      DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
      time = fmt.format(start) + "–" + fmt.format(end);
    }
    if (!days.isBlank() && !time.isBlank()) {
      return days + " · " + time;
    }
    return !days.isBlank() ? days : time;
  }

  /** Formats a number with comma decimal separator (es-DO style). */
  public static String formatDecimal(Number value, int decimals) {
    if (decimals < 0) decimals = 0;
    String zero = decimals == 0 ? "0" : "0," + "0".repeat(decimals);
    if (value == null) {
      return zero;
    }

    BigDecimal normalized;
    try {
      normalized = (value instanceof BigDecimal)
          ? ((BigDecimal) value)
          : new BigDecimal(value.toString());
    } catch (Exception ex) {
      return zero;
    }

    normalized = normalized.setScale(decimals, RoundingMode.HALF_UP);

    DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "DO"));
    symbols.setDecimalSeparator(',');
    symbols.setGroupingSeparator('.');
    String pattern = decimals > 0 ? "#,##0." + "0".repeat(decimals) : "#,##0";
    DecimalFormat df = new DecimalFormat(pattern, symbols);
    return df.format(normalized);
  }

  public static String formatWorkDays(String raw) {
    if (raw == null || raw.isBlank()) return "";
    String[] parts = raw.split(",");
    List<String> out = new ArrayList<>();
    for (String p : parts) {
      if (p == null) continue;
      String t = p.trim().toUpperCase();
      switch (t) {
        case "MON" -> out.add("Lun");
        case "TUE" -> out.add("Mar");
        case "WED" -> out.add("Mié");
        case "THU" -> out.add("Jue");
        case "FRI" -> out.add("Vie");
        case "SAT" -> out.add("Sáb");
        case "SUN" -> out.add("Dom");
        default -> {}
      }
    }
    return String.join(", ", out);
  }
}
