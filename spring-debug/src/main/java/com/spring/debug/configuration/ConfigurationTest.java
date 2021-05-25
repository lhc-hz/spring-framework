package com.spring.debug.configuration;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ConfigurationTest {

	public static void main(String[] args) {

		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("ConfigurationTest.xml");

		/*PersonConfiguration1 personConfiguration1 = applicationContext.getBean(PersonConfiguration1.class);
		PersonConfiguration3 personConfiguration3 = applicationContext.getBean(PersonConfiguration3.class);*/
		System.out.println(applicationContext.getBean(PersonConfiguration1.class));
		System.out.println(applicationContext.getBean(PersonConfiguration1.class));
		System.out.println(applicationContext.getBean(PersonConfiguration3.class));
		System.out.println(applicationContext.getBean(PersonConfiguration3.class));
	}
}
