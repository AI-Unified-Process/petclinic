package ai.unifiedprocess.petclinic;

import org.springframework.boot.SpringApplication;

/**
 * Local development entry point for {@code ./mvnw spring-boot:test-run}: the
 * application on the test classpath, so its embedded H2 database also gets the
 * seed data of {@code src/test/resources/db/migration}. {@code ./mvnw
 * spring-boot:run} starts the same application with an empty database.
 */
public class TestPetclinicApplication {

    public static void main(String[] args) {
        SpringApplication.from(PetclinicApplication::main).run(args);
    }

}
