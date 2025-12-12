package com.example.client.holders;

import java.util.ArrayList;
import java.util.List;

public class TaskHolder {

    public static Long groupId;
    public static Long taskId;
    public static Long openTaskId;
    public static Long userId;
    public static String currentTitle;
    public static String currentDescription;
    public static String currentStatus;

    public static List<TaskItem> tasks = new ArrayList<>();

    public static class TaskItem {
        public Long id;
        public String title;

        public TaskItem(Long id, String title) {
            this.id = id;
            this.title = title;
        }
    }
}
