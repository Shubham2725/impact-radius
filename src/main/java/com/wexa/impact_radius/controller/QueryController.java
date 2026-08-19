package com.wexa.impact_radius.controller;

import com.wexa.impact_radius.service.GraphQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final GraphQueryService graphQueryService;

    public QueryController(GraphQueryService graphQueryService) {
        this.graphQueryService = graphQueryService;
    }

    @GetMapping("/servers")
    public List<String> getAllServers() {
        return graphQueryService.getAllServerNames();
    }

    @GetMapping("/blast-radius/{serverName}")
    public Map<String, Object> getBlastRadius(@PathVariable String serverName) {
        return graphQueryService.getBlastRadius(serverName);
    }
}