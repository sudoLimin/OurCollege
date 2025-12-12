package com.example.demo.websocket;

import com.example.demo.model.Notification;
import com.example.demo.model.GroupMember;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.GroupMemberRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final GroupMemberRepository groupMemberRepo;

    public NotificationService(NotificationRepository notificationRepo, GroupMemberRepository groupMemberRepo) {
        this.notificationRepo = notificationRepo;
        this.groupMemberRepo = groupMemberRepo;
    }

    public void notifyAll(String type, String content) {
        String escapedContent = content.replace("\"", "\\\"");
        String msg = "{ \"type\": \"" + type + "\", \"content\": \"" + escapedContent + "\" }";
        NotificationWS.broadcast(msg);
    }

    public void notifyTaskNew(String taskTitle) {
        notifyAll("task_new", "New task added: " + taskTitle);
    }
    
    public void notifyTaskNewForGroup(Long groupId, String taskTitle) {
        String message = "New task added: " + taskTitle;
        notifyAll("task_new", message);
        saveNotificationForGroupMembers(groupId, message);
    }

    public void notifyMemberNew(String groupName) {
        notifyAll("member_new", "New member joined group: " + groupName);
    }
    
    public void notifyMemberNewForGroup(Long groupId, String memberName) {
        String message = "New member joined: " + memberName;
        notifyAll("member_new", message);
        saveNotificationForGroupMembers(groupId, message);
    }

    public void notifyMaterialNew(String materialTitle) {
        notifyAll("material_new", "New material added: " + materialTitle);
    }
    
    public void notifyMaterialNewForGroup(Long groupId, String materialTitle) {
        String message = "New material added: " + materialTitle;
        notifyAll("material_new", message);
        saveNotificationForGroupMembers(groupId, message);
    }

    public void notifyTaskUpdated(String taskTitle) {
        notifyAll("task_updated", "Task updated: " + taskTitle);
    }

    public void notifyTaskDeleted() {
        notifyAll("task_deleted", "Task deleted");
    }

    public void notifyChatNew(String userName, String content) {
        notifyAll("chat_new", userName + ": " + content);
    }
    
    private void saveNotificationForGroupMembers(Long groupId, String message) {
        if (groupId == null) return;
        
        try {
            List<GroupMember> members = groupMemberRepo.findByGroupId(groupId);
            for (GroupMember member : members) {
                Notification notification = new Notification();
                notification.setUserId(member.getUserId());
                notification.setMessage(message);
                notification.setCreatedAt(LocalDateTime.now());
                notification.setRead(false);
                notificationRepo.save(notification);
            }
        } catch (Exception e) {
            System.err.println("Error saving notifications: " + e.getMessage());
        }
    }
    
    public void saveNotificationForUser(Long userId, String message) {
        if (userId == null) return;
        
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setMessage(message);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRead(false);
            notificationRepo.save(notification);
        } catch (Exception e) {
            System.err.println("Error saving notification: " + e.getMessage());
        }
    }
}
