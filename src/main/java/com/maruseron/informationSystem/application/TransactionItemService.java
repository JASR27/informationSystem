package com.maruseron.informationSystem.application;

import com.maruseron.informationSystem.application.dto.TransactionItemDTO;
import com.maruseron.informationSystem.domain.entity.TransactionItem;
import com.maruseron.informationSystem.domain.value.Either;
import com.maruseron.informationSystem.domain.value.HttpResult;
import com.maruseron.informationSystem.persistence.ProductDetailRepository;
import com.maruseron.informationSystem.persistence.TransactionItemRepository;
import com.maruseron.informationSystem.persistence.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionItemService implements
        CreateService<TransactionItem, TransactionItemDTO.Create, TransactionItemDTO.Read,
                      TransactionItemRepository>
{
    @Autowired
    TransactionItemRepository repository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    ProductDetailRepository productDetailRepository;

    @Override
    public TransactionItemRepository repository() {
        return repository;
    }

    @Override
    public TransactionItem fromDTO(TransactionItemDTO.Create spec) {
        final var productDetail = productDetailRepository
                .findById(spec.productDetailId())
                .orElseThrow();
        final var product = productDetail.getProduct();

        return TransactionItemDTO.createTransactionItem(
                spec,
                // validateForCreation ensures this call is safe
                transactionRepository.findById(spec.transactionId()).orElseThrow(),
                productDetail,
                // check transactionType, extract price from product
                switch (spec.transactionType()) {
                    case SALE, DEVOLUTION -> product.getSellingPrice();
                    case PURCHASE         -> product.getBuyingPrice();
                    case ADJUSTMENT       -> BigDecimal.ZERO;
                                          // ^ no associated value for adjustments
                });
    }

    @Override
    public TransactionItemDTO.Read toDTO(TransactionItem entity) {
        return TransactionItemDTO.fromTransactionItem(entity);
    }

    @Transactional
    public Either<List<TransactionItemDTO.Read>, HttpResult> findAllDetailsFor(int id) {
        if (!transactionRepository.existsById(id))
            return Either.right(new HttpResult(
                    HttpStatus.NOT_FOUND,
                    "La transacción solicitada no existe."));

        try (final var transactionItems = repository.streamAllByTransactionId(id)) {
            final var list = transactionItems.map(this::toDTO).toList();

            return list.isEmpty()
                    ? Either.right(new HttpResult(
                        HttpStatus.NOT_FOUND,
                        "No se ha registrado ninguna variante para el producto indicado."))
                    : Either.left(list);
        }
    }

    @Override
    public Either<TransactionItemDTO.Create, HttpResult> validateForCreation(TransactionItemDTO.Create request) {
        if (transactionRepository.existsById(request.transactionId()))
            return Either.right(new HttpResult(
                    HttpStatus.NOT_FOUND,
                    "La transacción indicada no existe."));

        if (!productDetailRepository.existsById(request.productDetailId()))
            return Either.right(new HttpResult(
                    HttpStatus.NOT_FOUND,
                    "La variante de producto indicada no existe."));

        return Either.left(request);
    }

    @Transactional
    public void bulkCreate(List<TransactionItemDTO.Create> items) {
        // trust this method: it's only ever called if a transaction's validation succeeds
        items.stream().map(this::fromDTO).forEach(repository::save);
    }
}
