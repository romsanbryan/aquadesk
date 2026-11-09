package romsanbryan.aquadesk.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import romsanbryan.aquadesk.dto.response.AquariumTaskResponse;
import romsanbryan.aquadesk.dto.response.NextEventResponse;
import romsanbryan.aquadesk.dto.response.NotificationParameterResponse;
import romsanbryan.aquadesk.exception.ExternalServiceException;
import romsanbryan.data.entities.Aquarium;
import romsanbryan.data.repositories.AquariumRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);
    @Value("${telegram.bot-token:}")
    private String botToken;

    @Value("${telegram.chat-id:}")
    private String chatId;

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Autowired
    private ParameterServices parametersService;
    @Autowired
    private EventServices eventServices;
    @Autowired
    private AquariumTaskService aquariumTaskService;
    @Autowired
    private AquariumRepository aquariumRepository;
    @Autowired
    private MessageSource messageSource;


    public Map<String, Object> sendNotificationsToTelegram() {
        try {
            log.info("Preparing notification message for all aquariums");
            List<Aquarium> aquariums = aquariumRepository.findAllByOrderByIdAsc();
            StringBuilder message = new StringBuilder();
            message.append("🔔 <b>").append(text("telegram.summary")).append("</b>\n");
            message.append("📅 ").append(LocalDate.now()).append("\n");
            message.append("⏰ ").append(String.format("%02d:%02d",
                    LocalDateTime.now().getHour(), LocalDateTime.now().getMinute())).append("\n\n");

            List<NotificationParameterResponse> allParameters = new ArrayList<>();
            for (Aquarium aquarium : aquariums) {
                Integer aquariumId = aquarium.getId();
                List<NotificationParameterResponse> notificationParameters =
                        parametersService.getNotificationParameters(aquariumId);
                List<NextEventResponse> events = eventServices.getUpcomingEvents(aquariumId);
                List<AquariumTaskResponse> upcomingTasks = aquariumTaskService.getUpcomingTasks(aquariumId);
                allParameters.addAll(notificationParameters);
                appendAquariumMessage(message, aquarium, notificationParameters, events, upcomingTasks);
            }

            boolean sent = false;
            if (isConfigured()) {
                sent = sendMessage(message.toString());
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", sent);
            response.put("message", text(sent ? "telegram.sent" : "telegram.sendFailed"));
            response.put("aquariumsCount", aquariums.size());
            response.put("parametersCount", allParameters.size());
            response.put("parameters", allParameters);
            return response;
        } catch (RuntimeException e) {
            log.error("Failed to prepare Telegram notification", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error processing the notification request: " + e.getMessage());
            return error;
        }
    }

    public boolean sendCombinedNotificationsToTelegram(
            List<NotificationParameterResponse> notificationParameters, List<NextEventResponse> events) {
        return sendCombinedNotificationsToTelegram(notificationParameters, events, List.of());
    }

    public boolean sendCombinedNotificationsToTelegram(
            List<NotificationParameterResponse> notificationParameters,
            List<NextEventResponse> events,
            List<AquariumTaskResponse> upcomingTasks) {
        if (!isConfigured()) {
            log.warn("Telegram integration is not configured; skipping message delivery");
            return false;
        }

        try {
            String message = buildCombinedMessage(notificationParameters, events, upcomingTasks);
            return sendMessage(message);
        } catch (RuntimeException e) {
            log.error("Failed to build or send Telegram notification", e);
            return false;
        }
    }


    private String buildCombinedMessage(
            List<NotificationParameterResponse> notificationParameters,
            List<NextEventResponse> events,
            List<AquariumTaskResponse> upcomingTasks) {
        StringBuilder message = new StringBuilder();
        message.append("🔔 <b>").append(text("telegram.summary")).append("</b>\n");
        message.append("📅 ").append(LocalDate.now()).append("\n");
        message.append("⏰ ").append(String.format("%02d:%02d", LocalDateTime.now().getHour(), LocalDateTime.now().getMinute())).append("\n\n");

        appendAquariumSections(message, notificationParameters, events, upcomingTasks);
        return message.toString();
    }

    private void appendAquariumMessage(
            StringBuilder message,
            Aquarium aquarium,
            List<NotificationParameterResponse> notificationParameters,
            List<NextEventResponse> events,
            List<AquariumTaskResponse> upcomingTasks) {
        message.append("<b>🐠 ").append(escapeHtml(aquarium.getName()))
                    .append("</b>\n");
        appendAquariumSections(message, notificationParameters, events, upcomingTasks);
        message.append("\n");
    }

    private void appendAquariumSections(
            StringBuilder message,
            List<NotificationParameterResponse> notificationParameters,
            List<NextEventResponse> events,
            List<AquariumTaskResponse> upcomingTasks) {
        message.append("<b>⏰ ").append(text("telegram.dueToday")).append("</b>\n");
        boolean hasDueEvents = events != null && events.stream().anyMatch(event -> event.remainingDays() <= 0);
        if (!hasDueEvents) {
            message.append("   ✅ ").append(text("telegram.none")).append("\n");
        } else {
            for (NextEventResponse notif : events) {
                if (notif.remainingDays() <= 0) {
                    message.append("   • ").append(escapeHtml(notif.type()))
                            .append(": ").append(notif.lastDate()).append("\n");
                }
            }
        }

        message.append("\n");

        message.append("<b>🔔 ").append(text("telegram.reminders")).append("</b>\n");
        if (upcomingTasks == null || upcomingTasks.isEmpty()) {
            message.append("   ✅ ").append(text("telegram.none")).append("\n");
        } else {
            for (AquariumTaskResponse task : upcomingTasks) {
                message.append("   • ").append(escapeHtml(task.title()))
                        .append(": ").append(task.actionDate()).append("\n");
            }
        }

        message.append("\n");

        message.append("<b>🚨 ").append(text("telegram.anomalies")).append("</b>\n");
        if (notificationParameters == null || notificationParameters.isEmpty()) {
            message.append("   ✅ ").append(text("telegram.none"));
        } else {
            for (NotificationParameterResponse param : notificationParameters) {
                message.append("   • ").append(escapeHtml(param.parameter()))
                        .append(": ").append(escapeHtml(param.value())).append("\n");
            }
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private boolean sendMessage(String message) {
        try {
            String url = TELEGRAM_API_URL + botToken + "/sendMessage";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chat_id", chatId);
            requestBody.put("text", message);
            requestBody.put("parse_mode", "HTML");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            restTemplate.postForObject(url, entity, String.class);
            log.info("Telegram notification sent successfully");
            return true;
        } catch (JsonProcessingException | RestClientException e) {
            log.error("Telegram API request failed", e);
            throw new ExternalServiceException("Unable to send the Telegram notification.", e);
        }
    }

    private boolean isConfigured() {
        return botToken != null && !botToken.isEmpty() &&
                chatId != null && !chatId.isEmpty();
    }

    private String text(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

}
