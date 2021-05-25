package com.spring.debug.beanPostProcess;

import com.spring.debug.configuration.PersonConfiguration1;
import org.springframework.context.annotation.Bean;

public interface MyInterfaces {

	@Bean
	PersonConfiguration1 personConfiguration1();

}
