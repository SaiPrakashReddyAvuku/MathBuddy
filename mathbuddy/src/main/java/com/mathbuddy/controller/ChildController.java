package com.mathbuddy.controller;

import com.mathbuddy.dto.ChildProgressResponse;
import com.mathbuddy.dto.ChildRequest;
import com.mathbuddy.dto.ChildResponse;
import com.mathbuddy.dto.RecommendationDto;
import com.mathbuddy.service.ChildService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/children")
public class ChildController {

    private final ChildService childService;

    public ChildController(ChildService childService) {
        this.childService = childService;
    }

    @PostMapping
    public ResponseEntity<ChildResponse> createChild(@Valid @RequestBody ChildRequest request) {
        ChildResponse created = childService.createChild(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ChildResponse>> getAllChildren() {
        return ResponseEntity.ok(childService.getAllChildren());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChildResponse> getChildById(@PathVariable Long id) {
        return ResponseEntity.ok(childService.getChildById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChildResponse> updateChild(@PathVariable Long id, @Valid @RequestBody ChildRequest request) {
        return ResponseEntity.ok(childService.updateChild(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChild(@PathVariable Long id) {
        childService.deleteChild(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<ChildProgressResponse> getChildProgress(@PathVariable Long id) {
        return ResponseEntity.ok(childService.getChildProgress(id));
    }

    @GetMapping("/{id}/recommendations")
    public ResponseEntity<List<RecommendationDto>> getRecommendations(@PathVariable Long id) {
        return ResponseEntity.ok(childService.getRecommendations(id));
    }
}

