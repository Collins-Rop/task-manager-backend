package com.taskmanager.service;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.User;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    public List<TaskResponse> getAllTasks(String username) {
        logger.info("Fetching all tasks for user: {}", username);

        User user = getUserByUsername(username);
        List<Task> tasks = taskRepository.findByUserOrderByCreatedAtDesc(user);

        return tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public TaskResponse getTaskById(Long taskId, String username) {
        logger.info("Fetching task with ID: {} for user: {}", taskId, username);

        User user = getUserByUsername(username);
        Task task = taskRepository.findByIdAndUser(taskId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        return convertToResponse(task);
    }

    public TaskResponse createTask(TaskRequest request, String username) {
        logger.info("Creating new task for user: {}", username);

        User user = getUserByUsername(username);

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() != null ? request.getStatus() :
                com.taskmanager.entity.TaskStatus.PENDING);
        task.setUser(user);

        Task savedTask = taskRepository.save(task);

        logger.info("Task created successfully with ID: {}", savedTask.getId());

        return convertToResponse(savedTask);
    }

    public TaskResponse updateTask(Long taskId, TaskRequest request, String username) {
        logger.info("Updating task with ID: {} for user: {}", taskId, username);

        User user = getUserByUsername(username);
        Task task = taskRepository.findByIdAndUser(taskId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }

        Task updatedTask = taskRepository.save(task);

        logger.info("Task updated successfully with ID: {}", updatedTask.getId());

        return convertToResponse(updatedTask);
    }

    public void deleteTask(Long taskId, String username) {
        logger.info("Deleting task with ID: {} for user: {}", taskId, username);

        User user = getUserByUsername(username);
        Task task = taskRepository.findByIdAndUser(taskId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        taskRepository.delete(task);

        logger.info("Task deleted successfully with ID: {}", taskId);
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private TaskResponse convertToResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}