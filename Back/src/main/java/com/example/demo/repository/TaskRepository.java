package com.example.demo.repository;

import com.example.demo.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByGroupId(Long groupId);

    long countByGroupId(Long groupId);
    long countByGroupIdAndStatus(Long groupId, String status);

    long countByGroupIdAndCreatedBy(Long groupId, Long createdBy);
    long countByGroupIdAndCreatedByAndStatus(Long groupId, Long createdBy, String status);

    List<Task> findByGroupIdAndCreatedBy(Long groupId, Long createdBy);
}
