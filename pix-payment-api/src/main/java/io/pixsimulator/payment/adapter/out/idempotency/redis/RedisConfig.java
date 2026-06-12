package io.pixsimulator.payment.adapter.out.idempotency.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuracao Redis do adapter de idempotencia.
 *
 * <p>Define um {@link RedisTemplate} tipado em {@link RedisIdempotencyRecord}:
 * chave como String e valor serializado em JSON. O {@code ObjectMapper} local e
 * suficiente porque o registro serializado so contem Strings e numeros (as
 * datas ja chegam como texto ISO-8601).
 *
 * <p>Fica confinado ao pacote do adapter: nenhuma outra camada conhece Redis.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, RedisIdempotencyRecord> idempotencyRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, RedisIdempotencyRecord> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        Jackson2JsonRedisSerializer<RedisIdempotencyRecord> valueSerializer =
                new Jackson2JsonRedisSerializer<>(new ObjectMapper(), RedisIdempotencyRecord.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
