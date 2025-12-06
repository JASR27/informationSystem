package com.maruseron.informationSystem.application;

import com.maruseron.informationSystem.application.dto.AdjustmentDTO;
import com.maruseron.informationSystem.application.dto.DevolutionDTO;
import com.maruseron.informationSystem.application.dto.TransactionItemDTO;
import com.maruseron.informationSystem.domain.entity.*;
import com.maruseron.informationSystem.domain.value.Either;
import com.maruseron.informationSystem.domain.value.HttpResult;
import com.maruseron.informationSystem.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Service
public class AdjustmentService implements
        CreateService<Adjustment, AdjustmentDTO.Create, AdjustmentDTO.Read, AdjustmentRepository>
{
    @Autowired
    AdjustmentRepository repository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    ProductDetailRepository productDetailRepository;

    @Override
    public AdjustmentRepository repository() {
        return repository;
    }

    @Override
    public Adjustment fromDTO(AdjustmentDTO.Create spec) {
        return AdjustmentDTO.createAdjustment(
                // validateForCreation ensures this call is safe
                employeeRepository.findById(spec.employeeId()).orElseThrow(),
                null,
                spec.reason());
    }

    @Override
    public AdjustmentDTO.Read toDTO(Adjustment entity) {
        return AdjustmentDTO.fromAdjustment(entity);
    }

    @Override
    public Either<AdjustmentDTO.Create, HttpResult> validateForCreation(AdjustmentDTO.Create request) {
        if (!employeeRepository.existsById(request.employeeId()))
            return Either.right(new HttpResult(
                    HttpStatus.NOT_FOUND,
                    "El empleado indicado no existe."));

        if (request.items().isEmpty())
            return Either.right(new HttpResult(
                    HttpStatus.CONFLICT,
                    "La lista de artículos está vacía."));

        if (request.reason().isBlank())
            return Either.right(new HttpResult(
                    HttpStatus.CONFLICT,
                    "No se ha indicado una razón para el ajuste de inventario."));

        final var someDetailInvalid = request
                .items()
                .stream()
                .map(TransactionItemDTO.Create::productDetailId)
                .anyMatch(not(productDetailRepository::existsById));

        if (someDetailInvalid)
            return Either.right(new HttpResult(
                    HttpStatus.CONFLICT,
                    "Uno o más de los productos indicados no existen."));

        return Either.left(request);
    }
}