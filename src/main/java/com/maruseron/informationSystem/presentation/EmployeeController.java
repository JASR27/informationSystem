package com.maruseron.informationSystem.presentation;

import com.maruseron.informationSystem.domain.Employee;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("employee")
public class EmployeeController {
    private final com.maruseron.informationSystem.persistence.EmployeeRepository employeeRepository;

    public EmployeeController(final com.maruseron.informationSystem.persistence.EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    public List<Employee> getEmployees() {
        return employeeRepository.findAll();
    }

    @GetMapping("/{id}")
    public Employee getEmployees(@PathVariable Integer id) {
        return employeeRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    @PostMapping
    public ResponseEntity<Employee> getEmployees(@RequestBody Employee request)
            throws URISyntaxException {
        final var employee = employeeRepository.save(request);
        // employee.setCreatedAt(Instant.now());
        return ResponseEntity.created(
                new URI("/employee/" + employee.getId())).body(employee);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> update(@PathVariable Integer id,
            @RequestBody Employee request) {
        var employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        employee.setUsername(request.getUsername());
        employee.setPassword(request.getPassword());
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setNid(request.getNid());
        employee.setRole(request.getRole());

        

        employee = employeeRepository.save(employee);

        return ResponseEntity.ok(employee);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteClient(@PathVariable Integer id) {
        employeeRepository.deleteById(id);
        return ResponseEntity.ok().body(id.toString());
    }
}
