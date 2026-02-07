package com.maruseron.informationSystem.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maruseron.informationSystem.domain.value.Either;
import com.maruseron.informationSystem.domain.value.HttpResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

@Service
public final class CurrencyConversionService {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    record ExchangeRate(
            String fuente,
            String nombre,
            double compra,
            double venta,
            double promedio,
            String fechaActualizacion
    ) {}

    static CompletableFuture<BigDecimal> VED_PER_USD_RATE = HttpClient
            .newHttpClient()
            .sendAsync(
                    HttpRequest.newBuilder()
                            .uri(URI.create("https://ve.dolarapi.com/v1/dolares/oficial"))
                            .GET()
                            .build(),
                    HttpResponse
                            .BodyHandlers
                            .ofString())
            .thenApply(response ->
                    readValueUnchecked(response.body(), ExchangeRate.class))
            .thenApply(ExchangeRate::promedio)
            .thenApply(BigDecimal::new);

    public String exchangeRate() {
        return VED_PER_USD_RATE.join().toString();
    }

    public Either<BigDecimal, HttpResult> usdToVed(String usd) {
        try {
            return Either.left(usdToVed(new BigDecimal(usd)));
        } catch (NumberFormatException _) {
            return Either.right(new HttpResult(
                    HttpStatus.BAD_REQUEST,
                    "No se ha proporcionado un número"));
        }
    }

    public BigDecimal usdToVed(BigDecimal usd) {
        return usd.multiply(VED_PER_USD_RATE.join());
    }

    public Either<BigDecimal, HttpResult> vedToUsd(String ved) {
        try {
            return Either.left(vedToUsd(new BigDecimal(ved)));
        } catch (NumberFormatException _) {
            return Either.right(new HttpResult(
                    HttpStatus.BAD_REQUEST,
                    "No se ha proporcionado un número"));
        }
    }

    public BigDecimal vedToUsd(BigDecimal ved) {
        return ved.divide(VED_PER_USD_RATE.join(), 2, RoundingMode.HALF_UP);
    }

    private static <T> T readValueUnchecked(String json, Class<T> valueType) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON: " + e.getMessage(), e);
        }
    }
}
