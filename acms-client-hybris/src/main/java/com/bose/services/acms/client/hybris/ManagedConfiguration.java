package com.bose.services.acms.client.hybris;

import java.lang.annotation.*;

/**
 * Mimicked @ConfigurationProperties support (Spring Boot).
 *
 * Add this annotation to a java bean, and the specified configuration will be mapped to the bean.
 * Note that the property names must be valid bean paths for this to work. Nested beans are supported.
 *
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedConfiguration {
    /**
     * The name of the configuration to bind to this object. Synonym
     * for {@link #configName()}.
     * @return the name of the configuration to bind
     */
    String value() default "";

    /**
     * The name of the configuration to bind to this object. Synonym
     * for {@link #value()}.
     * @return the name of the configuration to bind
     */
    String configName() default "";

    /**
     * Profiles to be used for this configuration. These will override any default profiles
     * specified by the extension
     *
     * @see ApplicationConfigurationServiceConfigurer
     * @return the list of profiles to apply
     */
    String[] profiles() default {};

    /**
     * Flag to indicate that when binding to this object invalid fields should be ignored.
     * Invalid means invalid according to the binder that is used, and usually this means
     * fields of the wrong type (or that cannot be coerced into the correct type).
     * @return the flag value (default false)
     */
    boolean ignoreInvalidFields() default false;

    /**
     * Flag to indicate that when binding to this object fields with periods in their
     * names should be ignored.
     * @return the flag value (default false)
     */
    boolean ignoreNestedProperties() default false;

    /**
     * Flag to indicate that when binding to this object unknown fields should be ignored.
     * An unknown field could be a sign of a mistake in the Properties.
     * @return the flag value (default true)
     */
    boolean ignoreUnknownFields() default true;

    /**
     * Flag to indicate that an exception should be raised if a Validator is available and
     * validation fails. If it is set to false, validation errors will be swallowed. They
     * will be logged, but not propagated to the caller.
     * @return the flag value (default true)
     */
    boolean exceptionIfInvalid() default true;


    /**
     * Flag to indicate that configuration loaded from the specified locations should be
     * merged with the default configuration.
     * @return the flag value (default true)
     */
    boolean merge() default true;
}
