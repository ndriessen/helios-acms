package com.bose.services.acms.client.hybris;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Created by niki on 10/12/15.
 */
@Component
public class TestConfigBean implements ApplicationListener<ContextRefreshedEvent> {
    @Value("${tax.url:This means config server didn't work...}")
    private String taxServer;
    @Value("${tax.account:This means config server didn't work...}")
    private String taxAccount;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if(event.getSource() == this.applicationContext) {
            System.out.println("taxServer = " + taxServer);
            System.out.println("taxAccount = " + taxAccount);
            System.out.println(event.getApplicationContext().getDisplayName());
        }
    }
}
