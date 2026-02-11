package com.maruseron.informationSystem.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Entity
public final class Purchase extends Transaction {
    @Column(name = "bill", nullable = false)
    private String bill;

    @ManyToOne(cascade = CascadeType.ALL)
    private Supplier supplier;

    public Purchase() {}

    public Purchase(int id, Instant createdAt, Employee employee, List<TransactionItem> items,
                    String bill, Supplier supplier) {
        super(id, createdAt, employee, items);
        this.bill = bill;
        this.supplier = supplier;
    }

    public String getBill() {
        return bill;
    }

    public void setBill(String bill) {
        this.bill = bill;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Purchase purchase && id == purchase.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(supplier);
    }
}
