package uz.zarflow;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * Started once per test JVM and never stopped explicitly -- Testcontainers'
     * Ryuk reaper tears it down when the JVM exits. Every @SpringBootTest context
     * group (RANDOM_PORT vs MOCK, different profiles, etc.) gets its own
     * ApplicationContext, but @ServiceConnection just needs to learn this one
     * already-running container's JDBC URL each time, not start a new one.
     */
    private static final PostgreSQLContainer<?> SHARED_POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

    static {
        SHARED_POSTGRES.start();
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return SHARED_POSTGRES;
    }

}
