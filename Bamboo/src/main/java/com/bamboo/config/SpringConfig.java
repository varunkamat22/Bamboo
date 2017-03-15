package com.bamboo.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("configuration,com.bamboo.core,com.bamboo.jdbc")
public class SpringConfig {

}
