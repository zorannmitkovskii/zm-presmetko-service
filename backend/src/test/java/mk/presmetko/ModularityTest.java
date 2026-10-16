package mk.presmetko;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    @Test
    void moduleBoundariesAreRespected() {
        ApplicationModules.of(PresmetkoApplication.class).verify();
    }
}
