package com.example.demo.controller;

import com.example.demo.dto.TaskDTO;
import com.example.demo.model.Task;
import com.example.demo.model.GroupMember;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.GroupMemberRepository;
import com.example.demo.websocket.NotificationService;
import com.example.demo.util.InputSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tasks")
@CrossOrigin
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private SimpMessagingTemplate messaging;

    @Autowired
    private NotificationService notifier;

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody Task t,
            @RequestParam(value = "createdBy", required = false) Long createdByParam) {

        // Sanitize inputs
        t.setTitle(InputSanitizer.sanitize(t.getTitle()));
        t.setDescription(InputSanitizer.sanitize(t.getDescription()));

        System.out.println("RECEIVED TASK JSON:");
        System.out.println("title=" + t.getTitle() + " groupId=" + t.getGroupId() + " createdBy=" + t.getCreatedBy());

        t.setStatus("OPEN");

        // If createdBy not in body, try query param
        if (t.getCreatedBy() == null && createdByParam != null) {
            t.setCreatedBy(createdByParam);
        }

        // Log warning if createdBy is still null
        if (t.getCreatedBy() == null) {
            System.out.println("WARNING: Task created without createdBy - statistics may be affected");
        }

        try {
            Task saved = taskRepository.save(t);

            // notify WebSocket clients
            try {
                messaging.convertAndSend("/topic/tasks", "update");
            } catch (Exception ex) {
                System.err.println("Warning: failed to send WS update: " + ex.getMessage());
            }

            // Send real-time notification and persist to database
            notifier.notifyTaskNewForGroup(saved.getGroupId(), saved.getTitle());

            return ResponseEntity.ok(saved);
        } catch (Exception ex) {
            System.err.println("Error saving task:");
            logException(ex);
            return ResponseEntity.status(500).body("ERROR_SAVING_TASK: " + ex.getMessage());
        }
    }

    // Alternative endpoint: POST /tasks/group/{groupId} - for frontends that send groupId in URL
    @PostMapping({"/group/{groupId}", "/{groupId}"})
    public ResponseEntity<?> createWithGroupId(
            @PathVariable Long groupId,
            @RequestBody Task t,
            @RequestParam(value = "createdBy", required = false) Long createdByParam) {

        // Sanitize inputs
        t.setTitle(InputSanitizer.sanitize(t.getTitle()));
        t.setDescription(InputSanitizer.sanitize(t.getDescription()));

        System.out.println("RECEIVED TASK JSON (with groupId in path):");
        System.out.println("title=" + t.getTitle() + " groupId=" + groupId + " createdBy=" + t.getCreatedBy());

        t.setGroupId(groupId);
        t.setStatus("OPEN");

        // If createdBy not in body, try query param
        if (t.getCreatedBy() == null && createdByParam != null) {
            t.setCreatedBy(createdByParam);
        }

        // Log warning if createdBy is still null
        if (t.getCreatedBy() == null) {
            System.out.println("WARNING: Task created without createdBy - statistics may be affected");
        }

        try {
            Task saved = taskRepository.save(t);

            // notify WebSocket clients
            try {
                messaging.convertAndSend("/topic/tasks", "update");
            } catch (Exception ex) {
                System.err.println("Warning: failed to send WS update: " + ex.getMessage());
            }

            // Send real-time notification and persist to database
            notifier.notifyTaskNewForGroup(saved.getGroupId(), saved.getTitle());

            return ResponseEntity.ok(saved);
        } catch (Exception ex) {
            System.err.println("Error saving task:");
            logException(ex);
            return ResponseEntity.status(500).body("ERROR_SAVING_TASK: " + ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable Long id, @RequestBody Task updated) {
        Task t = taskRepository.findById(id).orElseThrow();

        // Sanitize inputs
        t.setTitle(InputSanitizer.sanitize(updated.getTitle()));
        t.setDescription(InputSanitizer.sanitize(updated.getDescription()));
        t.setStatus(updated.getStatus());

        Task saved = taskRepository.save(t);

        messaging.convertAndSend("/topic/tasks", "update");

        return saved;
    }



    @GetMapping({"/group/{groupId}", "/{groupId}"})
    public List<Task> getTasksByGroup(@PathVariable Long groupId) {
        return taskRepository.findByGroupId(groupId);
    }



    @GetMapping("/info/{id}")
    public TaskDTO getTaskInfo(@PathVariable Long id) {

        Task task = taskRepository.findById(id).orElseThrow();

        TaskDTO dto = new TaskDTO();
        dto.id = task.getId();
        dto.title = task.getTitle();
        dto.description = task.getDescription();
        dto.createdBy = task.getCreatedBy();
        dto.status = task.getStatus();
        dto.groupId = task.getGroupId();
        dto.createdAt = task.getCreatedAt();

        return dto;
    }



    @PutMapping("/status/{id}")
    public void updateStatus(@PathVariable Long id, @RequestBody String status) {
        Task t = taskRepository.findById(id).orElseThrow();
        t.setStatus(status);
        taskRepository.save(t);

        messaging.convertAndSend("/topic/tasks", "update");
    }


    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable Long id) {
        taskRepository.deleteById(id);

        messaging.convertAndSend("/topic/tasks", "update");
    }

    @PatchMapping("/{id}/status")
    public Task updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");

        Task t = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        t.setStatus(newStatus);

        Task saved = taskRepository.save(t);

        // Оповіщаємо фронт про оновлення
        messaging.convertAndSend("/topic/tasks", "update");

        return saved;
    }

    private void logException(Exception ex) {
        System.err.println("Error occurred: " + ex.getMessage());
    }

    @PatchMapping("/{id}/deadline")
    public Task updateDeadline(@PathVariable Long id, @RequestBody Task data) {
        Task t = taskRepository.findById(id).orElseThrow();
        t.setDeadline(data.getDeadline());
        return taskRepository.save(t);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTaskById(@PathVariable Long id) {
        try {
            Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
            return ResponseEntity.ok(task);
        } catch (Exception ex) {
            System.err.println("Error fetching task:");
            logException(ex);
            return ResponseEntity.status(404).body("TASK_NOT_FOUND");
        }
    }

    @GetMapping("/upcoming/{userId}")
    public List<Task> getUpcomingDeadlines(@PathVariable Long userId) {
        // Find all groups user participates in
        List<GroupMember> memberships = groupMemberRepository.findByUserId(userId);
        List<Long> groupIds = memberships.stream()
                .map(GroupMember::getGroupId)
                .collect(Collectors.toList());

        if (groupIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Get all tasks from these groups
        List<Task> allTasks = new ArrayList<>();
        for (Long groupId : groupIds) {
            allTasks.addAll(taskRepository.findByGroupId(groupId));
        }

        // Filter tasks with deadline within next 24 hours
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in24Hours = now.plusHours(24);

        return allTasks.stream()
                .filter(task -> task.getDeadline() != null && !task.getDeadline().isBlank())
                .filter(task -> {
                    try {
                        LocalDateTime deadline = LocalDateTime.parse(task.getDeadline());
                        return deadline.isAfter(now) && deadline.isBefore(in24Hours);
                    } catch (Exception e) {
                        // Try alternative format
                        try {
                            LocalDateTime deadline = LocalDateTime.parse(task.getDeadline(),
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                            return deadline.isAfter(now) && deadline.isBefore(in24Hours);
                        } catch (Exception e2) {
                            return false;
                        }
                    }
                })
                .collect(Collectors.toList());
    }
}
