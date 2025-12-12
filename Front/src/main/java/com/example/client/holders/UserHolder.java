package com.example.client.holders;

public class UserHolder {
    public static Long userId;
    public static String userName;

    public static boolean isAdmin() {
        return userName != null && userName.equalsIgnoreCase("admin");
    }
}
