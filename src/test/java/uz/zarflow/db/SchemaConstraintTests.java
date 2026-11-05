package uz.zarflow.db;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import uz.zarflow.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SchemaConstraintTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID createUser() {
        return jdbcTemplate.queryForObject(
                "insert into users (telegram_id) values (?) returning id",
                UUID.class,
                ThreadLocalRandom.current().nextLong());
    }

    @Test
    void rejectsInvalidAccountType() {
        UUID userId = createUser();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into accounts (user_id, name, type, initial_balance) values (?, ?, ?, 0)",
                userId, "Broken", "NOT_A_TYPE"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsNonUzsCurrency() {
        UUID userId = createUser();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into accounts (user_id, name, type, currency, initial_balance) values (?, ?, 'CASH', ?, 0)",
                userId, "Broken", "USD"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsIncomeCategoryWithNecessity() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into categories (code, name_uz, name_ru, type, necessity, icon, color) "
                        + "values ('broken', 'Broken', 'Broken', 'INCOME', 'ESSENTIAL', 'tag', '#000000')"))
                .isInstanceOf(DataAccessException.class);
    }

}
