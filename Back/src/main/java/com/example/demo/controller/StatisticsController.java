package com.example.demo.controller;

import com.example.demo.model.GroupMember;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.repository.GroupMemberRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stats")
@CrossOrigin
public class StatisticsController {

    private final TaskRepository taskRepo;
    private final GroupMemberRepository memberRepo;
    private final UserRepository userRepo;

    public StatisticsController(TaskRepository taskRepo, GroupMemberRepository memberRepo, UserRepository userRepo) {
        this.taskRepo = taskRepo;
        this.memberRepo = memberRepo;
        this.userRepo = userRepo;
    }

    // GET /stats/group/{groupId} - Get overall group statistics
    @GetMapping("/group/{groupId}")
    public Map<String, Object> getGroupStatistics(@PathVariable Long groupId) {
        Map<String, Object> stats = new HashMap<>();

        // Total tasks
        long totalTasks = taskRepo.countByGroupId(groupId);
        stats.put("totalTasks", totalTasks);

        // Tasks by status
        long tasksOpen = taskRepo.countByGroupIdAndStatus(groupId, "OPEN");
        long tasksInProgress = taskRepo.countByGroupIdAndStatus(groupId, "IN_PROGRESS");
        long tasksDone = taskRepo.countByGroupIdAndStatus(groupId, "DONE");

        stats.put("tasksOpen", tasksOpen);
        stats.put("tasksInProgress", tasksInProgress);
        stats.put("tasksDone", tasksDone);

        // Tasks completed in time ranges (based on createdAt field for DONE tasks)
        List<Task> allTasks = taskRepo.findByGroupId(groupId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().minusDays(30).atStartOfDay();

        long tasksCompletedToday = 0;
        long tasksCompletedThisWeek = 0;
        long tasksCompletedThisMonth = 0;

        for (Task task : allTasks) {
            if ("DONE".equals(task.getStatus()) && task.getCreatedAt() != null) {
                try {
                    LocalDateTime createdAt = LocalDateTime.parse(task.getCreatedAt());
                    if (createdAt.isAfter(startOfToday)) {
                        tasksCompletedToday++;
                    }
                    if (createdAt.isAfter(startOfWeek)) {
                        tasksCompletedThisWeek++;
                    }
                    if (createdAt.isAfter(startOfMonth)) {
                        tasksCompletedThisMonth++;
                    }
                } catch (Exception e) {
                    // Ignore parsing errors
                }
            }
        }

        stats.put("tasksCompletedToday", tasksCompletedToday);
        stats.put("tasksCompletedThisWeek", tasksCompletedThisWeek);
        stats.put("tasksCompletedThisMonth", tasksCompletedThisMonth);

        return stats;
    }

    // GET /stats/group/{groupId}/members - Get member contribution statistics
    @GetMapping("/group/{groupId}/members")
    public List<Map<String, Object>> getMemberStatistics(@PathVariable Long groupId) {
        List<Map<String, Object>> memberStats = new ArrayList<>();

        // Get all members of the group
        List<GroupMember> members = memberRepo.findByGroupId(groupId);

        for (GroupMember member : members) {
            Long userId = member.getUserId();

            // Get user info
            User user = userRepo.findById(userId.intValue()).orElse(null);
            String userName = user != null ? user.getName() : "Unknown";

            // Count tasks created by this user in this group
            long tasksCreated = taskRepo.countByGroupIdAndCreatedBy(groupId, userId);

            // Count tasks completed (DONE status) by this user in this group
            long tasksCompleted = taskRepo.countByGroupIdAndCreatedByAndStatus(groupId, userId, "DONE");

            Map<String, Object> stat = new HashMap<>();
            stat.put("userId", userId);
            stat.put("userName", userName);
            stat.put("tasksCreated", tasksCreated);
            stat.put("tasksCompleted", tasksCompleted);

            memberStats.add(stat);
        }

        return memberStats;
    }
}

