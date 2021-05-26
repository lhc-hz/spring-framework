package com.spring.debug.supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
public class PersonConfiguration {

	@Bean
	public Person person(){
		return person();
	}
}
