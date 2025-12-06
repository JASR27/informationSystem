package com.maruseron.informationSystem.presentation;

import com.maruseron.informationSystem.application.PaymentService;
import com.maruseron.informationSystem.application.ProductDetailService;
import com.maruseron.informationSystem.application.SaleService;
import com.maruseron.informationSystem.application.TransactionItemService;
import com.maruseron.informationSystem.application.dto.PaymentDTO;
import com.maruseron.informationSystem.application.dto.SaleDTO;
import com.maruseron.informationSystem.application.dto.TransactionItemDTO;
import com.maruseron.informationSystem.domain.entity.Sale;
import com.maruseron.informationSystem.domain.enumeration.TransactionType;
import com.maruseron.informationSystem.persistence.SaleRepository;
import com.maruseron.informationSystem.util.Controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("sale")
public class SaleController
    implements CreateController<Sale, SaleDTO.Create, SaleDTO.Read, SaleRepository, SaleService>
{
    @Autowired
    SaleService service;

    @Autowired
    TransactionItemService transactionItemService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    ProductDetailService productDetailService;

    @Override
    public String endpoint() {
        return "sale";
    }

    @Override
    public SaleService service() {
        return service;
    }

    @Override
    @PostMapping
    public ResponseEntity<?> create(@RequestBody SaleDTO.Create request) throws URISyntaxException {
        return Controllers.handleResult(
                service().create(request).flatMap(read -> {
                    transactionItemService.bulkCreate(
                            TransactionItemDTO.completeCreateSpecs(
                                    request.items(),
                                    read.id(),
                                    TransactionType.SALE));
                    paymentService.bulkCreate(
                            PaymentDTO.completeCreateSpecs(
                                    request.payments(),
                                    read.id()));
                    productDetailService.reduceStockFor(request.items());
                    return service.findById(read.id());
                }),
                read -> ResponseEntity.created(
                        new URI("/" + endpoint() + "/" + read.id())).body(read));
    }
}