package com.example.demo.controller;

import com.example.demo.model.StudyMaterial;
import com.example.demo.repository.StudyMaterialRepository;
import com.example.demo.websocket.NotificationService;
import com.example.demo.util.InputSanitizer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/materials")
@CrossOrigin
public class MaterialController {

    private final StudyMaterialRepository materialRepo;
    private final NotificationService notifier;
    private static final String UPLOAD_DIR = "uploads";

    public MaterialController(StudyMaterialRepository materialRepo, NotificationService notifier) {
        this.materialRepo = materialRepo;
        this.notifier = notifier;
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
        }
    }

    @PostMapping("/link")
    public ResponseEntity<?> uploadLink(@RequestBody StudyMaterial material) {
        material.setTitle(InputSanitizer.sanitize(material.getTitle()));

        if (material.getGroupId() == null) {
            return ResponseEntity.badRequest().body("GROUP_ID_REQUIRED");
        }
        if (material.getUrl() == null || material.getUrl().isBlank()) {
            return ResponseEntity.badRequest().body("URL_REQUIRED");
        }

        material.setFilePath(null);
        material.setCreatedAt(java.time.LocalDateTime.now().toString());

        try {
            StudyMaterial saved = materialRepo.save(material);

            notifier.notifyMaterialNewForGroup(saved.getGroupId(), saved.getTitle());

            return ResponseEntity.ok(saved);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("ERROR_SAVING_MATERIAL");
        }
    }

    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "groupId", required = false) Long groupId,
            @RequestParam(value = "uploadedBy", required = false) Long uploadedBy,
            @RequestParam(value = "title", required = false) String title) {

        // groupId and uploadedBy are optional Longs directly bound from form-data

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("FILE_REQUIRED");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID() + extension;

            String materialTitle = InputSanitizer.sanitize(
                (title != null && !title.isBlank()) ? title : originalFilename
            );

            Path uploadPath = Paths.get(UPLOAD_DIR, uniqueFilename);
            Files.write(uploadPath, file.getBytes());

            StudyMaterial material = new StudyMaterial();
            material.setGroupId(groupId);
            material.setUploadedBy(uploadedBy);
            material.setTitle(materialTitle);
            material.setUrl(null);
            material.setFilePath(uploadPath.toString());
            material.setCreatedAt(java.time.LocalDateTime.now().toString());

            StudyMaterial saved = materialRepo.save(material);

            notifier.notifyMaterialNewForGroup(saved.getGroupId(), saved.getTitle());

            return ResponseEntity.ok(saved);
        } catch (IOException ex) {
            return ResponseEntity.status(500).body("ERROR_SAVING_FILE");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("ERROR_SAVING_MATERIAL");
        }
    }

    @GetMapping("/group/{groupId}")
    public List<StudyMaterial> getMaterialsByGroup(@PathVariable Long groupId) {
        return materialRepo.findByGroupId(groupId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMaterial(@PathVariable Long id) {

        return materialRepo.findById(id)
                .map(material -> {
                    if (material.getFilePath() != null && !material.getFilePath().isBlank()) {
                        try {
                            Path filePath = Paths.get(material.getFilePath());
                            Files.deleteIfExists(filePath);
                        } catch (IOException ex) {
                            System.err.println("Warning: could not delete file: " + ex.getMessage());
                        }
                    }

                    materialRepo.deleteById(id);
                    return ResponseEntity.ok("OK");
                })
                .orElse(ResponseEntity.status(404).body("MATERIAL_NOT_FOUND"));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadFile(@PathVariable Long id) {
        return materialRepo.findById(id)
                .map(material -> {
                    if (material.getFilePath() == null || material.getFilePath().isBlank()) {
                        return ResponseEntity.badRequest().body("NOT_A_FILE");
                    }

                    try {
                        Path filePath = Paths.get(material.getFilePath());
                        if (!Files.exists(filePath)) {
                            return ResponseEntity.status(404).body("FILE_NOT_FOUND");
                        }

                        byte[] fileContent = Files.readAllBytes(filePath);
                        String filename = filePath.getFileName().toString();

                        return ResponseEntity.ok()
                                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                                .header("Content-Type", "application/octet-stream")
                                .body(fileContent);
                    } catch (IOException ex) {
                        return ResponseEntity.status(500).body("ERROR_READING_FILE");
                    }
                })
                .orElse(ResponseEntity.status(404).body(null));
    }
}
