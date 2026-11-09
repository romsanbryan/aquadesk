package romsanbryan.aquadesk.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.aquadesk.services.HistoryExportService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/aquariums/{aquariumId}/history")
public class HistoryExportController {

    private final HistoryExportService historyExportService;
    private final MessageSource messageSource;

    public HistoryExportController(HistoryExportService historyExportService, MessageSource messageSource) {
        this.historyExportService = historyExportService;
        this.messageSource = messageSource;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @PathVariable Integer aquariumId,
            @RequestParam String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean allHistory,
            @RequestParam List<String> sections) {
        HistoryExportService.Format exportFormat = parseFormat(format);
        String extension = exportFormat.name().toLowerCase(Locale.ROOT);
        byte[] content = historyExportService.export(
                aquariumId, exportFormat, startDate, endDate, allHistory, sections);
        String contentType = exportFormat == HistoryExportService.Format.CSV
                ? "text/csv;charset=UTF-8"
                : "text/markdown;charset=UTF-8";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(exportFilename() + "." + extension, StandardCharsets.UTF_8)
                                .build().toString())
                .body(content);
    }

    private String exportFilename() {
        return messageSource.getMessage("historyExport.filename", null, LocaleContextHolder.getLocale());
    }

    private HistoryExportService.Format parseFormat(String format) {
        try {
            return HistoryExportService.Format.valueOf(format.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("Export format must be CSV or MD.");
        }
    }
}
