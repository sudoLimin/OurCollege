package com.example.client.controllers;

import com.example.client.Main;
import com.example.client.WSClient;
import com.example.client.holders.TaskHolder;
import com.example.client.utils.SceneUtils;
import com.example.client.utils.ValidationUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MaterialsController {

    private static final Logger LOGGER = Logger.getLogger(MaterialsController.class.getName());

    @FXML private ListView<String> materialsList;

    private List<MaterialItem> materials = new ArrayList<>();

    private WSClient ws;

    private static class MaterialItem {
        Long id;
        String title;
        String type;
        String url;
        String filePath;

        MaterialItem(Long id, String title, String type, String url, String filePath) {
            this.id = id;
            this.title = title;
            this.type = type;
            this.url = url;
            this.filePath = filePath;
        }
    }

    @FXML
    public void initialize() {
        LOGGER.log(Level.INFO, "MaterialsController initialized for groupId: {0}", TaskHolder.groupId);

        materialsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                int selectedIndex = materialsList.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0 && selectedIndex < materials.size()) {
                    openMaterialOptions(materials.get(selectedIndex));
                }
            }
        });

        loadMaterials();

        try {
            ws = new WSClient(() -> Platform.runLater(this::loadMaterials));
            ws.onType("material_new", () -> Platform.runLater(this::loadMaterials));
            ws.connect();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not connect WebSocket", e);
        }
    }

    private void loadMaterials() {
        if (TaskHolder.groupId == null) {
            LOGGER.log(Level.WARNING, "Cannot load materials: groupId is null");
            return;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/materials/group/" + TaskHolder.groupId))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();

            LOGGER.log(Level.INFO, "Materials response status: {0}", resp.statusCode());
            LOGGER.log(Level.INFO, "Materials response body: {0}", body);

            materialsList.getItems().clear();
            materials.clear();

            if (body.startsWith("[") && body.length() > 2) {
                String[] parts = body.split("\\{");
                for (String part : parts) {
                    if (part.contains("\"id\":")) {
                        try {
                            String idStr = part.split("\"id\":")[1].split(",")[0].trim();
                            Long id = Long.parseLong(idStr);

                            String title = "";
                            if (part.contains("\"title\":\"")) {
                                title = part.split("\"title\":\"")[1].split("\"")[0];
                            }

                            String url = null;
                            if (part.contains("\"url\":\"") && !part.contains("\"url\":null")) {
                                url = part.split("\"url\":\"")[1].split("\"")[0];
                            }

                            String filePath = null;
                            if (part.contains("\"filePath\":\"") && !part.contains("\"filePath\":null")) {
                                filePath = part.split("\"filePath\":\"")[1].split("\"")[0];
                            } else if (part.contains("\"file_path\":\"") && !part.contains("\"file_path\":null")) {
                                filePath = part.split("\"file_path\":\"")[1].split("\"")[0];
                            }

                            String type;
                            if (url != null && !url.isEmpty()) {
                                type = "LINK";
                            } else if (filePath != null && !filePath.isEmpty()) {
                                type = "FILE";
                            } else {
                                if (part.contains("\"type\":\"")) {
                                    type = part.split("\"type\":\"")[1].split("\"")[0];
                                } else {
                                    type = "UNKNOWN";
                                }
                            }

                            LOGGER.log(Level.INFO, "Parsed material: id={0}, title={1}, type={2}, url={3}, filePath={4}",
                                    new Object[]{id, title, type, url, filePath});

                            String displayText;
                            if ("LINK".equalsIgnoreCase(type)) {
                                displayText = "🔗 " + title + " — link";
                            } else if ("FILE".equalsIgnoreCase(type)) {
                                displayText = "📄 " + title + " — file";
                            } else {
                                displayText = title;
                            }

                            materials.add(new MaterialItem(id, title, type, url, filePath));
                            materialsList.getItems().add(displayText);

                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error parsing material part", e);
                        }
                    }
                }
            }

            if (materialsList.getItems().isEmpty()) {
                materialsList.getItems().add("No materials yet. Add a link or upload a file!");
            }

            LOGGER.log(Level.INFO, "Loaded {0} materials", materials.size());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading materials", e);
            materialsList.getItems().add("Error loading materials. Check console.");
        }
    }

    private void openMaterialOptions(MaterialItem material) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Material Options");
        alert.setHeaderText(material.title);
        SceneUtils.styleDialog(alert);

        ButtonType deleteButton = new ButtonType("Delete");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        if ("LINK".equalsIgnoreCase(material.type)) {
            ButtonType openButton = new ButtonType("Open Link");
            alert.setContentText("URL: " + material.url);
            alert.getButtonTypes().setAll(openButton, deleteButton, cancelButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == openButton) {
                    openLink(material.url);
                } else if (result.get() == deleteButton) {
                    deleteMaterial(material.id);
                }
            }
        } else {
            ButtonType downloadButton = new ButtonType("Download File");
            alert.setContentText("What would you like to do with this file?");
            alert.getButtonTypes().setAll(downloadButton, deleteButton, cancelButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == downloadButton) {
                    downloadFile(material);
                } else if (result.get() == deleteButton) {
                    deleteMaterial(material.id);
                }
            }
        }
    }

    private void openLink(String url) {
        try {
            if (url != null && !url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://" + url;
                }
                Desktop.getDesktop().browse(new URI(url));
                LOGGER.log(Level.INFO, "Opened link: {0}", url);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error opening link", e);
            showAlert("Error", "Could not open link: " + e.getMessage());
        }
    }

    private void downloadFile(MaterialItem material) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String downloadUrl = "http://localhost:8080/materials/download/" + material.id;
            LOGGER.log(Level.INFO, "Downloading from: {0}", downloadUrl);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .build();

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");
            fileChooser.setInitialFileName(material.title);

            Stage stage = (Stage) materialsList.getScene().getWindow();
            File saveFile = fileChooser.showSaveDialog(stage);

            if (saveFile != null) {
                HttpResponse<Path> resp = client.send(req, HttpResponse.BodyHandlers.ofFile(saveFile.toPath()));
                LOGGER.log(Level.INFO, "Download response status: {0}", resp.statusCode());

                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    showAlert("Success", "File downloaded to: " + saveFile.getAbsolutePath());
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(saveFile);
                    }
                } else {
                    showAlert("Error", "Failed to download file. Status: " + resp.statusCode());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error downloading file", e);
            showAlert("Error", "Could not download file: " + e.getMessage());
        }
    }

    private void deleteMaterial(Long id) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Material");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("This will permanently delete this material.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/materials/" + id))
                        .DELETE()
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                LOGGER.log(Level.INFO, "Delete material response: {0}", resp.statusCode());

                loadMaterials();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error deleting material", e);
                showAlert("Error", "Could not delete material: " + e.getMessage());
            }
        }
    }

    @FXML
    public void addLink() {
        if (TaskHolder.groupId == null) {
            ValidationUtils.showServerError("No group selected");
            return;
        }

        TextInputDialog titleDialog = new TextInputDialog();
        titleDialog.setTitle("Add Link");
        titleDialog.setHeaderText("Enter link title:");
        titleDialog.setContentText("Title:");
        SceneUtils.styleDialog(titleDialog);

        Optional<String> titleResult = titleDialog.showAndWait();
        if (titleResult.isEmpty() || titleResult.get().trim().isEmpty()) {
            return;
        }
        String title = titleResult.get().trim();

        if (!ValidationUtils.isValidName(title)) {
            ValidationUtils.showValidationError("Invalid Title", "Link title cannot be empty.");
            return;
        }

        TextInputDialog urlDialog = new TextInputDialog("https://");
        urlDialog.setTitle("Add Link");
        urlDialog.setHeaderText("Enter URL:");
        urlDialog.setContentText("URL:");
        SceneUtils.styleDialog(urlDialog);

        Optional<String> urlResult = urlDialog.showAndWait();
        if (urlResult.isEmpty() || urlResult.get().trim().isEmpty()) {
            return;
        }
        String url = urlResult.get().trim();

        if (!ValidationUtils.isValidName(url)) {
            ValidationUtils.showValidationError("Invalid URL", "URL cannot be empty.");
            return;
        }

        String sanitizedTitle = ValidationUtils.sanitize(title);
        String sanitizedUrl = ValidationUtils.sanitizeForDisplay(url);

        try {
            HttpClient client = HttpClient.newHttpClient();
            String json = "{ \"title\": \"" + sanitizedTitle + "\", \"url\": \"" + sanitizedUrl + "\", \"groupId\": " + TaskHolder.groupId + " }";

            LOGGER.log(Level.INFO, "Adding link with JSON: {0}", json);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/materials/link"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            LOGGER.log(Level.INFO, "Add link response: {0} - {1}", new Object[]{resp.statusCode(), resp.body()});

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                showAlert("Success", "Link added successfully!");
                loadMaterials();
            } else {
                ValidationUtils.showServerError("Failed to add link. Status: " + resp.statusCode());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding link", e);
            ValidationUtils.showServerError("Could not add link: " + e.getMessage());
        }
    }

    @FXML
    public void uploadFile() {
        if (TaskHolder.groupId == null) {
            showAlert("Error", "No group selected");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");

        Stage stage = (Stage) materialsList.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file == null) {
            return;
        }

        TextInputDialog titleDialog = new TextInputDialog(file.getName());
        titleDialog.setTitle("Upload File");
        titleDialog.setHeaderText("Enter file title (or keep filename):");
        titleDialog.setContentText("Title:");

        Optional<String> titleResult = titleDialog.showAndWait();
        String title = titleResult.isPresent() && !titleResult.get().trim().isEmpty()
                ? titleResult.get().trim()
                : file.getName();

        try {
            String boundary = "----FormBoundary" + System.currentTimeMillis();
            HttpClient client = HttpClient.newHttpClient();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            baos.write(("--" + boundary + "\r\n").getBytes());
            baos.write("Content-Disposition: form-data; name=\"groupId\"\r\n\r\n".getBytes());
            baos.write((TaskHolder.groupId.toString() + "\r\n").getBytes());

            baos.write(("--" + boundary + "\r\n").getBytes());
            baos.write("Content-Disposition: form-data; name=\"title\"\r\n\r\n".getBytes());
            baos.write((title + "\r\n").getBytes());

            baos.write(("--" + boundary + "\r\n").getBytes());
            baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) contentType = "application/octet-stream";
            baos.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes());
            baos.write(Files.readAllBytes(file.toPath()));
            baos.write("\r\n".getBytes());

            baos.write(("--" + boundary + "--\r\n").getBytes());

            byte[] requestBody = baos.toByteArray();
            LOGGER.log(Level.INFO, "Uploading file: {0}, size: {1} bytes", new Object[]{file.getName(), requestBody.length});

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/materials/upload"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            LOGGER.log(Level.INFO, "Upload response: {0} - {1}", new Object[]{resp.statusCode(), resp.body()});

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                showAlert("Success", "File uploaded successfully!");
                loadMaterials();
            } else {
                showAlert("Error", "Upload failed.\nStatus: " + resp.statusCode() + "\nResponse: " + resp.body());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error uploading file", e);
            showAlert("Error", "Could not upload file: " + e.getMessage());
        }
    }

    @FXML
    public void back() {
        try {
            Stage stage = (Stage) materialsList.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("groups.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Groups");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating back to groups", e);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

