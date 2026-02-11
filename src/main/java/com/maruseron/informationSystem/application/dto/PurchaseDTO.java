package com.maruseron.informationSystem.application.dto;

import com.maruseron.informationSystem.domain.entity.*;

import java.time.Instant;
import java.util.List;

public final class PurchaseDTO {
    private PurchaseDTO() {}

    public static Purchase createPurchase(Employee employee, List<TransactionItem> items,
                                          String bill, Supplier supplier) {
        return new Purchase(
                0,
                Instant.now(),
                employee,
                items,
                bill,
                supplier);
    }

    public static Read fromPurchase(Purchase entity) {
        return new Read(
                entity.getId(),
                entity.getCreatedAt().toEpochMilli(),
                EmployeeDTO.fromEmployee(entity.getEmployee()),
                entity.getItems() == null ? null :
                        entity.getItems().stream().map(TransactionItemDTO::fromTransactionItem).toList(),
                entity.getBill(),
                SupplierDTO.fromSupplier(entity.getSupplier()));
    }

    public record StockInputDescriptor(int productId, String sku, int size, String color,
                                       int quantity) {
        public ProductDetailDTO.Create toProductDetailSpec() {
            return new ProductDetailDTO.Create(productId(), sku(), quantity(), size(), color());
        }
    }

    public record StockDescriptor(int productDetailId, int quantity, boolean isUpdating) {}

    public record Create(int employeeId, List<StockInputDescriptor> items, String bill,
                         int supplierId)
            implements DtoTypes.CreateDto<Purchase> {}

    public record Read(int id, long createdAt, EmployeeDTO.Read employee,
                       List<TransactionItemDTO.Read> items, String bill, SupplierDTO.Read supplier)
            implements DtoTypes.ReadDto<Purchase> {}
}
