package com.spring.debug.supplier;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SupplierTest {

	public static void main(String[] args) {

		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("supplierTest.xml");

		Person person = applicationContext.getBean(Person.class);

		System.out.println(person.getName());
	}
}
