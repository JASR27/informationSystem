package com.maruseron.informationSystem.application;

import com.maruseron.informationSystem.application.dto.PaymentDTO;
import com.maruseron.informationSystem.application.dto.SaleDTO;
import com.maruseron.informationSystem.application.dto.TransactionItemDTO;
import com.maruseron.informationSystem.domain.entity.Product;
import com.maruseron.informationSystem.domain.entity.ProductDetail;
import com.maruseron.informationSystem.domain.entity.Sale;
import com.maruseron.informationSystem.domain.value.Either;
import com.maruseron.informationSystem.domain.value.HttpResult;
import com.maruseron.informationSystem.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

import static java.util.function.Predicate.not;

@Service
public class SaleService
        implements CreateService<Sale, SaleDTO.Create, SaleDTO.Read, SaleRepository>
{
    @Autowired
    SaleRepository repository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    ClientRepository clientRepository;

    @Autowired
    ProductDetailRepository productDetailRepository;

    @Autowired
    CurrencyConversionService currencyConversionService;

    @Override
    public SaleRepository repository() {
        return repository;
    }

    @Override
    public Sale fromDTO(SaleDTO.Create spec) {
        // items and payments are sent null because they are not yet saved to the database
        // the sale must be saved first, then transaction items and payments must be updated
        // to point to the sale
        return SaleDTO.createSale(
                spec,
                // validateForCreation ensures this call is safe
                employeeRepository.findById(spec.employeeId()).orElseThrow(),
                null,
                null,
                // validateForCreation ensures this call is safe
                clientRepository.findById(spec.clientId()).orElseThrow());
    }

    @Override
    public SaleDTO.Read toDTO(Sale entity) {
        return SaleDTO.fromSale(entity);
    }

    @Override
    public Either<SaleDTO.Create, HttpResult> validateForCreation(SaleDTO.Create request) {
        if (!employeeRepository.existsById(request.employeeId()))
            return Either.right(new HttpResult(
                    HttpStatus.NOT_FOUND,
                    "El empleado indicado no existe."));

        if (!clientRepository.existsById(request.clientId()))
            return Either.right(new HttpResult(
                    HttpStatus.NOT_FOUND,
                    "El cliente indicado no existe."));

        if (request.items().isEmpty())
            return Either.right(new HttpResult(
                    HttpStatus.CONFLICT,
                    "La lista de artículos está vacía."));

        if (request.payments().isEmpty())
            return Either.right(new HttpResult(
                    HttpStatus.CONFLICT,
                    "La lista de pagos está vacía."));

        final var someProductIsInvalid = request
                .items()
                .stream()
                .map(TransactionItemDTO.Create::productDetailId)
                .anyMatch(not(productDetailRepository::existsById));

        if (someProductIsInvalid)
            return Either.right(new HttpResult(
                    HttpStatus.CONFLICT,
                    "Uno o más de los productos indicados no existen."));

        // prices in database are in USD
        final var totalToPay = request
                .items()
                .stream()
                .map(TransactionItemDTO.Create::productDetailId)
                .map(productDetailRepository::findById)
                .map(Optional::orElseThrow)
                .map(ProductDetail::getProduct)
                .map(Product::getSellingPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // amounts in payments are in VED, must be converted to USD
        final var totalPaid = request
                .payments()
                .stream()
                .map(PaymentDTO.Create::amount)
                .map(BigDecimal::new)
                .map(currencyConversionService::vedToUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaid.compareTo(totalToPay) < 0)
            return Either.right(new HttpResult(
                    HttpStatus.CONFLICT,
                    "La suma total de los pagos no satisface el monto a pagar."));

        return Either.left(request);
    }
}
