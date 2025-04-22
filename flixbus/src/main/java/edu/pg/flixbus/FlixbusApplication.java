package edu.pg.flixbus;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import edu.pg.flixbus.rpc.WebScraper;
import edu.pg.flixbus.dto.QueryDto;
import edu.pg.flixbus.dto.OfferDto;
import org.springframework.beans.factory.annotation.Autowired;


@SpringBootApplication
public class FlixbusApplication implements CommandLineRunner {
	public static void main(String[] args) {
		SpringApplication.run(FlixbusApplication.class, args);
	}


}
