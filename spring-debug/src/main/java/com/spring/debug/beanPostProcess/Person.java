package com.spring.debug.beanPostProcess;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

//@Configuration
@PropertySource(value = "classpath:params.properties")
@Component
public class Person {

	private Integer id;

	@Value("${name}")
	private String name;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		System.out.println("getName....");
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}