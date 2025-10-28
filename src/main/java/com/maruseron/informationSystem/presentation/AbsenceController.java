package com.maruseron.informationSystem.presentation;

import com.maruseron.informationSystem.domain.Absence;
import com.maruseron.informationSystem.persistence.AbsenceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("absence")
public class AbsenceController {
    private final AbsenceRepository absenceRepository;

    public AbsenceController(final AbsenceRepository absenceRepository) {
        this.absenceRepository = absenceRepository;
    }

    @GetMapping
    public List<Absence> getAbsences() {
        return absenceRepository.findAll();
    }

    @GetMapping("/{id}")
    public Absence getAbsence(@PathVariable Integer id) {
        return absenceRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    @PostMapping
    public ResponseEntity<Absence> createAbsence(@RequestBody Absence request)
            throws URISyntaxException {
        final var absence = absenceRepository.save(request);
        // absence.setCreatedAt(Instant.now());
        return ResponseEntity.created(
                new URI("/absence/" + absence.getId())).body(absence);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Absence> updateAbsence(@PathVariable Integer id,
                                                 @RequestBody Absence request) {
        var absence =
                absenceRepository.findById(id).orElseThrow(RuntimeException::new);
        absence.setAuthorizer(request.getAuthorizer());
        absence.setEmployee(request.getEmployee());
        absence.setStartTime(request.getStartTime());
        absence.setDuration(request.getDuration());
        absence.setReason(request.getReason());
        absence.setPermissionStatus(request.getPermissionStatus());
        absence = absenceRepository.save(request);

        return ResponseEntity.ok(absence);
    }
}