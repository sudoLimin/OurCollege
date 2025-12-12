package com.example.demo.controller;

import com.example.demo.model.Group;
import com.example.demo.model.GroupMember;
import com.example.demo.model.User;
import com.example.demo.repository.GroupRepository;
import com.example.demo.repository.GroupMemberRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.websocket.NotificationService;
import com.example.demo.util.InputSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/groups")
@CrossOrigin
public class GroupController {

    private final GroupRepository repo;
    private final GroupMemberRepository memberRepo;
    private final UserRepository userRepo;
    private final NotificationService notifier;

    public GroupController(GroupRepository repo, GroupMemberRepository memberRepo, UserRepository userRepo, NotificationService notifier) {
        this.repo = repo;
        this.memberRepo = memberRepo;
        this.userRepo = userRepo;
        this.notifier = notifier;
    }

    @PostMapping
    public Group create(@RequestBody Group g) {
        g.setName(InputSanitizer.sanitize(g.getName()));
        return repo.save(g);
    }

    @GetMapping
    public List<Group> all() {
        return repo.findAll();
    }

    @PostMapping("/{groupId}/add-member")
    public ResponseEntity<String> addMember(
            @PathVariable Long groupId,
            @RequestParam(required = false) String email,
            HttpServletRequest request) {

        String memberEmail = null;

        if (email != null && !email.isBlank()) {
            memberEmail = email;
        } else {
            try {
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = request.getReader();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String body = sb.toString();
                if (body != null && !body.isBlank()) {
                    ObjectMapper om = new ObjectMapper();
                    Map<String, Object> map = om.readValue(body, Map.class);
                    if (map.get("email") != null) {
                        memberEmail = map.get("email").toString();
                    }
                }
            } catch (Exception ex) {
                System.out.println("Could not read JSON body for add-member: " + ex.getMessage());
            }
        }

        System.out.println("Adding member with email: " + memberEmail + " to group: " + groupId);

        if (memberEmail == null || memberEmail.isBlank()) {
            System.out.println("Email is missing");
            return ResponseEntity.badRequest().body("EMAIL_REQUIRED");
        }

        memberEmail = InputSanitizer.sanitizeEmail(memberEmail);

        User user = userRepo.findByEmail(memberEmail);
        if (user == null) {
            System.out.println("User not found with email: " + memberEmail);
            return ResponseEntity.status(404).body("USER_NOT_FOUND");
        }

        Long userId = user.getId().longValue();
        System.out.println("Found user: " + user.getName() + " with id: " + userId);

        if (memberRepo.existsByGroupIdAndUserId(groupId, userId)) {
            System.out.println("User already exists in group");
            return ResponseEntity.status(409).body("ALREADY_EXISTS");
        }

        try {
            GroupMember member = new GroupMember();
            member.setGroupId(groupId);
            member.setUserId(userId);
            GroupMember saved = memberRepo.save(member);
            System.out.println("Saved GroupMember with id: " + saved.getId());

            notifier.notifyMemberNewForGroup(groupId, user.getName());

            return ResponseEntity.ok("OK");
        } catch (Exception ex) {
            System.err.println("Error saving GroupMember: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.status(500).body("ERROR_SAVING_MEMBER");
        }
    }

    @GetMapping("/{groupId}/members")
    public List<User> getMembers(@PathVariable Long groupId) {
        List<GroupMember> members = memberRepo.findByGroupId(groupId);

        return members.stream()
                .map(m -> userRepo.findById(m.getUserId().intValue()).orElse(null))
                .filter(u -> u != null)
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> renameGroup(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newName = InputSanitizer.sanitize(body.get("name"));

        if (newName == null || newName.isBlank()) {
            return ResponseEntity.badRequest().body("NAME_REQUIRED");
        }

        return repo.findById(id)
                .map(group -> {
                    group.setName(newName);
                    Group saved = repo.save(group);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.status(404).body(null));
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<?> removeMember(@PathVariable Long groupId, @PathVariable Long memberId) {
        System.out.println("Removing member with id: " + memberId + " from group: " + groupId);

        GroupMember member = memberRepo.findByGroupIdAndUserId(groupId, memberId);
        if (member == null) {
            System.out.println("Member not found in group");
            return ResponseEntity.status(404).body("MEMBER_NOT_FOUND");
        }

        try {
            memberRepo.delete(member);
            System.out.println("Member removed successfully");
            return ResponseEntity.ok("OK");
        } catch (Exception ex) {
            System.err.println("Error removing member: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.status(500).body("ERROR_REMOVING_MEMBER");
        }
    }

    @DeleteMapping("/{groupId}/remove-member")
    @Transactional
    public ResponseEntity<?> removeMemberByQuery(@PathVariable Long groupId, @RequestParam("userId") Long userId) {
        System.out.println("Removing member (via query param) with userId: " + userId + " from group: " + groupId);

        try {
            GroupMember member = memberRepo.findByGroupIdAndUserId(groupId, userId);
            if (member == null) {
                System.out.println("Member not found in group");
                return ResponseEntity.status(404).body("MEMBER_NOT_FOUND");
            }

            memberRepo.delete(member);
            memberRepo.flush();
            System.out.println("Member removed successfully");
            return ResponseEntity.ok("OK");
        } catch (Exception ex) {
            System.err.println("Error removing member: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.status(500).body("ERROR_REMOVING_MEMBER");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGroup(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.status(404).body("GROUP_NOT_FOUND");
        }

        List<GroupMember> members = memberRepo.findByGroupId(id);
        memberRepo.deleteAll(members);

        repo.deleteById(id);

        return ResponseEntity.ok("OK");
    }
}
