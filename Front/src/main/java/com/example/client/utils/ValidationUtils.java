package com.example.client.utils;

import javafx.scene.control.Alert;

public class ValidationUtils {

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        if (email.trim().equalsIgnoreCase("admin")) {
            return true;
        }
        return email.contains("@") && email.indexOf("@") > 0 && email.indexOf("@") < email.length() - 1;
    }

    public static boolean isAdminUser(String username) {
        return username != null && username.trim().equalsIgnoreCase("admin");
    }

    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        return password.length() >= 6;
    }

    public static boolean isValidName(String name) {
        if (name == null) {
            return false;
        }
        return !name.trim().isEmpty();
    }

    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        String sanitized = input.trim();
        sanitized = sanitized.replaceAll("<[^>]*>", "");
        sanitized = sanitized.replace("<", "").replace(">", "");
        sanitized = sanitized.replace("\\", "\\\\").replace("\"", "\\\"");
        return sanitized;
    }

    public static String sanitizeForDisplay(String input) {
        if (input == null) {
            return "";
        }
        String sanitized = input.trim();
        sanitized = sanitized.replaceAll("<[^>]*>", "");
        sanitized = sanitized.replace("<", "").replace(">", "");
        return sanitized;
    }

    public static boolean isValidResponse(String response) {
        return response != null && !response.trim().isEmpty();
    }

    public static boolean responseContainsField(String response, String fieldName) {
        if (!isValidResponse(response)) {
            return false;
        }
        return response.contains("\"" + fieldName + "\"");
    }

    public static boolean isErrorResponse(String response) {
        if (!isValidResponse(response)) {
            return true;
        }
        return response.contains("\"error\"") ||
               response.contains("\"status\":4") ||
               response.contains("\"status\":5");
    }

    public static void showValidationError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText("Validation Error");
        alert.setContentText(message);
        SceneUtils.styleDialog(alert);
        alert.showAndWait();
    }

    public static void showServerError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Server Error");
        alert.setHeaderText("Could not complete the request");
        alert.setContentText(message);
        SceneUtils.styleDialog(alert);
        alert.showAndWait();
    }

    public static void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        SceneUtils.styleDialog(alert);
        alert.showAndWait();
    }
}
