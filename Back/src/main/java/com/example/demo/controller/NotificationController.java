package com.example.demo.controller;

import com.example.demo.model.Notification;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.util.InputSanitizer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@CrossOrigin
public class NotificationController {

    private final NotificationRepository notificationRepo;

    public NotificationController(NotificationRepository notificationRepo) {
        this.notificationRepo = notificationRepo;
    }

    @GetMapping("/{userId}")
    public List<Notification> getNotifications(@PathVariable Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @GetMapping("/{userId}/unread")
    public List<Notification> getUnreadNotifications(@PathVariable Long userId) {
        return notificationRepo.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    @GetMapping("/{userId}/count")
    public Map<String, Long> getUnreadCount(@PathVariable Long userId) {
        long count = notificationRepo.countByUserIdAndReadFalse(userId);
        return Map.of("unreadCount", count);
    }

    @PostMapping
    public ResponseEntity<?> createNotification(@RequestBody Notification notification) {
        if (notification.getUserId() == null) {
            return ResponseEntity.badRequest().body("USER_ID_REQUIRED");
        }
        if (notification.getMessage() == null || notification.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body("MESSAGE_REQUIRED");
        }

        notification.setMessage(InputSanitizer.sanitize(notification.getMessage()));

        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(LocalDateTime.now());
        }
        notification.setRead(false);

        try {
            Notification saved = notificationRepo.save(notification);
            return ResponseEntity.ok(saved);
        } catch (Exception ex) {
            System.err.println("Error saving notification: " + ex.getMessage());
            return ResponseEntity.status(500).body("ERROR_SAVING_NOTIFICATION");
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        return notificationRepo.findById(id)
                .map(notification -> {
                    notification.setRead(true);
                    Notification saved = notificationRepo.save(notification);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.status(404).body(null));
    }

    @PutMapping("/{userId}/read-all")
    public ResponseEntity<?> markAllAsRead(@PathVariable Long userId) {
        List<Notification> unread = notificationRepo.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepo.saveAll(unread);
        return ResponseEntity.ok(Map.of("markedAsRead", unread.size()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        if (!notificationRepo.existsById(id)) {
            return ResponseEntity.status(404).body("NOTIFICATION_NOT_FOUND");
        }
        notificationRepo.deleteById(id);
        return ResponseEntity.ok("OK");
    }
}
