package co.devdavco.coworking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CoworkingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoworkingApplication.class, args);
    }
}
