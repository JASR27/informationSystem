package com.maruseron.informationSystem.application;

import com.maruseron.informationSystem.application.dto.AuthDTO;
import com.maruseron.informationSystem.domain.entity.Employee;
import com.maruseron.informationSystem.domain.enumeration.Role;
import com.maruseron.informationSystem.application.dto.EmployeeDTO;
import com.maruseron.informationSystem.persistence.EmployeeRepository;
import com.maruseron.informationSystem.domain.value.Either;
import com.maruseron.informationSystem.domain.value.HttpResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService implements
        CreateService<Employee, EmployeeDTO.Create, EmployeeDTO.Read, EmployeeRepository>,
        UpdateService<Employee, EmployeeDTO.Update, EmployeeDTO.Read, EmployeeRepository>
{
    @Autowired
    EmployeeRepository repository;

    @Override
    public EmployeeRepository repository() {
        return repository;
    }

    @Override
    public Employee fromDTO(EmployeeDTO.Create spec) {
        return EmployeeDTO.createEmployee(spec);
    }

    @Override
    public EmployeeDTO.Read toDTO(Employee entity) {
        return EmployeeDTO.fromEmployee(entity);
    }

    @Override
    public Either<EmployeeDTO.Create, HttpResult> validateForCreation(
            final EmployeeDTO.Create request) {
        if (repository.existsByNid(request.nid()))
            return Either.right(new HttpResult(
                    HttpStatus.CONFLICT,
                    "La persona con la identificación provista ya se encuentra registrada."));

        if (repository.existsByUsername(request.username()))
            return Either.right(new HttpResult(
                    HttpStatus.CONFLICT,
                    "Este nombre de usuario ya existe."));

        return Either.left(request);
    }

    @Override
    public Either<Employee, HttpResult> validateAndUpdate(
            final Employee employee, final EmployeeDTO.Update request) {

        // if username is changing and new username already exists
        if (!employee.getUsername().equals(request.username())
            && repository.existsByUsername(request.username())) {
                return Either.right(new HttpResult(
                        HttpStatus.CONFLICT,
                        "Este nombre de usuario ya se encuentra en uso."));
        }

        // if we've reached here, it's safe to change overwrite these fields freely
        employee.setUsername(request.username());
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setRole(Role.valueOf(request.role().toUpperCase()));
        return Either.left(employee);
    }

    public Either<String, HttpResult> auth(final AuthDTO request) {
        if (!repository().existsByUsername(request.username()))
            return Either.right(new HttpResult(
                    HttpStatus.CONFLICT,
                    "Nombre de usuario inexistente."));

        final var employee = repository
                .findByUsername(request.username())
                .orElseThrow();

        if (!employee.getPassword().equals(request.password()))
            return Either.right(new HttpResult(
                    HttpStatus.CONFLICT,
                    "Contraseña incorrecta."));

        return Either.left(Integer.toString(employee.getId()));
    }
}
