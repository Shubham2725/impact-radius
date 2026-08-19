package com.wexa.impact_radius.controller;

import com.wexa.impact_radius.service.GraphSeedService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SeedController {

    private final GraphSeedService graphSeedService;

    public SeedController(GraphSeedService graphSeedService) {
        this.graphSeedService = graphSeedService;
    }

    @PostMapping("/seed")
    public String seed() {
        graphSeedService.seed();
        return "Graph seeded successfully";
    }
}