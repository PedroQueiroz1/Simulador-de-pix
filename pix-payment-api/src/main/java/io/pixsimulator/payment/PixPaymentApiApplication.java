package io.pixsimulator.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do microservico pix-payment-api (Lote 1).
 */
@SpringBootApplication
public class PixPaymentApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PixPaymentApiApplication.class, args);
    }
}
