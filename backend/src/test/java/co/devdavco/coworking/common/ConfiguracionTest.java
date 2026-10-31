package co.devdavco.coworking.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ConfiguracionTest {

    /**
     * Si application.yml activara "local" por defecto, un jar arrancado sin SPRING_PROFILES_ACTIVE
     * usaría el secreto JWT de desarrollo que está en el repo y cualquiera podría firmar tokens de ADMIN.
     */
    @Test
    void sinPerfilExplicitoNoSeActivaElPerfilLocal() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties propiedades = yaml.getObject();

        assertThat(propiedades).doesNotContainKey("spring.profiles.default");
        assertThat(propiedades.getProperty("app.jwt.secret")).isEqualTo("${JWT_SECRET}");
    }
}
