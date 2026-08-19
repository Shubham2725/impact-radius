package com.wexa.impact_radius.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

@Service
public class GraphSeedService {

    private final Driver driver;

    public GraphSeedService(Driver driver) {
        this.driver = driver;
    }

    public void seed() {
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                // Step 1: wipe existing data so re-seeding is safe
                tx.run("MATCH (n) DETACH DELETE n");

                // Step 2: create Teams
                tx.run("""
                    MERGE (t1:Team {name: 'Platform Team', slack_channel: '#platform-team'})
                    MERGE (t2:Team {name: 'Payments Team', slack_channel: '#payments-team'})
                    """);

                // Step 3: create Applications
                tx.run("""
                    MERGE (a1:Application {name: 'Checkout App', criticality: 'high'})
                    MERGE (a2:Application {name: 'Order Service Portal', criticality: 'medium'})
                    MERGE (a3:Application {name: 'Inventory Dashboard', criticality: 'low'})
                    """);

                // Step 4: create Services
                tx.run("""
                    MERGE (s1:Service {name: 'Payment Service', version: '2.3'})
                    MERGE (s2:Service {name: 'Auth Service', version: '1.8'})
                    MERGE (s3:Service {name: 'Order Service', version: '3.1'})
                    MERGE (s4:Service {name: 'Inventory Service', version: '1.2'})
                    MERGE (s5:Service {name: 'Notification Service', version: '1.0'})
                    """);

                // Step 5: create Servers
                tx.run("""
                    MERGE (srv1:Server {name: 'srv-01', region: 'us-east', status: 'active'})
                    MERGE (srv2:Server {name: 'srv-02', region: 'us-east', status: 'active'})
                    MERGE (srv3:Server {name: 'srv-03', region: 'us-west', status: 'active'})
                    MERGE (srv4:Server {name: 'srv-04', region: 'us-west', status: 'active'})
                    """);

                // Step 6: relationships — Applications USE Services
                tx.run("""
                    MATCH (a1:Application {name: 'Checkout App'}), (s1:Service {name: 'Payment Service'})
                    MERGE (a1)-[:USES]->(s1)
                    """);
                tx.run("""
                    MATCH (a1:Application {name: 'Checkout App'}), (s2:Service {name: 'Auth Service'})
                    MERGE (a1)-[:USES]->(s2)
                    """);
                tx.run("""
                    MATCH (a2:Application {name: 'Order Service Portal'}), (s3:Service {name: 'Order Service'})
                    MERGE (a2)-[:USES]->(s3)
                    """);
                tx.run("""
                    MATCH (a3:Application {name: 'Inventory Dashboard'}), (s4:Service {name: 'Inventory Service'})
                    MERGE (a3)-[:USES]->(s4)
                    """);

                // Step 7: relationships — Services CALL other Services
                tx.run("""
                    MATCH (s1:Service {name: 'Payment Service'}), (s2:Service {name: 'Auth Service'})
                    MERGE (s1)-[:CALLS]->(s2)
                    """);
                tx.run("""
                    MATCH (s1:Service {name: 'Payment Service'}), (s5:Service {name: 'Notification Service'})
                    MERGE (s1)-[:CALLS]->(s5)
                    """);
                tx.run("""
                    MATCH (s3:Service {name: 'Order Service'}), (s1:Service {name: 'Payment Service'})
                    MERGE (s3)-[:CALLS]->(s1)
                    """);
                tx.run("""
                    MATCH (s3:Service {name: 'Order Service'}), (s4:Service {name: 'Inventory Service'})
                    MERGE (s3)-[:CALLS]->(s4)
                    """);

                // Step 8: relationships — Services RUN_ON Servers
                tx.run("""
                    MATCH (s1:Service {name: 'Payment Service'}), (srv1:Server {name: 'srv-01'})
                    MERGE (s1)-[:RUNS_ON]->(srv1)
                    """);
                tx.run("""
                    MATCH (s2:Service {name: 'Auth Service'}), (srv1:Server {name: 'srv-01'})
                    MERGE (s2)-[:RUNS_ON]->(srv1)
                    """);
                tx.run("""
                    MATCH (s3:Service {name: 'Order Service'}), (srv2:Server {name: 'srv-02'})
                    MERGE (s3)-[:RUNS_ON]->(srv2)
                    """);
                tx.run("""
                    MATCH (s4:Service {name: 'Inventory Service'}), (srv3:Server {name: 'srv-03'})
                    MERGE (s4)-[:RUNS_ON]->(srv3)
                    """);
                tx.run("""
                    MATCH (s5:Service {name: 'Notification Service'}), (srv4:Server {name: 'srv-04'})
                    MERGE (s5)-[:RUNS_ON]->(srv4)
                    """);

                // Step 9: relationships — Applications OWNED_BY Teams
                tx.run("""
                    MATCH (a1:Application {name: 'Checkout App'}), (t2:Team {name: 'Payments Team'})
                    MERGE (a1)-[:OWNED_BY]->(t2)
                    """);
                tx.run("""
                    MATCH (a2:Application {name: 'Order Service Portal'}), (t1:Team {name: 'Platform Team'})
                    MERGE (a2)-[:OWNED_BY]->(t1)
                    """);
                tx.run("""
                    MATCH (a3:Application {name: 'Inventory Dashboard'}), (t1:Team {name: 'Platform Team'})
                    MERGE (a3)-[:OWNED_BY]->(t1)
                    """);

                return null;
            });
        }
    }
}