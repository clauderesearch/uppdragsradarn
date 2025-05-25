package com.uppdragsradarn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UppdragsRadarnApplication {

  public static void main(String[] args) {
    SpringApplication.run(UppdragsRadarnApplication.class, args);
  }
}
