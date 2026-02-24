package com.wolffsoft.jdrivenecommerce.config.database;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayDataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @FlywayDataSource
    @Bean
    @ConfigurationProperties(prefix = "spring.flyway")
    DataSource flywayDataSource() {
       return DataSourceBuilder.create().build();
    }
}
