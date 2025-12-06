package com.maruseron.informationSystem.application.dto;

import com.maruseron.informationSystem.domain.entity.*;

import java.time.Instant;
import java.util.List;

public final class AdjustmentDTO {
    private AdjustmentDTO() {}

    public static Adjustment createAdjustment(Employee employee, List<TransactionItem> items,
                                              String reason) {
        return new Adjustment(
                0,
                Instant.now(),
                employee,
                items,
                reason);
    }

    public static AdjustmentDTO.Read fromAdjustment(Adjustment entity) {
        return new AdjustmentDTO.Read(
                entity.getId(),
                entity.getCreatedAt().toEpochMilli(),
                EmployeeDTO.fromEmployee(entity.getEmployee()),
                entity.getItems() == null ? null :
                        entity.getItems().stream().map(TransactionItemDTO::fromTransactionItem).toList(),
                entity.getReason());
    }

    public record Create(int employeeId, List<TransactionItemDTO.Create> items, String reason)
            implements DtoTypes.CreateDto<Adjustment> {}

    public record Read(int id, long createdAt, EmployeeDTO.Read employee,
                       List<TransactionItemDTO.Read> items, String reason)
            implements DtoTypes.ReadDto<Adjustment> {}
}
