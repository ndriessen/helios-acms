Use
---
* Should be added in a Hybris extension as a dependency
* in the extension extensionname-spring.xml file. add: 
```xml
<bean class="com.bose.services.acms.client.hybris.ApplicationConfigurationServiceConfigurer">
        <property name="configurationName" value="tax"/>
        <property name="profiles" value="dev,us"/>
    </bean>
```

Todo
---
* Use extension name as default config name, and maybe support profiles from environment?
* Code that needs more profiles (like e.g a basestore) can than pass that as additional ones in the bean def. This would
 support al of our use cases better.
* Refreshing: we should use something like a Proxy Configuration Object, or something similar to @ConfigurationProperties
 that code uses to get values. These proxies or beans (maybe custom refreshable scope) can be refreshed with new config and code using it will
 start using it on next execution. This also provides you a nice VO model for all of you config in one go.
 @ConfigurationProperties can only be used with a higher spring version then hybris uses, and I started a poc to simulate it (more basically)
 