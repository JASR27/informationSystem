package com.maruseron.informationSystem.presentation;

import com.maruseron.informationSystem.application.*;
import com.maruseron.informationSystem.application.dto.PurchaseDTO;
import com.maruseron.informationSystem.application.dto.TransactionItemDTO;
import com.maruseron.informationSystem.domain.entity.Purchase;
import com.maruseron.informationSystem.domain.value.Either;
import com.maruseron.informationSystem.domain.value.HttpResult;
import com.maruseron.informationSystem.persistence.PurchaseRepository;
import com.maruseron.informationSystem.util.Controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("purchase")
public class PurchaseController implements
        CreateController<Purchase, PurchaseDTO.Create, PurchaseDTO.Read,
                         PurchaseRepository, PurchaseService>
{
    @Autowired
    PurchaseService service;

    @Autowired
    TransactionItemService transactionItemService;

    @Autowired
    ProductDetailService productDetailService;

    @Override
    public String endpoint() {
        return "devolution";
    }

    @Override
    public PurchaseService service() {
        return service;
    }

    @Override
    @PostMapping
    public ResponseEntity<?> create(@RequestBody PurchaseDTO.Create req) throws URISyntaxException {
        record Zip(
                List<PurchaseDTO.StockDescriptor> descriptors,
                Either<PurchaseDTO.Read, HttpResult> result) {}
        return Controllers.handleResult(
                // purchase pre-validation to avoid cases where the creation of items would be
                // valid even with a faulty purchase, leading to them getting saved to the
                // database with a set stock amount, only for the transaction to get rolled back
                // and the variants to not get removed
                service.validateForCreation(req).flatMap(request ->
                                productDetailService
                                        // the non-existent variants are created with the stock
                                        // already set
                                        .createNonExistent(request.items())
                                        // a list of stock descriptors for each item in the
                                        // transaction are returned. these stock descriptors
                                        // contain the information necessary to turn them into
                                        // transaction items and to discern whether they will
                                        // update the stock or not after the transaction is created
                                        .map(variants ->
                                                // ugly, but zip joins the variant read DTOs (we
                                                // need the ids) and the result of create together
                                                // so we can keep flat mapping
                                                new Zip(variants,
                                                        service().create(request)))
                                        .flatMap(zip ->
                                                zip.result().flatMap(read -> {
                                                    // can't use purchase read dto items here:
                                                    // object is partial must create the
                                                    // transaction item creation specs manually
                                                    // through makeCreateSpecs, which extracts the
                                                    // variant id and stock for quantity
                                                    transactionItemService.bulkCreate(
                                                            TransactionItemDTO.makeCreateSpecs(
                                                                    zip.descriptors(),
                                                                    read.id()));
                                                    // addPurchasedStock works like
                                                    // increaseStockFor, but will check
                                                    // isUpdating in the stock descriptor before
                                                    // adding
                                                    productDetailService.addPurchasedStock(zip.descriptors());
                                                    return service.findById(read.id());
                                                }))
                                        ),
                read -> ResponseEntity.created(
                        new URI("/" + endpoint() + "/" + read.id())).body(read));
    }
}
