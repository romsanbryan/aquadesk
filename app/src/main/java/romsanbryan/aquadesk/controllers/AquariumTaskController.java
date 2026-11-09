package romsanbryan.aquadesk.controllers;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import romsanbryan.aquadesk.dto.request.AquariumTaskRequest;
import romsanbryan.aquadesk.dto.request.TaskCompletionRequest;
import romsanbryan.aquadesk.dto.response.AquariumTaskResponse;
import romsanbryan.aquadesk.services.AquariumTaskService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/aquariums/{aquariumId}/tasks")
public class AquariumTaskController {
    private final AquariumTaskService taskService;

    public AquariumTaskController(AquariumTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<?> getTasks(@PathVariable Integer aquariumId) {
        List<AquariumTaskResponse> tasks = taskService.getTasks(aquariumId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/calendar")
    public ResponseEntity<?> getCalendarTasks(
            @PathVariable Integer aquariumId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(taskService.getCalendarTasks(aquariumId, start, end));
    }

    @PostMapping
    public ResponseEntity<?> createTask(@PathVariable Integer aquariumId,
                                        @Valid @RequestBody AquariumTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(aquariumId, request));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTask(@PathVariable Integer aquariumId, @PathVariable Integer taskId) {
        return ResponseEntity.ok(taskService.getTask(aquariumId, taskId));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<?> updateTask(@PathVariable Integer aquariumId, @PathVariable Integer taskId,
                                        @Valid @RequestBody AquariumTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(aquariumId, taskId, request));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Integer aquariumId, @PathVariable Integer taskId) {
        taskService.deleteTask(aquariumId, taskId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{taskId}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> completeTask(
            @PathVariable Integer aquariumId,
            @PathVariable Integer taskId,
            @Valid @RequestPart("completion") TaskCompletionRequest request,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        taskService.completeTask(aquariumId, taskId, request, photo);
        return ResponseEntity.noContent().build();
    }
}
