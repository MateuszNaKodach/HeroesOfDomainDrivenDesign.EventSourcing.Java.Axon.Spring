package com.dddheroes.heroesofddd;

import org.springframework.boot.SpringApplication;

public class TestHeroesofdddApplication {

	public static void main(String[] args) {
		SpringApplication.from(HeroesofdddApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
