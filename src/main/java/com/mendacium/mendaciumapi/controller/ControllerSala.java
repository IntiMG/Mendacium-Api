package com.mendacium.mendaciumapi.controller;

import com.mendacium.mendaciumapi.model.Jugador;
import com.mendacium.mendaciumapi.model.Sala;
import com.mendacium.mendaciumapi.service.ServiceSala;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/sala")
@CrossOrigin(origins = "*")
public class ControllerSala {

    private final ServiceSala service;

    public ControllerSala(ServiceSala service) {
        this.service = service;
    }

    @GetMapping("/all")
    public ResponseEntity<List<Sala>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/getId/{id}")
    public ResponseEntity<Sala> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/getCodigo/{codigo}")
    public ResponseEntity<Sala> findByCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(service.findByCodigo(codigo));
    }

    @PostMapping("/crear")
    public ResponseEntity<Sala> crear(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.crearSala(body.get("hostNombre")));
    }

    @PostMapping("/{codigo}/unirse")
    public ResponseEntity<Jugador> unirse(
            @PathVariable String codigo,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.unirseASala(codigo, body.get("nombre")));
    }
}
