package com.codice.sra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class SraApiApplication {
	@PostConstruct
	public void configurarZonaHoraria() {
		TimeZone.setDefault(TimeZone.getTimeZone("America/El_Salvador"));
	}

	public static void main(String[] args) {
		SpringApplication.run(SraApiApplication.class, args);
	}

}
