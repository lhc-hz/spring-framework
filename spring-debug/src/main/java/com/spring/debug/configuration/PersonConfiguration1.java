package com.spring.debug.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

//@Configuration
//@Component
public class PersonConfiguration1 {

	@Bean
	private PersonConfiguration2 personConfiguration2(){
		return new PersonConfiguration2();
	}

	@Bean
	private PersonConfiguration3 personConfiguration3(){
		personConfiguration2();
		return new PersonConfiguration3();
	}

}
