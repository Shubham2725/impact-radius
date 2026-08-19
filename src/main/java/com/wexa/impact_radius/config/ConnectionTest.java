package com.wexa.impact_radius.config;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

//@Component
public class ConnectionTest implements CommandLineRunner {

    private final Driver driver;

    public ConnectionTest(Driver driver) {
        this.driver = driver;
    }

    @Override
    public void run(String... args) {
        try (Session session = driver.session()) {
            var result = session.run("RETURN 1 AS test");
            System.out.println("✅ CognoDB connection successful: " + result.single().get("test").asInt());
        } catch (Exception e) {
            System.out.println("❌ CognoDB connection failed: " + e.getMessage());
        }
    }
}