package com.maruseron.informationSystem.presentation;

import com.maruseron.informationSystem.application.*;
import com.maruseron.informationSystem.application.dto.AdjustmentDTO;
import com.maruseron.informationSystem.application.dto.DevolutionDTO;
import com.maruseron.informationSystem.application.dto.TransactionItemDTO;
import com.maruseron.informationSystem.domain.entity.Adjustment;
import com.maruseron.informationSystem.domain.enumeration.TransactionType;
import com.maruseron.informationSystem.persistence.AdjustmentRepository;
import com.maruseron.informationSystem.util.Controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("adjustment")
public class AdjustmentController implements
        CreateController<Adjustment, AdjustmentDTO.Create, AdjustmentDTO.Read,
                AdjustmentRepository, AdjustmentService>
{
    @Autowired
    AdjustmentService service;

    @Autowired
    TransactionItemService transactionItemService;

    @Autowired
    ProductDetailService productDetailService;

    @Override
    public String endpoint() {
        return "adjustment";
    }

    @Override
    public AdjustmentService service() {
        return service;
    }

    @Override
    @PostMapping
    public ResponseEntity<?> create(@RequestBody AdjustmentDTO.Create request) throws URISyntaxException {
        return Controllers.handleResult(
                service().create(request).flatMap(read -> {
                    transactionItemService.bulkCreate(
                            TransactionItemDTO.completeCreateSpecs(
                                    request.items(),
                                    read.id(),
                                    TransactionType.ADJUSTMENT));
                    // since some items will be negative, this works regardless
                    // by doing algebraic sum
                    productDetailService.increaseStockFor(request.items());
                    return service.findById(read.id());
                }),
                read -> ResponseEntity.created(
                        new URI("/" + endpoint() + "/" + read.id())).body(read));
    }
}