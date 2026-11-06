package uz.zarflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FlywayBaselineTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliedBaselineMigration() {
        Integer appliedSuccessfully = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success = true",
                Integer.class);

        assertThat(appliedSuccessfully).isEqualTo(1);
    }

    @Test
    void flywayAppliedAllCoreSchemaMigrations() {
        Integer appliedSuccessfully = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version in ('1', '2', '3', '4', '5', '6', '7') and success = true",
                Integer.class);

        assertThat(appliedSuccessfully).isEqualTo(7);
    }

}
