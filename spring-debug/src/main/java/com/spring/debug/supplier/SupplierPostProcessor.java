package com.spring.debug.supplier;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

@Component
public class SupplierPostProcessor implements BeanDefinitionRegistryPostProcessor, PriorityOrdered{

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		/*AbstractBeanDefinition abstractBeanDefinition = (AbstractBeanDefinition) beanFactory.getBeanDefinition("person");
		abstractBeanDefinition.setInstanceSupplier(Person::new);
		abstractBeanDefinition.setBeanClass(Person.class);*/
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		AbstractBeanDefinition abstractBeanDefinition = (AbstractBeanDefinition) registry.getBeanDefinition("person");
		abstractBeanDefinition.setInstanceSupplier(Person::new);
		abstractBeanDefinition.setBeanClass(Person.class);
	}

	@Override
	public int getOrder() {
		//这样会导致这个处理器在 ConfigurationClassPostProcessor 之前被处理
		//会导致  AbstractBeanDefinition abstractBeanDefinition = (AbstractBeanDefinition) registry.getBeanDefinition("person"); 这里报错
		//解决办法是改为实现 Ordered 接口或不实现任何排序接口
		return Ordered.LOWEST_PRECEDENCE + 1;
	}
}
