/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}


	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.

		//首先定义一个容器将已经解析过的 BeanFactoryPostProcessor 缓存起来
		Set<String> processedBeans = new HashSet<>();

		//这里先判断 beanFactory 是否是一个BeanDefinitionRegistry 即实现的是  BeanDefinitionRegistryPostProcessor 这个接口
		//如果是一个 BeanDefinitionRegistry，则在处理的时候可能又新增了新的 BeanFactoryPostProcessor，需要循环处理
		if (beanFactory instanceof BeanDefinitionRegistry) {

			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

			// 此集合用处理 BeanFactoryPostProcessor 的 postProcessBeanFactory 方法
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();

			// 此集合用处理 BeanDefinitionRegistryPostProcessor 的 postProcessBeanFactory 方法
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			/*这里遍历的是用户在调
				invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors)
				这个方法的时候 传入的 beanFactoryPostProcessors 这个集合。。。 要注意下
			*/
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;

					//这里直接把 BeanDefinitionRegistryPostProcessor 的 postProcessBeanDefinitionRegistry 方法处理了
					registryProcessor.postProcessBeanDefinitionRegistry(registry);

					// 把 BeanDefinitionRegistryPostProcessor 的 postProcessBeanFactory 方法加到集合里面最后一起处理
					registryProcessors.add(registryProcessor);
				}
				else {
					// 把 BeanFactoryPostProcessor 的 postProcessBeanFactory 方法加到集合里面最后一起处理
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.

			//这个集合用来存从配置文件或注解中扫描出来的实现了 BeanDefinitionRegistryPostProcessor 这个接口的集合
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.

			//从配置文件或注解中扫描出所有实现了BeanDefinitionRegistryPostProcessor接口的类
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					//将从 从配置文件或注解中扫描出来的实现了 BeanDefinitionRegistryPostProcessor 这个接口 并实现了PriorityOrdered 这个接口 的类 实例化并存入 currentRegistryProcessors
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					//将已经处理过得类加入缓存，防止重复处理
					processedBeans.add(ppName);
				}
			}
			//因为实现了 PriorityOrdered 接口因此需要排下序
			sortPostProcessors(currentRegistryProcessors, beanFactory);

			// 把 BeanDefinitionRegistryPostProcessor 的 postProcessBeanFactory 方法加到集合里面最后一起处理
			registryProcessors.addAll(currentRegistryProcessors);

			//这里也是直接把 BeanDefinitionRegistryPostProcessor 的 postProcessBeanDefinitionRegistry 方法处理了
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);

			//因为上面已经把 currentRegistryProcessors 加入到了 registryProcessors 因此这里可以重新使用currentRegistryProcessors，把它清空了，下面再用它
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			//从配置文件或注解中扫描出所有实现了BeanDefinitionRegistryPostProcessor接口的类
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);

			for (String ppName : postProcessorNames) {
				//将从 从配置文件或注解中扫描出来的实现了 BeanDefinitionRegistryPostProcessor 这个接口 并实现了Ordered 这个接口 的类 实例化并存入 currentRegistryProcessors
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					//将已经处理过得类加入缓存，防止重复处理
					processedBeans.add(ppName);
				}
			}
			//因为实现了 Ordered 接口因此需要排下序
			sortPostProcessors(currentRegistryProcessors, beanFactory);

			// 把 BeanDefinitionRegistryPostProcessor 的 postProcessBeanFactory 方法加到集合里面最后一起处理
			registryProcessors.addAll(currentRegistryProcessors);

			//这里也是直接把 BeanDefinitionRegistryPostProcessor 的 postProcessBeanDefinitionRegistry 方法处理了
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);

			//因为上面已经把 currentRegistryProcessors 加入到了 registryProcessors 因此这里可以重新使用currentRegistryProcessors，把它清空了，下面再用它
			currentRegistryProcessors.clear();


			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			//这里即为在处理上面的 BeanDefinitionRegistryPostProcessor 过程中如果产生了新的实现了 BeanDefinitionRegistryPostProcessor 的接口那么需要循环处理

			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.

			//最后把所有实现了 BeanDefinitionRegistryPostProcessor 接口的 postProcessBeanFactory 方法都执行了
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);

			//最后再把只实现了 BeanFactoryPostProcessor 接口的 postProcessBeanFactory 方法执行了
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			//这里如果一个接口没有实现 BeanDefinitionRegistry 则直接把它的 postProcessBeanFactory 给执行了
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!

		//下面的流程就是处理只实现了 BeanFactoryPostProcessor 的接口的类
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.

		// 实现了BeanFactoryPostProcessor 且实现了 PriorityOrdered 的集合
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();

		// 实现了实现了BeanFactoryPostProcessor 且实现了 Ordered 的集合
		List<String> orderedPostProcessorNames = new ArrayList<>();

		// 实现了BeanFactoryPostProcessor 但是没有实现任何排序接口的集合
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();

		for (String ppName : postProcessorNames) {

			//过滤掉已经执行过得集合
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		//排序并先执行 实现了BeanFactoryPostProcessor 且实现了 PriorityOrdered 的集合
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		//排序然后执行 实现了BeanFactoryPostProcessor 且实现了 Ordered 的集合
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		//最后执行实现了BeanFactoryPostProcessor 没有实现排序接口的集合
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
		//首先获取BeanFactory中所有实现了 BeanPostProcessor 这个接口的子类
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.

		// 存放实现了 BeanPostProcessor 且实现了 PriorityOrdered 接口的子类
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();

		// 存放实现了 BeanPostProcessor 并且是spring内部定义的接口子类
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();

		// 存放实现了 BeanPostProcessor 且实现了 Ordered 接口的子类名称
		List<String> orderedPostProcessorNames = new ArrayList<>();

		// 存放实现了 BeanPostProcessor 且没有实现任何排序接口的子类名称
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();

		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				// 把实现了 BeanPostProcessor 且实现了 PriorityOrdered 接口的子类放到 priorityOrderedPostProcessors 中
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					// 把实现了 BeanPostProcessor，且实现了  MergedBeanDefinitionPostProcessor 且实现了 PriorityOrdered 接口的子类放到 priorityOrderedPostProcessors 中
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				// 把实现了 BeanPostProcessor 且实现了 Ordered 接口的子类放到 orderedPostProcessorNames 中
				orderedPostProcessorNames.add(ppName);
			}
			else {
				// 把实现了 BeanPostProcessor,且没有实现任何排序的子类放到 nonOrderedPostProcessorNames 中
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		// 将实现了 BeanPostProcessor 且实现了 PriorityOrdered 接口的子类 排序并注册到BeanFactory中
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		// 将实现了 BeanPostProcessor 且实现了 Ordered 接口的子类 排序并注册到BeanFactory中
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		// 将实现了 BeanPostProcessor 但是没有实现任何排序的接口子类放到nonOrderedPostProcessors
		// 如果 实现了 BeanPostProcessor 也实现了MergedBeanDefinitionPostProcessor且没有实现任何排序的接口子类放到 internalPostProcessors
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			//将实现了 BeanPostProcessor 但是没有实现任何排序的接口子类放到nonOrderedPostProcessors
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				//将实现了 BeanPostProcessor 也实现了MergedBeanDefinitionPostProcessor且没有实现任何排序的接口子类放到 internalPostProcessors
				internalPostProcessors.add(pp);
			}
		}

		// 将实现了 BeanPostProcessor 且没有实现 MergedBeanDefinitionPostProcessor 且没有实现任何排序的接口子类注册到BeanFactory中
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		// 将 所有实现了 BeanPostProcessor 且实现了 MergedBeanDefinitionPostProcessor 接口的子接口进行排序并注册到BeanFactory中
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// Nothing to sort?
		if (postProcessors.size() <= 1) {
			return;
		}
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
