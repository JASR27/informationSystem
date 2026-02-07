package com.maruseron.informationSystem.application;

import com.maruseron.informationSystem.application.dto.DevolutionDTO;
import com.maruseron.informationSystem.application.dto.TransactionItemDTO;
import com.maruseron.informationSystem.domain.entity.Devolution;
import com.maruseron.informationSystem.domain.entity.ProductDetail;
import com.maruseron.informationSystem.domain.entity.Sale;
import com.maruseron.informationSystem.domain.entity.TransactionItem;
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
public class DevolutionService implements
                CreateService<Devolution, DevolutionDTO.Create, DevolutionDTO.Read, DevolutionRepository> {
        @Autowired
        DevolutionRepository repository;
        @Autowired
        EmployeeRepository employeeRepository;
        @Autowired
        ClientRepository clientRepository;
        @Autowired
        SaleRepository saleRepository;
        @Autowired
        ProductDetailRepository productDetailRepository;

        @Override
        public DevolutionRepository repository() {
                return repository;
        }

        @Override
        public Devolution fromDTO(DevolutionDTO.Create spec) {
                return DevolutionDTO.createDevolution(
                                employeeRepository.findById(spec.employeeId()).orElseThrow(),
                                null, // Los items suelen procesarse después o mediante la lógica de persistencia
                                clientRepository.findById(spec.clientId()).orElseThrow(),
                                saleRepository.findById(spec.saleId()).orElseThrow());
        }

        @Override
        public DevolutionDTO.Read toDTO(Devolution entity) {
                return DevolutionDTO.fromDevolution(entity);
        }

        @Override
        public Either<DevolutionDTO.Create, HttpResult> validateForCreation(DevolutionDTO.Create request) {
                // 1. Validar existencia básica
                if (!employeeRepository.existsById(request.employeeId()))
                        return Either.right(new HttpResult(HttpStatus.NOT_FOUND, "El empleado indicado no existe."));

                if (!clientRepository.existsById(request.clientId()))
                        return Either.right(new HttpResult(HttpStatus.NOT_FOUND, "El cliente indicado no existe."));

                // 2. Validar Venta y pertenencia al cliente
                var saleOpt = saleRepository.findById(request.saleId());
                if (saleOpt.isEmpty())
                        return Either.right(new HttpResult(HttpStatus.NOT_FOUND, "La venta indicada no existe."));

                Sale sale = saleOpt.get();
                if (sale.getClient().getId() != request.clientId())
                        return Either.right(new HttpResult(HttpStatus.CONFLICT,
                                        "El cliente no coincide con el dueño de la venta original."));

                // 3. Validar items
                if (request.items() == null || request.items().isEmpty())
                        return Either.right(new HttpResult(HttpStatus.CONFLICT, "La lista de artículos está vacía."));

                final var productDetailsInSale = sale.getItems().stream()
                                .collect(Collectors.toMap(item -> item.getProductDetail().getId(),
                                                TransactionItem::getQuantity));

                for (var itemRequest : request.items()) {
                        if (!productDetailRepository.existsById(itemRequest.productDetailId()))
                                return Either.right(new HttpResult(HttpStatus.NOT_FOUND,
                                                "Producto ID " + itemRequest.productDetailId() + " no existe."));

                        Integer quantityInSale = productDetailsInSale.get(itemRequest.productDetailId());
                        if (quantityInSale == null)
                                return Either.right(new HttpResult(HttpStatus.CONFLICT, "El producto "
                                                + itemRequest.productDetailId() + " no pertenece a la venta."));

                        if (itemRequest.quantity() > quantityInSale)
                                return Either.right(new HttpResult(HttpStatus.CONFLICT,
                                                "Cantidad a devolver excede la compra original para el producto "
                                                                + itemRequest.productDetailId()));
                }

                return Either.left(request);
        }
}