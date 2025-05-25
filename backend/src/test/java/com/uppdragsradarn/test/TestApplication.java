package com.uppdragsradarn.test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Test application used for slice tests */
@SpringBootApplication
@ComponentScan("com.uppdragsradarn")
@EntityScan("com.uppdragsradarn.domain.model")
@EnableJpaRepositories("com.uppdragsradarn.domain.repository")
public class TestApplication {}
