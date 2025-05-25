package com.uppdragsradarn.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Configuration for background job scheduling */
@Configuration
@EnableScheduling
public class SchedulerConfig {

  /**
   * Creates a thread pool task scheduler for background jobs
   *
   * @return ThreadPoolTaskScheduler configured for assignment crawling tasks
   */
  @Bean
  public ThreadPoolTaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(5); // Configurable based on needs
    scheduler.setThreadNamePrefix("assignment-crawler-");
    scheduler.setAwaitTerminationSeconds(60);
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setErrorHandler(
        t -> {
          // Log any errors that occur during task execution
          // Using System.err for demonstration, but should use proper logging
          System.err.println("Scheduled task error: " + t.getMessage());
          t.printStackTrace();
        });
    return scheduler;
  }
}
