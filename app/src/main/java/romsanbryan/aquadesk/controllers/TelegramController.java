package romsanbryan.aquadesk.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import romsanbryan.aquadesk.services.TelegramService;

@RestController
@RequestMapping("/api/notifications")
public class TelegramController {

    private static final Logger log = LoggerFactory.getLogger(TelegramController.class);
    @Autowired
    private TelegramService telegramService;

    @PostMapping("/telegram/send")
    public ResponseEntity<?> sendNotificationsToTelegram() {
        log.info("Received request to send notifications to Telegram");
        return ResponseEntity.ok(telegramService.sendNotificationsToTelegram());
    }
}
