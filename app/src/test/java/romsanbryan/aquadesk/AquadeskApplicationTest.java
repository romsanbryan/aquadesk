package romsanbryan.aquadesk;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class AquadeskApplicationTest {

    @Test
    void mainDelegatesStartupToSpringApplicationWithoutStartingAnotherContext() {
        String[] args = {"--spring.profiles.active=test"};
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            AquadeskApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(AquadeskApplication.class, args));
        }
    }
}
