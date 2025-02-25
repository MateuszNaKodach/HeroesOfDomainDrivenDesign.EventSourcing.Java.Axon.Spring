package com.dddheroes.heroesofddd;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI heroesOfDddOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                              .title("Heroes of Domain-Driven Design API")
                              .description("REST API for Heroes of Domain-Driven Design game")
                              .version("v1.0.0")
                );
    }
}