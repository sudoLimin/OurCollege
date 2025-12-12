package com.example.demo.controller;

import com.example.demo.model.ChatMessage;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.websocket.NotificationService;
import com.example.demo.util.InputSanitizer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/chat")
@CrossOrigin
public class ChatController {

    private final ChatMessageRepository chatRepo;
    private final NotificationService notifier;

    public ChatController(ChatMessageRepository chatRepo, NotificationService notifier) {
        this.chatRepo = chatRepo;
        this.notifier = notifier;
    }

    @GetMapping("/{groupId}")
    public List<ChatMessage> getMessages(@PathVariable Long groupId) {
        return chatRepo.findByGroupIdOrderByTimestampAsc(groupId);
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody ChatMessage message) {
        if (message.getGroupId() == null) {
            return ResponseEntity.badRequest().body("GROUP_ID_REQUIRED");
        }

        message.setContent(InputSanitizer.sanitize(message.getContent()));

        if (message.getContent() == null || message.getContent().isBlank()) {
            return ResponseEntity.badRequest().body("CONTENT_REQUIRED");
        }

        if (message.getUserName() == null || message.getUserName().isBlank()) {
            message.setUserName("User" + (message.getUserId() != null ? message.getUserId() : ""));
        } else {
            message.setUserName(InputSanitizer.sanitize(message.getUserName()));
        }

        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }

        try {
            ChatMessage saved = chatRepo.save(message);
            notifier.notifyChatNew(saved.getUserName(), saved.getContent());
            return ResponseEntity.ok(saved);
        } catch (Exception ex) {
            System.err.println("Error saving chat message: " + ex.getMessage());
            return ResponseEntity.status(500).body("ERROR_SAVING_MESSAGE");
        }
    }
}
