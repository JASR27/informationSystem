package com.maruseron.informationSystem.application;

import com.maruseron.informationSystem.application.dto.PaymentDTO;
import com.maruseron.informationSystem.domain.entity.Payment;
import com.maruseron.informationSystem.domain.value.Either;
import com.maruseron.informationSystem.domain.value.HttpResult;
import com.maruseron.informationSystem.persistence.PaymentRepository;
import com.maruseron.informationSystem.persistence.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentService implements
        CreateService<Payment, PaymentDTO.Create, PaymentDTO.Read, PaymentRepository>
{
    @Autowired
    PaymentRepository repository;

    @Autowired
    SaleRepository saleRepository;

    @Override
    public PaymentRepository repository() {
        return repository;
    }

    @Override
    public Payment fromDTO(PaymentDTO.Create spec) {
        return PaymentDTO.createPayment(
                spec,
                saleRepository.findById(spec.saleId()).orElseThrow());
    }

    @Override
    public PaymentDTO.Read toDTO(Payment entity) {
        return PaymentDTO.fromPayment(entity);
    }

    @Override
    public Either<PaymentDTO.Create, HttpResult> validateForCreation(PaymentDTO.Create request) {
        if (!saleRepository.existsById(request.saleId()))
            return Either.right(new HttpResult(
                    HttpStatus.NOT_FOUND,
                    "La venta indicada no existe."));

        return Either.left(request);
    }

    @Transactional
    public void bulkCreate(List<PaymentDTO.Create> payments) {
        // trust this method: it's only ever called if a sale's validation succeeds
        payments.stream().map(this::fromDTO).forEach(repository::save);
    }
}
