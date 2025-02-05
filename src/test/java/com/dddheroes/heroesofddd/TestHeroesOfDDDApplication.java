package com.dddheroes.heroesofddd;

import org.springframework.boot.SpringApplication;

public class TestHeroesOfDDDApplication {

	public static void main(String[] args) {
		SpringApplication.from(HeroesOfDDDApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
