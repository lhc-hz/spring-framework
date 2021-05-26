package com.spring.debug.supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

public class Person {

	private Integer id;

	private String name;

	public Person(){}

	public Person(String name){
		this.name = name;
	}

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
