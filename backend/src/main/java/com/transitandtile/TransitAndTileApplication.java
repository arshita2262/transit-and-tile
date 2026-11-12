package com.transitandtile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Transit & Tile Spring Boot application.
 *
 * @SpringBootApplication enables:
 *   - @Configuration  (this class is a source of bean definitions)
 *   - @EnableAutoConfiguration (Spring Boot auto-configures beans based on classpath)
 *   - @ComponentScan (scans com.transitandtile and sub-packages for components)
 *
 * Spring Boot version: 3.3.6
 * Java version: 17
 */
@SpringBootApplication
public class TransitAndTileApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransitAndTileApplication.class, args);
    }
}
