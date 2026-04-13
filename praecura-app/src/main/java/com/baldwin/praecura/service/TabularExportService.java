package com.baldwin.praecura.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * Shared tabular export writer (CSV/XLSX) used by controllers.
 * Keeps file-format concerns out of web layer endpoints.
 */
@Service
public class TabularExportService {

  private static final int XLSX_ROW_WINDOW = 200;
  private static final int MIN_COLUMN_CHARS = 10;
  private static final int MAX_COLUMN_CHARS = 80;

  @FunctionalInterface
  public interface RowAppender {
    void append(List<?> values);
  }

  @FunctionalInterface
  public interface StreamingSheetWriter {
    void writeRows(RowAppender appender) throws Exception;
  }

  public record StreamingSheetData(
      String name,
      List<String> headers,
      StreamingSheetWriter writer
  ) {
  }

  public void prepareCsvResponse(HttpServletResponse response, String filename) {
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType("text/csv; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
  }

  public PrintWriter csvWriter(HttpServletResponse response) throws Exception {
    return new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
  }

  public void writeBom(PrintWriter out) {
    out.write('\uFEFF');
  }

  public void writeCsvLine(PrintWriter out, boolean formulaSafe, Object... values) {
    String line = Arrays.stream(values)
        .map(v -> csv(v, formulaSafe))
        .collect(Collectors.joining(","));
    out.println(line);
  }

  public void writeXlsx(HttpServletResponse response,
                        String filename,
                        String sheetName,
                        List<String> headers,
                        List<? extends List<?>> rows,
                        boolean formulaSafe) throws Exception {
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    SXSSFWorkbook workbook = createStreamingWorkbook();
    try {
      Sheet sheet = workbook.createSheet(sheetName);
      CellStyle headerStyle = buildHeaderStyle(workbook);
      int[] columnWidths = initColumnWidths(headers);
      writeHeaderRow(sheet, headers, headerStyle);
      int rowIdx = 1;
      for (List<?> rowData : rows) {
        Row row = sheet.createRow(rowIdx++);
        writeDataRow(row, rowData, formulaSafe, columnWidths);
      }
      applyColumnWidths(sheet, columnWidths);
      workbook.write(response.getOutputStream());
    } finally {
      disposeQuietly(workbook);
    }
  }

  public void writeXlsxMulti(HttpServletResponse response,
                             String filename,
                             List<SheetData> sheets,
                             boolean formulaSafe) throws Exception {
    List<StreamingSheetData> streamingSheets = sheets.stream()
        .map(s -> new StreamingSheetData(s.name(), s.headers(), appender -> {
          for (List<?> row : s.rows()) {
            appender.append(row);
          }
        }))
        .toList();
    writeXlsxMultiStreaming(response, filename, streamingSheets, formulaSafe);
  }

  public void writeXlsxMultiStreaming(HttpServletResponse response,
                                      String filename,
                                      List<StreamingSheetData> sheets,
                                      boolean formulaSafe) throws Exception {
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

    SXSSFWorkbook workbook = createStreamingWorkbook();
    try {
      CellStyle headerStyle = buildHeaderStyle(workbook);
      for (StreamingSheetData data : sheets) {
        Sheet sheet = workbook.createSheet(data.name());
        int[] columnWidths = initColumnWidths(data.headers());
        writeHeaderRow(sheet, data.headers(), headerStyle);
        int[] rowIdx = {1};
        data.writer().writeRows(values -> {
          Row row = sheet.createRow(rowIdx[0]++);
          writeDataRow(row, values, formulaSafe, columnWidths);
        });
        applyColumnWidths(sheet, columnWidths);
      }
      workbook.write(response.getOutputStream());
    } finally {
      disposeQuietly(workbook);
    }
  }

  private String csv(Object value, boolean formulaSafe) {
    if (value == null) return "";
    String s = String.valueOf(value);
    if (formulaSafe && isFormulaInjection(s)) {
      s = "'" + s;
    }
    if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
      s = s.replace("\"", "\"\"");
      return "\"" + s + "\"";
    }
    return s;
  }

  private boolean isFormulaInjection(String s) {
    if (s == null || s.isEmpty()) return false;
    char first = s.charAt(0);
    return first == '=' || first == '+' || first == '-' || first == '@';
  }

  private SXSSFWorkbook createStreamingWorkbook() {
    SXSSFWorkbook workbook = new SXSSFWorkbook(XLSX_ROW_WINDOW);
    workbook.setCompressTempFiles(true);
    return workbook;
  }

  private CellStyle buildHeaderStyle(SXSSFWorkbook workbook) {
    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    return headerStyle;
  }

  private void writeHeaderRow(Sheet sheet, List<String> headers, CellStyle headerStyle) {
    Row headerRow = sheet.createRow(0);
    for (int i = 0; i < headers.size(); i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers.get(i));
      cell.setCellStyle(headerStyle);
    }
  }

  private int[] initColumnWidths(List<String> headers) {
    int[] widths = new int[headers.size()];
    for (int i = 0; i < headers.size(); i++) {
      String header = headers.get(i);
      widths[i] = header != null ? header.length() : 0;
    }
    return widths;
  }

  private void writeDataRow(Row row, List<?> rowData, boolean formulaSafe, int[] columnWidths) {
    for (int i = 0; i < rowData.size(); i++) {
      Cell cell = row.createCell(i);
      Object value = rowData.get(i);
      if (value == null) {
        cell.setCellValue("");
        updateColumnWidth(columnWidths, i, 0);
      } else if (value instanceof Number n) {
        cell.setCellValue(n.doubleValue());
        updateColumnWidth(columnWidths, i, String.valueOf(value).length());
      } else {
        String text = String.valueOf(value);
        if (formulaSafe && isFormulaInjection(text)) {
          text = "'" + text;
        }
        cell.setCellValue(text);
        updateColumnWidth(columnWidths, i, text.length());
      }
    }
  }

  private void updateColumnWidth(int[] widths, int index, int valueLength) {
    if (widths == null || index < 0 || index >= widths.length) return;
    if (valueLength > widths[index]) {
      widths[index] = valueLength;
    }
  }

  private void applyColumnWidths(Sheet sheet, int[] widths) {
    if (sheet == null || widths == null) return;
    for (int i = 0; i < widths.length; i++) {
      int adjustedChars = Math.max(MIN_COLUMN_CHARS, Math.min(widths[i] + 2, MAX_COLUMN_CHARS));
      sheet.setColumnWidth(i, adjustedChars * 256);
    }
  }

  private void disposeQuietly(SXSSFWorkbook workbook) throws Exception {
    if (workbook == null) return;
    Exception closeError = null;
    try {
      workbook.dispose();
    } catch (Exception ex) {
      closeError = ex;
    }
    try {
      workbook.close();
    } catch (Exception ex) {
      if (closeError != null) {
        ex.addSuppressed(closeError);
      }
      throw ex;
    }
    if (closeError != null) {
      throw closeError;
    }
  }

  public record SheetData(
      String name,
      List<String> headers,
      List<? extends List<?>> rows
  ) {
  }
}
