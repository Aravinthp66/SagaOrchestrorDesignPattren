package com.card.notification.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NotificationController {
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    @PostMapping("/notification")
    Map<String, String> sendNotification(@RequestBody Map<String, String> message){
            message.put("status","message sent successful");
          log.info("sendNotification ::success::{}",message);
        return message;
    }

}
