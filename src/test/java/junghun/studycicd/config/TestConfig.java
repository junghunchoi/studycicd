package junghun.studycicd.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public WebClient testWebClient() {
        return WebClient.builder().build();
    }
}