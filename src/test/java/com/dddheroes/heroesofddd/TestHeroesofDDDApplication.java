package com.dddheroes.heroesofddd;

import org.springframework.boot.SpringApplication;

public class TestHeroesofDDDApplication {

	public static void main(String[] args) {
		SpringApplication.from(HeroesofDDDApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
