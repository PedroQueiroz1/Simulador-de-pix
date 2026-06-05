package io.pixsimulator.payment.adapter.out.id;

import io.pixsimulator.payment.application.port.out.IdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UuidV7IdGeneratorTest {

    private final IdGenerator idGenerator = new UuidV7IdGenerator();

    @Test
    @DisplayName("Deve gerar UUID nao nulo")
    void shouldGenerateNonNullUuid() {
        UUID uuid = idGenerator.generate();

        assertNotNull(uuid);
    }

    @Test
    @DisplayName("Deve gerar UUID versao 7")
    void shouldGenerateVersion7Uuid() {
        UUID uuid = idGenerator.generate();

        assertEquals(7, uuid.version());
    }
}
