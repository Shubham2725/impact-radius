package com.wexa.impact_radius.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GraphQueryService {

    private final Driver driver;

    public GraphQueryService(Driver driver) {
        this.driver = driver;
    }

    public Map<String, Object> getBlastRadius(String serverName) {
        String query = """
            MATCH (srv:Server {name: $serverName})<-[:RUNS_ON]-(directService:Service)
            WITH srv, collect(DISTINCT directService) AS directServices
            UNWIND (CASE WHEN size(directServices) = 0 THEN [null] ELSE directServices END) AS ds
            OPTIONAL MATCH (ds)<-[:CALLS*1..3]-(upstreamService:Service)
            WITH srv, directServices, collect(DISTINCT upstreamService) AS upstreamServices
            WITH srv, directServices, upstreamServices, directServices + upstreamServices AS allImpactedServices
            UNWIND (CASE WHEN size(allImpactedServices) = 0 THEN [null] ELSE allImpactedServices END) AS impactedService
            OPTIONAL MATCH (impactedService)<-[:USES]-(app:Application)
            WITH srv, directServices, upstreamServices, collect(DISTINCT app) AS apps
            UNWIND (CASE WHEN size(apps) = 0 THEN [null] ELSE apps END) AS app
            OPTIONAL MATCH (app)-[:OWNED_BY]->(team:Team)
            WITH srv, directServices, upstreamServices, apps, collect(DISTINCT team) AS teams
            RETURN
            srv.name AS server,
            [s IN directServices | s.name] AS directlyAffectedServices,
            [s IN upstreamServices | s.name] AS upstreamAffectedServices,
            [a IN apps | a.name] AS affectedApplications,
            [t IN teams | t.name] AS teamsToNotify
            """;

        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(query, Map.of("serverName", serverName));
                if (!result.hasNext()) {
                    return Map.of("server", serverName, "message", "No dependencies found for this server");
                }
                Record record = result.single();
                Map<String, Object> response = new HashMap<>();
                response.put("server", record.get("server").asString());
                response.put("directlyAffectedServices", record.get("directlyAffectedServices").asList());
                response.put("upstreamAffectedServices", record.get("upstreamAffectedServices").asList());
                response.put("affectedApplications", record.get("affectedApplications").asList());
                response.put("teamsToNotify", record.get("teamsToNotify").asList());
                return response;
            });
        }
    }

    public List<String> getAllServerNames() {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("MATCH (s:Server) RETURN s.name AS name ORDER BY s.name");
                return result.list(record -> record.get("name").asString());
            });
        }
    }

}