package uz.zarflow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(ZarflowApplication.class);

    @Test
    void verifiesModuleStructure() {
        modules.verify();
    }

}
