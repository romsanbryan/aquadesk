package romsanbryan.aquadesk.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import romsanbryan.aquadesk.exception.ValidationException;
import romsanbryan.aquadesk.services.HistoryExportService;

import java.time.LocalDate;
import java.util.List;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryExportControllerTest {

    @Mock
    private HistoryExportService historyExportService;

    private HistoryExportController controller;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/aquadesk");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        controller = new HistoryExportController(historyExportService, messages);
    }

    @AfterEach
    void clearLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void exportsCsvAndMarkdownWithTheirContentTypesAndFileNames() {
        byte[] content = "export".getBytes();
        when(historyExportService.export(anyInt(), any(), any(), any(), anyBoolean(), anyList()))
                .thenReturn(content);

        var csv = controller.export(7, "csv", null, null, true, List.of("events"));
        var markdown = controller.export(7, "MD", LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2), false, List.of("events"));

        assertEquals(MediaType.parseMediaType("text/csv;charset=UTF-8"), csv.getHeaders().getContentType());
        assertTrue(csv.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("aquadesk-historial.csv"));
        assertArrayEquals(content, csv.getBody());
        assertEquals(MediaType.parseMediaType("text/markdown;charset=UTF-8"),
                markdown.getHeaders().getContentType());
        assertTrue(markdown.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("aquadesk-historial.md"));
        assertArrayEquals(content, markdown.getBody());

        verify(historyExportService).export(7, HistoryExportService.Format.CSV,
                null, null, true, List.of("events"));
        verify(historyExportService).export(7, HistoryExportService.Format.MD,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), false, List.of("events"));
    }

    @Test
    void rejectsUnsupportedExportFormat() {
        assertThrows(ValidationException.class,
                () -> controller.export(7, "pdf", null, null, true, List.of("events")));

        verifyNoInteractions(historyExportService);
    }
}
