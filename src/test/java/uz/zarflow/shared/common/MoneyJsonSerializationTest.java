package uz.zarflow.shared.common;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyJsonSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new MoneyJsonModule());

    @Test
    void serializesAsJsonString() throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(Money.ofWhole(8_500_000));

        assertThat(json).isEqualTo("\"8500000.00\"");
    }

    @Test
    void roundTripsThroughJsonString() throws JsonProcessingException {
        Money original = Money.of("1234567.89");

        String json = objectMapper.writeValueAsString(original);
        Money restored = objectMapper.readValue(json, Money.class);

        assertThat(restored).isEqualTo(original);
    }

    @Test
    void rejectsJsonNumberOnDeserialize() {
        assertThatThrownBy(() -> objectMapper.readValue("8500000.00", Money.class))
                .isInstanceOf(JsonProcessingException.class);
    }

}
