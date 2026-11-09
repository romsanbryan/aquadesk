package romsanbryan.aquadesk.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import romsanbryan.aquadesk.dto.response.NextEventResponse;
import romsanbryan.aquadesk.dto.response.NotificationParameterResponse;
import romsanbryan.aquadesk.dto.response.AquariumTaskResponse;
import romsanbryan.data.entities.Aquarium;
import romsanbryan.data.repositories.AquariumRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TelegramServiceTest {

    private final TelegramService service = new TelegramService();
    private final ParameterServices parametersService = mock(ParameterServices.class);
    private final EventServices eventServices = mock(EventServices.class);
    private final AquariumTaskService aquariumTaskService = mock(AquariumTaskService.class);
    private final AquariumRepository aquariumRepository = mock(AquariumRepository.class);
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "parametersService", parametersService);
        ReflectionTestUtils.setField(service, "eventServices", eventServices);
        ReflectionTestUtils.setField(service, "aquariumTaskService", aquariumTaskService);
        ReflectionTestUtils.setField(service, "aquariumRepository", aquariumRepository);
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/aquadesk");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        ReflectionTestUtils.setField(service, "messageSource", messages);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
        configure("test-token", "chat-42");
    }

    @org.junit.jupiter.api.AfterEach
    void clearLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void sendsCombinedMessageAsHtmlAndReturnsSuccessForConfiguredBot() throws Exception {
        server.expect(requestTo("https://api.telegram.org/bottest-token/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(request -> {
                    assertInstanceOf(ByteArrayOutputStream.class, request.getBody());
                    JsonNode body = new ObjectMapper().readTree(
                            ((ByteArrayOutputStream) request.getBody()).toString(StandardCharsets.UTF_8));
                    assertEquals("chat-42", body.get("chat_id").asText());
                    assertEquals("HTML", body.get("parse_mode").asText());
                    String text = body.get("text").asText();
                    assertTrue(text.contains("<b>Notification summary</b>"));
                    assertTrue(text.contains("<b>⏰ Due or expiring today:</b>"));
                    assertTrue(text.contains("co2:Carbon"));
                    assertFalse(text.contains("future:Filter"));
                    assertTrue(text.contains("<b>🚨 Detected anomalies:</b>"));
                    assertTrue(text.contains("pH: High"));
                })
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        List<NotificationParameterResponse> parameters =
                List.of(new NotificationParameterResponse("pH", "High"));
        List<NextEventResponse> events = List.of(
                new NextEventResponse("co2:Carbon", LocalDate.now().minusDays(1), LocalDate.now(), 0),
                new NextEventResponse("future:Filter", LocalDate.now(), LocalDate.now().plusDays(4), 4));

        assertTrue(service.sendCombinedNotificationsToTelegram(parameters, events));
        server.verify();
    }

    @Test
    void returnsFalseWithoutCallingTelegramWhenConfigurationIsMissing() {
        configure("", "chat-42");

        assertFalse(service.sendCombinedNotificationsToTelegram(List.of(), List.of()));

        server.verify();
    }

    @Test
    void returnsFalseWhenChatConfigurationIsNullOrEmpty() {
        configure("test-token", null);
        assertFalse(service.sendCombinedNotificationsToTelegram(List.of(), List.of()));
        configure("test-token", "");
        assertFalse(service.sendCombinedNotificationsToTelegram(List.of(), List.of()));
        server.verify();
    }

    @Test
    void returnsFalseWhenTelegramApiCallFails() {
        server.expect(requestTo("https://api.telegram.org/bottest-token/sendMessage"))
                .andRespond(withServerError());

        assertFalse(service.sendCombinedNotificationsToTelegram(List.of(), List.of()));

        server.verify();
    }

    @Test
    void handlesEmptyAnomalyAndEventLists() throws Exception {
        server.expect(requestTo("https://api.telegram.org/bottest-token/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("✅ None"),
                        org.hamcrest.Matchers.containsString("<b>🚨 Detected anomalies:</b>"))))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        assertTrue(service.sendCombinedNotificationsToTelegram(List.of(), List.of()));
        server.verify();
    }

    @Test
    void handlesNullAnomalyListAsNoDetectedAnomalies() throws Exception {
        server.expect(requestTo("https://api.telegram.org/bottest-token/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("✅ None")))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        assertTrue(service.sendCombinedNotificationsToTelegram(null, List.of()));
        server.verify();
    }

    @Test
    void sendsTelegramMessageUsingSpanishBundleWhenSpanishIsConfigured() throws Exception {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));
        server.expect(requestTo("https://api.telegram.org/bottest-token/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("<b>Resumen de notificaciones</b>"),
                        org.hamcrest.Matchers.containsString("<b>⏰ Vencidas o que vencen hoy:</b>"),
                        org.hamcrest.Matchers.containsString("<b>🚨 Anomalías detectadas:</b>"))))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        assertTrue(service.sendCombinedNotificationsToTelegram(List.of(), List.of()));
        server.verify();
    }

    @Test
    void treatsNullEventListAsNoDueEvents() throws Exception {
        server.expect(requestTo("https://api.telegram.org/bottest-token/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("✅ None")))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        assertTrue(service.sendCombinedNotificationsToTelegram(
                List.of(new NotificationParameterResponse("pH", "High")), null));
        server.verify();
    }

    @Test
    void handlesNullReminderAndAnomalyValuesAndEventsWithoutDueDates() throws Exception {
        server.expect(requestTo("https://api.telegram.org/bottest-token/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("✅ None")))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));
        server.expect(requestTo("https://api.telegram.org/bottest-token/sendMessage"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(": ")))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        assertTrue(service.sendCombinedNotificationsToTelegram(
                List.of(new NotificationParameterResponse(null, null)),
                List.of(new NextEventResponse("future", LocalDate.now(), LocalDate.now().plusDays(1), 1)),
                null));
        assertTrue(service.sendCombinedNotificationsToTelegram(
                List.of(new NotificationParameterResponse(null, null)),
                List.of(new NextEventResponse(null, null, LocalDate.now(), 0)),
                null));

        server.verify();
    }

    @Test
    void sendsAllAquariumsInIdOrderWithTheirOwnNotificationsAndReminders() throws Exception {
        Aquarium aquarium2 = aquarium(2, "Reef");
        Aquarium aquarium5 = aquarium(5, "Tropical");
        when(aquariumRepository.findAllByOrderByIdAsc()).thenReturn(List.of(aquarium2, aquarium5));
        List<NotificationParameterResponse> reefParameters =
                List.of(new NotificationParameterResponse("pH", "High"));
        List<NotificationParameterResponse> tropicalParameters =
                List.of(new NotificationParameterResponse("temperature", "Low"));
        when(parametersService.getNotificationParameters(2)).thenReturn(reefParameters);
        when(parametersService.getNotificationParameters(5)).thenReturn(tropicalParameters);
        when(eventServices.getUpcomingEvents(2)).thenReturn(List.of(
                new NextEventResponse("co2:Carbon", LocalDate.now().minusDays(5), LocalDate.now(), 0)));
        when(eventServices.getUpcomingEvents(5)).thenReturn(List.of());
        when(aquariumTaskService.getUpcomingTasks(2)).thenReturn(List.of(
                new AquariumTaskResponse(11, "Clean filter", null, LocalDate.now().plusDays(3), "info")));
        when(aquariumTaskService.getUpcomingTasks(5)).thenReturn(List.of());
        server.expect(requestTo("https://api.telegram.org/bottest-token/sendMessage"))
                .andExpect(request -> {
                    JsonNode body = new ObjectMapper().readTree(
                            ((ByteArrayOutputStream) request.getBody()).toString(StandardCharsets.UTF_8));
                    String message = body.get("text").asText();
                    assertTrue(message.indexOf("🐠 Reef") < message.indexOf("🐠 Tropical"));
                    assertFalse(message.contains("(ID "));
                    assertTrue(message.contains("co2:Carbon"));
                    assertTrue(message.contains("Clean filter"));
                    assertTrue(message.contains("pH: High"));
                    assertTrue(message.contains("temperature: Low"));
                })
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        Map<String, Object> result = service.sendNotificationsToTelegram();

        assertEquals(true, result.get("success"));
        assertEquals("Notifications sent to Telegram", result.get("message"));
        assertEquals(2, result.get("aquariumsCount"));
        assertEquals(2, result.get("parametersCount"));
        assertEquals(List.of(reefParameters.get(0), tropicalParameters.get(0)), result.get("parameters"));
        verify(parametersService).getNotificationParameters(2);
        verify(parametersService).getNotificationParameters(5);
        verify(eventServices).getUpcomingEvents(2);
        verify(eventServices).getUpcomingEvents(5);
        verify(aquariumTaskService).getUpcomingTasks(2);
        verify(aquariumTaskService).getUpcomingTasks(5);
        server.verify();
    }

    @Test
    void returnsFalseWhenTelegramIsNotConfiguredButReportsAquariumParameters() {
        configure(null, "chat-42");
        when(aquariumRepository.findAllByOrderByIdAsc()).thenReturn(List.of(aquarium(5, "Test")));
        List<NotificationParameterResponse> parameters =
                List.of(new NotificationParameterResponse("nitrate", "High"));
        when(parametersService.getNotificationParameters(5)).thenReturn(parameters);
        when(eventServices.getUpcomingEvents(5)).thenReturn(List.of());
        when(aquariumTaskService.getUpcomingTasks(5)).thenReturn(List.of());

        Map<String, Object> result = service.sendNotificationsToTelegram();

        assertEquals(false, result.get("success"));
        assertEquals("Unable to send notifications", result.get("message"));
        assertEquals(1, result.get("aquariumsCount"));
        assertEquals(1, result.get("parametersCount"));
        assertEquals(parameters, result.get("parameters"));
        verify(parametersService).getNotificationParameters(5);
        verify(eventServices).getUpcomingEvents(5);
        server.verify();
    }

    @Test
    void turnsPreparationExceptionsIntoErrorResponse() {
        when(aquariumRepository.findAllByOrderByIdAsc()).thenReturn(List.of(aquarium(8, "Broken")));
        when(parametersService.getNotificationParameters(8))
                .thenThrow(new IllegalStateException("parameters unavailable"));

        Map<String, Object> result = service.sendNotificationsToTelegram();

        assertEquals(false, result.get("success"));
        assertEquals("Error processing the notification request: parameters unavailable", result.get("message"));
        assertFalse(result.containsKey("parameters"));
        verifyNoInteractions(eventServices, aquariumTaskService);
    }

    @Test
    void returnsFailureStatusWhenLegacyRequestCannotSend() {
        configure("", "chat-42");
        when(aquariumRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

        Map<String, Object> result = service.sendNotificationsToTelegram();

        assertEquals(false, result.get("success"));
        assertEquals("Unable to send notifications", result.get("message"));
        server.verify();
    }

    @Test
    void reportsTelegramApiFailureForAllAquariums() {
        when(aquariumRepository.findAllByOrderByIdAsc()).thenReturn(List.of());
        server.expect(requestTo("https://api.telegram.org/bottest-token/sendMessage"))
                .andRespond(withServerError());

        Map<String, Object> result = service.sendNotificationsToTelegram();

        assertEquals(false, result.get("success"));
        assertEquals("Error processing the notification request: Unable to send the Telegram notification.",
                result.get("message"));
        server.verify();
    }

    @Test
    void alsoCapturesPreparationErrorsWhileLoadingAnyAquarium() {
        when(aquariumRepository.findAllByOrderByIdAsc()).thenReturn(List.of(aquarium(8, "Broken")));
        when(parametersService.getNotificationParameters(8)).thenReturn(List.of());
        when(eventServices.getUpcomingEvents(8)).thenThrow(new IllegalArgumentException("event lookup failed"));

        Map<String, Object> result = service.sendNotificationsToTelegram();

        assertEquals(false, result.get("success"));
        assertEquals("Error processing the notification request: event lookup failed", result.get("message"));
        verify(parametersService).getNotificationParameters(8);
        server.verify();
    }

    private Aquarium aquarium(Integer id, String name) {
        Aquarium aquarium = new Aquarium();
        aquarium.setId(id);
        aquarium.setName(name);
        return aquarium;
    }

    private void configure(String token, String chatId) {
        ReflectionTestUtils.setField(service, "botToken", token);
        ReflectionTestUtils.setField(service, "chatId", chatId);
    }
}
