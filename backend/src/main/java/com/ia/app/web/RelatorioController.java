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
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
      @RequestParam(required = false) Long entidadeDefinicaoId,
      @RequestParam(required = false) LocalDate criadoDe,
      @RequestParam(required = false) LocalDate criadoAte) {
    if (entidadeDefinicaoId != null || criadoDe != null || criadoAte != null) {
      return ResponseEntity.ok(service.entidadesPorTipoFiltrado(entidadeDefinicaoId, criadoDe, criadoAte));
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
      @RequestParam(required = false) Long entidadeDefinicaoId,
      @RequestParam(required = false) LocalDate criadoDe,
      @RequestParam(required = false) LocalDate criadoAte) {
    List<RelatorioEntidadeResponse> data = (entidadeDefinicaoId != null || criadoDe != null || criadoAte != null)
      ? service.entidadesPorTipoFiltrado(entidadeDefinicaoId, criadoDe, criadoAte)
      : service.entidadesPorTipo();
    StringBuilder sb = new StringBuilder();
    sb.append("tipo,total\n");
    data.forEach(r -> sb.append(csv(r.nome())).append(',').append(r.total()).append('\n'));
    return csvResponse("relatorio-entidades.csv", sb.toString());
  }

  @GetMapping("/entidades.xlsx")
  @PreAuthorize("@permissaoGuard.hasPermissao('RELATORIO_VIEW')")
  public ResponseEntity<byte[]> entidadesXlsx(
      @RequestParam(required = false) Long entidadeDefinicaoId,
      @RequestParam(required = false) LocalDate criadoDe,
      @RequestParam(required = false) LocalDate criadoAte) {
    List<RelatorioEntidadeResponse> data = (entidadeDefinicaoId != null || criadoDe != null || criadoAte != null)
      ? service.entidadesPorTipoFiltrado(entidadeDefinicaoId, criadoDe, criadoAte)
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

  private String csv(String value) {
    if (value == null) return "";
    String v = value.replace("\"", "\"\"");
    if (v.contains(",") || v.contains("\n") || v.contains("\r")) {
      return "\"" + v + "\"";
    }
    return v;
  }
}
