package uz.zarflow.shared.common;

import java.io.IOException;
import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Money and raw BigDecimal amounts must travel over JSON as strings, never as
 * numbers — a JSON number round-tripped through a JS client loses precision.
 */
@Component
public class MoneyJsonModule extends SimpleModule {

    public MoneyJsonModule() {
        addSerializer(Money.class, new MoneySerializer());
        addDeserializer(Money.class, new MoneyDeserializer());
        addSerializer(BigDecimal.class, new BigDecimalSerializer());
        addDeserializer(BigDecimal.class, new BigDecimalDeserializer());
    }

    private static class MoneySerializer extends JsonSerializer<Money> {
        @Override
        public void serialize(Money value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.amount().toPlainString());
        }
    }

    private static class MoneyDeserializer extends JsonDeserializer<Money> {
        @Override
        public Money deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() != JsonToken.VALUE_STRING) {
                throw new JsonMappingException(p, "Money must be a JSON string, not " + p.currentToken());
            }
            return Money.of(p.getText());
        }
    }

    private static class BigDecimalSerializer extends JsonSerializer<BigDecimal> {
        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.toPlainString());
        }
    }

    private static class BigDecimalDeserializer extends JsonDeserializer<BigDecimal> {
        @Override
        public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() != JsonToken.VALUE_STRING) {
                throw new JsonMappingException(p, "Amount must be a JSON string, not " + p.currentToken());
            }
            return new BigDecimal(p.getText());
        }
    }

}
