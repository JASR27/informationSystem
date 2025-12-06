package com.maruseron.informationSystem.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Entity
public final class Adjustment extends Transaction {
    @Column(name = "reason", nullable = false)
    private String reason;

    public Adjustment() {}

    public Adjustment(int id, Instant createdAt, Employee employee,
                    List<TransactionItem> items, String reason) {
        super(id, createdAt, employee, items);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Adjustment adjustment && id == adjustment.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(reason);
    }
}
