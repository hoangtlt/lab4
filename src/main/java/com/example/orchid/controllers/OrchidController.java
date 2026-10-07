package com.example.orchid.controllers;

import com.example.orchid.entity.Orchid;
import com.example.orchid.services.IOrchidService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orchids")
public class OrchidController {
    private final IOrchidService orchidService;

    public OrchidController(IOrchidService orchidService) {
        this.orchidService = orchidService;
    }

    @GetMapping
    public List<Orchid> getAll(@RequestParam(required = false) String name) {
        if (name != null && !name.isBlank()) {
            return orchidService.searchByName(name);
        }
        return orchidService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Orchid> getById(@PathVariable Long id) {
        Optional<Orchid> orchid = orchidService.getById(id);
        if (orchid.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(orchid.get());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Orchid orchid) {
        try {
            Orchid created = orchidService.create(orchid);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Orchid orchid) {
        try {
            Optional<Orchid> updated = orchidService.update(id, orchid);
            if (updated.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated.get());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!orchidService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
