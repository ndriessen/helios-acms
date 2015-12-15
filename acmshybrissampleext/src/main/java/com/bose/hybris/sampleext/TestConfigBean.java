package com.bose.hybris.sampleext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Created by niki on 10/12/15.
 */
public class TestConfigBean implements ApplicationListener<ContextRefreshedEvent> {
    @Value("${tax.url:This means config server didn't work...}")
    private String taxServer;
    @Value("${tax.account:This means config server didn't work...}")
    private String taxAccount;

    @Value("${java.home:No Java home?}")
    private String javaHome;

    //this is set in spring xml
    private String taxUsername;

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

    public void setTaxUsername(String taxUsername) {
        this.taxUsername = taxUsername;
    }
}
