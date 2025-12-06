package com.maruseron.informationSystem.application;

import com.maruseron.informationSystem.application.dto.ProductDetailDTO;
import com.maruseron.informationSystem.application.dto.PurchaseDTO;
import com.maruseron.informationSystem.application.dto.TransactionItemDTO;
import com.maruseron.informationSystem.domain.entity.ProductDetail;
import com.maruseron.informationSystem.domain.value.Either;
import com.maruseron.informationSystem.domain.value.HttpResult;
import com.maruseron.informationSystem.persistence.ProductDetailRepository;
import com.maruseron.informationSystem.persistence.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductDetailService implements
    CreateService<ProductDetail, ProductDetailDTO.Create, ProductDetailDTO.Read,
                  ProductDetailRepository>,
    UpdateService<ProductDetail, ProductDetailDTO.Update, ProductDetailDTO.Read,
                  ProductDetailRepository>
{
    @Autowired
    ProductDetailRepository repository;

    @Autowired
    ProductRepository productRepository;

    @Override
    public ProductDetailRepository repository() {
        return repository;
    }

    @Override
    public ProductDetail fromDTO(ProductDetailDTO.Create spec) {
        return ProductDetailDTO.createProductDetail(
                spec,
                // validateForCreation ensures this call is safe
                productRepository.findById(spec.productId()).orElseThrow());
    }

    @Override
    public ProductDetailDTO.Read toDTO(ProductDetail entity) {
        return ProductDetailDTO.fromProductDetail(entity);
    }

    @Transactional
    public Either<List<ProductDetailDTO.Read>, HttpResult> findAllDetailsFor(int id) {
        if (!productRepository.existsById(id))
            return Either.right(new HttpResult(
                    HttpStatus.NOT_FOUND,
                    "El producto solicitado no existe."));

        try (final var productVariants = repository.streamAllByProductId(id)) {
            final var list = productVariants
                    .map(this::toDTO)
                    .toList();

            return list.isEmpty()
                    ? Either.right(new HttpResult(
                    HttpStatus.NOT_FOUND,
                    "No se ha registrado ninguna variante para el producto indicado."))
                    : Either.left(list);
        }
    }

    @Override
    public Either<ProductDetailDTO.Create, HttpResult>
    validateForCreation(ProductDetailDTO.Create request) {
        return productRepository.existsById(request.productId())
                ? Either.left(request)
                : Either.right(new HttpResult(HttpStatus.NOT_FOUND));
    }

    public Either<Integer, HttpResult> validateForCreation(int productId) {
        return productRepository.existsById(productId)
                ? Either.left(productId)
                : Either.right(new HttpResult(HttpStatus.NOT_FOUND));
    }


    @Override
    public Either<ProductDetail, HttpResult> validateAndUpdate(
            ProductDetail entity, ProductDetailDTO.Update request) {
        entity.setStock(request.stock());
        return Either.left(entity);
    }

    public Either<List<PurchaseDTO.StockDescriptor>, HttpResult>
    createNonExistent(List<PurchaseDTO.StockInputDescriptor> items) {
        for (final var item : items) {
            if (repository.existsBySku(item.sku())) continue;

            switch (validateForCreation(item.productId())) {
                case Either.Left<?, HttpResult> _ -> {}
                case Either.Right<?, HttpResult>(HttpResult res) -> {
                    return Either.right(res);
                }
            }
        }

        // split the items between existent and non-existent,
        // -> existent items will be converted directly into a StockDescriptor, indicating they
        //    will update the current stock
        // -> non-existent items will instead be saved before converted into a StockDescriptor,
        //    indicating they do not update the current stock (since the stock is already saved
        //    directly upon save)
        final var list = new ArrayList<PurchaseDTO.StockDescriptor>();
        for (final var item : items) {
            // for an existent item, just convert directly
            if (repository.existsBySku(item.sku())) {
                final var productDetail = repository.findBySku(item.sku()).orElseThrow();
                list.add(new PurchaseDTO.StockDescriptor(
                        productDetail.getId(),
                        item.quantity(),
                        true));
            } else {
                final var productDetail = repository.save(fromDTO(item.toProductDetailSpec()));
                list.add(new PurchaseDTO.StockDescriptor(
                        productDetail.getId(),
                        item.quantity(),
                        false));
            }
        }

        return Either.left(list);
    }

    @Transactional
    public void reduceStockFor(List<TransactionItemDTO.Create> items) {
        for (final var item : items) {
            final var productDetail = repository.findById(item.productDetailId()).orElseThrow();
            productDetail.setStock(productDetail.getStock() - item.quantity());
            repository.save(productDetail);
        }
    }

    @Transactional
    public void increaseStockFor(List<TransactionItemDTO.Create> items) {
        for (final var item : items) {
            final var productDetail = repository.findById(item.productDetailId()).orElseThrow();
            productDetail.setStock(productDetail.getStock() + item.quantity());
            repository.save(productDetail);
        }
    }

    @Transactional
    public void addPurchasedStock(List<PurchaseDTO.StockDescriptor> items) {
        for (final var item : items) {
            final var productDetail = repository.findById(item.productDetailId()).orElseThrow();
            productDetail.setStock(
                    item.isUpdating()
                            ? productDetail.getStock() + item.quantity()
                            : productDetail.getStock());
            repository.save(productDetail);
        }
    }
}
