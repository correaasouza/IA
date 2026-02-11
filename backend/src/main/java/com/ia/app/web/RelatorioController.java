package com.ia.app.web;

import com.ia.app.dto.RelatorioContatoResponse;
import com.ia.app.dto.RelatorioEntidadeComparativoResponse;
import com.ia.app.dto.RelatorioEntidadeResponse;
import com.ia.app.dto.RelatorioLocatarioStatusResponse;
import com.ia.app.dto.RelatorioPendenciaContatoResponse;
import com.ia.app.service.RelatorioService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

  private final RelatorioService service;

  public RelatorioController(RelatorioService service) {
    this.service = service;
  }

  @GetMapping("/entidades")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<List<RelatorioEntidadeResponse>> entidades(
      @RequestParam(required = false) Long tipoEntidadeId,
      @RequestParam(required = false) Long entidadeDefinicaoId,
      @RequestParam(required = false) LocalDate criadoDe,
      @RequestParam(required = false) LocalDate criadoAte) {
    Long effectiveId = tipoEntidadeId != null ? tipoEntidadeId : entidadeDefinicaoId;
    if (effectiveId != null || criadoDe != null || criadoAte != null) {
      return ResponseEntity.ok(service.entidadesPorTipoFiltrado(effectiveId, criadoDe, criadoAte));
    }
    return ResponseEntity.ok(service.entidadesPorTipo());
  }

  @GetMapping("/entidades-comparativo")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<List<RelatorioEntidadeComparativoResponse>> entidadesComparativo(
      @RequestParam(required = false) LocalDate criadoDe1,
      @RequestParam(required = false) LocalDate criadoAte1,
      @RequestParam(required = false) LocalDate criadoDe2,
      @RequestParam(required = false) LocalDate criadoAte2) {
    return ResponseEntity.ok(service.entidadesComparativo(criadoDe1, criadoAte1, criadoDe2, criadoAte2));
  }

  @GetMapping("/contatos")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<List<RelatorioContatoResponse>> contatos() {
    return ResponseEntity.ok(service.contatosPorTipo());
  }

  @GetMapping("/pendencias-contato")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<List<RelatorioPendenciaContatoResponse>> pendenciasContato() {
    return ResponseEntity.ok(service.pendenciasContato());
  }

  @GetMapping("/locatarios")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<RelatorioLocatarioStatusResponse> locatarios() {
    return ResponseEntity.ok(service.locatariosStatus());
  }

  @GetMapping("/entidades.csv")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<byte[]> entidadesCsv(
      @RequestParam(required = false) Long tipoEntidadeId,
      @RequestParam(required = false) Long entidadeDefinicaoId,
      @RequestParam(required = false) LocalDate criadoDe,
      @RequestParam(required = false) LocalDate criadoAte) {
    Long effectiveId = tipoEntidadeId != null ? tipoEntidadeId : entidadeDefinicaoId;
    List<RelatorioEntidadeResponse> data = (effectiveId != null || criadoDe != null || criadoAte != null)
      ? service.entidadesPorTipoFiltrado(effectiveId, criadoDe, criadoAte)
      : service.entidadesPorTipo();
    StringBuilder sb = new StringBuilder();
    sb.append("tipo,total\n");
    data.forEach(r -> sb.append(csv(r.nome())).append(',').append(r.total()).append('\n'));
    return csvResponse("relatorio-entidades.csv", sb.toString());
  }

  @GetMapping("/entidades.xlsx")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<byte[]> entidadesXlsx(
      @RequestParam(required = false) Long tipoEntidadeId,
      @RequestParam(required = false) Long entidadeDefinicaoId,
      @RequestParam(required = false) LocalDate criadoDe,
      @RequestParam(required = false) LocalDate criadoAte) {
    Long effectiveId = tipoEntidadeId != null ? tipoEntidadeId : entidadeDefinicaoId;
    List<RelatorioEntidadeResponse> data = (effectiveId != null || criadoDe != null || criadoAte != null)
      ? service.entidadesPorTipoFiltrado(effectiveId, criadoDe, criadoAte)
      : service.entidadesPorTipo();
    Workbook wb = new XSSFWorkbook();
    Sheet sheet = wb.createSheet("Entidades");
    Row header = sheet.createRow(0);
    header.createCell(0).setCellValue("tipo");
    header.createCell(1).setCellValue("total");
    int i = 1;
    for (RelatorioEntidadeResponse r : data) {
      Row row = sheet.createRow(i++);
      row.createCell(0).setCellValue(r.nome());
      row.createCell(1).setCellValue(r.total());
    }
    sheet.autoSizeColumn(0);
    sheet.autoSizeColumn(1);
    return xlsxResponse("relatorio-entidades.xlsx", wb);
  }

  @GetMapping("/entidades.pdf")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<byte[]> entidadesPdf(
      @RequestParam(required = false) Long tipoEntidadeId,
      @RequestParam(required = false) Long entidadeDefinicaoId,
      @RequestParam(required = false) LocalDate criadoDe,
      @RequestParam(required = false) LocalDate criadoAte) {
    Long effectiveId = tipoEntidadeId != null ? tipoEntidadeId : entidadeDefinicaoId;
    List<RelatorioEntidadeResponse> data = (effectiveId != null || criadoDe != null || criadoAte != null)
      ? service.entidadesPorTipoFiltrado(effectiveId, criadoDe, criadoAte)
      : service.entidadesPorTipo();
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Tipo", "Total" });
    data.forEach(r -> rows.add(new String[] { r.nome(), String.valueOf(r.total()) }));
    return pdfResponse("relatorio-entidades.pdf", simplePdf("Relatorio Entidades", rows));
  }

  @GetMapping("/entidades-comparativo.xlsx")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<byte[]> entidadesComparativoXlsx(
      @RequestParam(required = false) LocalDate criadoDe1,
      @RequestParam(required = false) LocalDate criadoAte1,
      @RequestParam(required = false) LocalDate criadoDe2,
      @RequestParam(required = false) LocalDate criadoAte2) {
    List<RelatorioEntidadeComparativoResponse> data = service.entidadesComparativo(criadoDe1, criadoAte1, criadoDe2, criadoAte2);
    Workbook wb = new XSSFWorkbook();
    Sheet sheet = wb.createSheet("Comparativo");
    Row header = sheet.createRow(0);
    header.createCell(0).setCellValue("tipo");
    header.createCell(1).setCellValue("periodo1");
    header.createCell(2).setCellValue("periodo2");
    header.createCell(3).setCellValue("variacao");
    int i = 1;
    for (RelatorioEntidadeComparativoResponse r : data) {
      Row row = sheet.createRow(i++);
      row.createCell(0).setCellValue(r.nome());
      row.createCell(1).setCellValue(r.totalPeriodo1());
      row.createCell(2).setCellValue(r.totalPeriodo2());
      row.createCell(3).setCellValue(r.totalPeriodo2() - r.totalPeriodo1());
    }
    sheet.autoSizeColumn(0);
    sheet.autoSizeColumn(1);
    sheet.autoSizeColumn(2);
    sheet.autoSizeColumn(3);
    return xlsxResponse("relatorio-entidades-comparativo.xlsx", wb);
  }

  @GetMapping("/entidades-comparativo.pdf")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<byte[]> entidadesComparativoPdf(
      @RequestParam(required = false) LocalDate criadoDe1,
      @RequestParam(required = false) LocalDate criadoAte1,
      @RequestParam(required = false) LocalDate criadoDe2,
      @RequestParam(required = false) LocalDate criadoAte2) {
    List<RelatorioEntidadeComparativoResponse> data = service.entidadesComparativo(criadoDe1, criadoAte1, criadoDe2, criadoAte2);
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Tipo", "Periodo1", "Periodo2", "Variacao" });
    data.forEach(r -> rows.add(new String[] {
      r.nome(),
      String.valueOf(r.totalPeriodo1()),
      String.valueOf(r.totalPeriodo2()),
      String.valueOf(r.totalPeriodo2() - r.totalPeriodo1())
    }));
    return pdfResponse("relatorio-entidades-comparativo.pdf", simplePdf("Relatorio Comparativo", rows));
  }

  @GetMapping("/contatos.csv")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<byte[]> contatosCsv() {
    List<RelatorioContatoResponse> data = service.contatosPorTipo();
    StringBuilder sb = new StringBuilder();
    sb.append("tipo,total\n");
    data.forEach(r -> sb.append(csv(r.tipo())).append(',').append(r.total()).append('\n'));
    return csvResponse("relatorio-contatos.csv", sb.toString());
  }

  @GetMapping("/contatos.xlsx")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<byte[]> contatosXlsx() {
    List<RelatorioContatoResponse> data = service.contatosPorTipo();
    Workbook wb = new XSSFWorkbook();
    Sheet sheet = wb.createSheet("Contatos");
    Row header = sheet.createRow(0);
    header.createCell(0).setCellValue("tipo");
    header.createCell(1).setCellValue("total");
    int i = 1;
    for (RelatorioContatoResponse r : data) {
      Row row = sheet.createRow(i++);
      row.createCell(0).setCellValue(r.tipo());
      row.createCell(1).setCellValue(r.total());
    }
    sheet.autoSizeColumn(0);
    sheet.autoSizeColumn(1);
    return xlsxResponse("relatorio-contatos.xlsx", wb);
  }

  @GetMapping("/contatos.pdf")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<byte[]> contatosPdf() {
    List<RelatorioContatoResponse> data = service.contatosPorTipo();
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Tipo", "Total" });
    data.forEach(r -> rows.add(new String[] { r.tipo(), String.valueOf(r.total()) }));
    return pdfResponse("relatorio-contatos.pdf", simplePdf("Relatorio Contatos", rows));
  }

  @GetMapping("/pendencias-contato.csv")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<byte[]> pendenciasCsv() {
    List<RelatorioPendenciaContatoResponse> data = service.pendenciasContato();
    StringBuilder sb = new StringBuilder();
    sb.append("entidade_id,entidade,contato\n");
    data.forEach(r -> sb.append(r.entidadeRegistroId())
      .append(',').append(csv(r.entidadeNome()))
      .append(',').append(csv(r.tipoContato()))
      .append('\n'));
    return csvResponse("relatorio-pendencias.csv", sb.toString());
  }

  @GetMapping("/pendencias-contato.xlsx")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<byte[]> pendenciasXlsx() {
    List<RelatorioPendenciaContatoResponse> data = service.pendenciasContato();
    Workbook wb = new XSSFWorkbook();
    Sheet sheet = wb.createSheet("Pendencias");
    Row header = sheet.createRow(0);
    header.createCell(0).setCellValue("entidade_id");
    header.createCell(1).setCellValue("entidade");
    header.createCell(2).setCellValue("contato");
    int i = 1;
    for (RelatorioPendenciaContatoResponse r : data) {
      Row row = sheet.createRow(i++);
      row.createCell(0).setCellValue(r.entidadeRegistroId());
      row.createCell(1).setCellValue(r.entidadeNome());
      row.createCell(2).setCellValue(r.tipoContato());
    }
    sheet.autoSizeColumn(0);
    sheet.autoSizeColumn(1);
    sheet.autoSizeColumn(2);
    return xlsxResponse("relatorio-pendencias.xlsx", wb);
  }

  @GetMapping("/pendencias-contato.pdf")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<byte[]> pendenciasPdf() {
    List<RelatorioPendenciaContatoResponse> data = service.pendenciasContato();
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Entidade", "Contato" });
    data.forEach(r -> rows.add(new String[] { r.entidadeNome(), r.tipoContato() }));
    return pdfResponse("relatorio-pendencias.pdf", simplePdf("Relatorio Pendencias", rows));
  }

  private ResponseEntity<byte[]> csvResponse(String filename, String content) {
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
      .contentType(MediaType.valueOf("text/csv;charset=UTF-8"))
      .body(bytes);
  }

  private ResponseEntity<byte[]> xlsxResponse(String filename, Workbook workbook) {
    try (Workbook wb = workbook; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      wb.write(out);
      byte[] bytes = out.toByteArray();
      return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
    } catch (IOException e) {
      throw new IllegalStateException("xlsx_export_failed", e);
    }
  }

  private ResponseEntity<byte[]> pdfResponse(String filename, byte[] content) {
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
      .contentType(MediaType.APPLICATION_PDF)
      .body(content);
  }

  private byte[] simplePdf(String title, List<String[]> rows) {
    try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PDPage page = new PDPage(PDRectangle.A4);
      doc.addPage(page);
      PDPageContentStream content = new PDPageContentStream(doc, page);

      float margin = 48;
      float y = page.getMediaBox().getHeight() - margin;
      float leading = 14;

      content.setFont(PDType1Font.HELVETICA_BOLD, 14);
      content.beginText();
      content.newLineAtOffset(margin, y);
      content.showText(Objects.toString(title, "Relatorio"));
      content.endText();

      y -= leading * 1.5f;
      content.setFont(PDType1Font.HELVETICA, 10);

      for (String[] row : rows) {
        if (y < margin) {
          content.close();
          page = new PDPage(PDRectangle.A4);
          doc.addPage(page);
          content = new PDPageContentStream(doc, page);
          y = page.getMediaBox().getHeight() - margin;
        }
        String line = java.util.Arrays.stream(row)
          .map(v -> v == null ? "" : v)
          .collect(Collectors.joining(" | "));
        content.beginText();
        content.newLineAtOffset(margin, y);
        content.showText(line);
        content.endText();
        y -= leading;
      }

      content.close();
      doc.save(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("pdf_export_failed", e);
    }
  }

  private String csv(String value) {
    if (value == null) return "";
    String v = value.replace("\"", "\"\"");
    if (v.contains(",") || v.contains("\n") || v.contains("\r")) {
      return "\"" + v + "\"";
    }
    return v;
  }
}
