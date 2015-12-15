package com.bose.services.acms.client.hybris;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

/**
 * Created by niki on 15/12/15.
 */
public class ConfigurationBeanPostProcessor implements BeanPostProcessor, EnvironmentAware {
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        MutablePropertyValues values = new MutablePropertyValues();
        values.addPropertyValue("id", "7839");
        values.addPropertyValue("name", "KING");
        values.addPropertyValue("department", "20");

        Employee employee = new Employee();
        DataBinder binder = new DataBinder(employee);
        binder.bind(values);

        log.debug(employee);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName)
            throws BeansException {
        ManagedConfiguration annotation = AnnotationUtils
                .findAnnotation(bean.getClass(), ManagedConfiguration.class);
        if (annotation != null) {
            postProcessBeforeInitialization(bean, beanName, annotation);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        return bean;
    }

    private void postProcessBeforeInitialization(Object bean, String beanName,
                                                 ManagedConfiguration annotation) {
        Object target = bean;
        PropertiesConfigurationFactory<Object> factory = new PropertiesConfigurationFactory<Object>(
                target);
        String label = "";
        String name = annotation.configName();
        String[] profiles = annotation.profiles();
        ConfigurationServicePropertySourceLocator locator = new ConfigurationServicePropertySourceLocator();
        MutablePropertySources sources = new MutablePropertySources();
        sources.addFirst(locator.locate(label, name, profiles));
        factory.setPropertySources(sources);

        //factory.setValidator(determineValidator(bean));
        // If no explicit conversion service is provided we add one so that (at least)
        // comma-separated arrays of convertibles can be bound automatically
        factory.setConversionService(this.conversionService == null
                ? getDefaultConversionService() : this.conversionService);
        if (annotation != null) {
            factory.setIgnoreInvalidFields(annotation.ignoreInvalidFields());
            factory.setIgnoreUnknownFields(annotation.ignoreUnknownFields());
            factory.setExceptionIfInvalid(annotation.exceptionIfInvalid());
            factory.setIgnoreNestedProperties(annotation.ignoreNestedProperties());
            String targetName = (StringUtils.hasLength(annotation.value())
                    ? annotation.value() : annotation.prefix());
            if (StringUtils.hasLength(targetName)) {
                factory.setTargetName(targetName);
            }
        }
        try {
            factory.bindPropertiesToTarget();
        }
        catch (Exception ex) {
            String targetClass = ClassUtils.getShortName(target.getClass());
            throw new BeanCreationException(beanName, "Could not bind properties to "
                    + targetClass + " (" + getAnnotationDetails(annotation) + ")", ex);
        }
    }
}
