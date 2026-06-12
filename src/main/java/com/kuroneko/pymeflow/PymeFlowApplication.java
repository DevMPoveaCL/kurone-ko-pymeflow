package com.kuroneko.pymeflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PymeFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(PymeFlowApplication.class, args);
    }
}
