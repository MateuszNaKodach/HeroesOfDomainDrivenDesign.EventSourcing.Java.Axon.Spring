package com.dddheroes.heroesofddd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {
		"com.dddheroes.heroesofddd",
		"org.axonframework",
		"io.axoniq.framework"
})
public class HeroesOfDDDApplication {

	public static void main(String[] args) {
		SpringApplication.run(HeroesOfDDDApplication.class, args);
	}

}
