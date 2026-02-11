package com.maruseron.informationSystem.application;

import com.maruseron.informationSystem.application.dto.PurchaseDTO;
import com.maruseron.informationSystem.domain.entity.Purchase;
import com.maruseron.informationSystem.domain.value.Either;
import com.maruseron.informationSystem.domain.value.HttpResult;
import com.maruseron.informationSystem.persistence.EmployeeRepository;
import com.maruseron.informationSystem.persistence.PurchaseRepository;
import com.maruseron.informationSystem.persistence.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PurchaseService implements
        CreateService<Purchase, PurchaseDTO.Create, PurchaseDTO.Read, PurchaseRepository>
{
    @Autowired
    PurchaseRepository repository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    SupplierRepository supplierRepository;

    @Override
    public PurchaseRepository repository() {
        return repository;
    }

    @Override
    public Purchase fromDTO(PurchaseDTO.Create spec) {
        return PurchaseDTO.createPurchase(
                employeeRepository.findById(spec.employeeId()).orElseThrow(),
                null,
                spec.bill(),
                supplierRepository.findById(spec.supplierId()).orElseThrow());
    }

    @Override
    public PurchaseDTO.Read toDTO(Purchase entity) {
        return PurchaseDTO.fromPurchase(entity);
    }

    @Override
    public Either<PurchaseDTO.Create, HttpResult> validateForCreation(PurchaseDTO.Create request) {
        if (!employeeRepository.existsById(request.employeeId()))
            return Either.right(new HttpResult(
                    HttpStatus.NOT_FOUND,
                    "El empleado indicado no existe."));

        if (!supplierRepository.existsById(request.supplierId()))
            return Either.right(new HttpResult(
                    HttpStatus.NOT_FOUND,
                    "El proveedor indicado no existe."));

        return Either.left(request);
    }
}
