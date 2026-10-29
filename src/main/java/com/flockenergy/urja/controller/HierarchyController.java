package com.flockenergy.urja.controller;

import com.flockenergy.urja.model.HierarchyTreeNode;
import com.flockenergy.urja.service.HierarchyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hierarchy")
@Tag(name = "Network Hierarchy", description = "Distribution network topology from Zone down to DT and meters")
public class HierarchyController {

    private final HierarchyService hierarchyService;

    public HierarchyController(HierarchyService hierarchyService) {
        this.hierarchyService = hierarchyService;
    }

    @GetMapping
    @Operation(
            summary = "Get network hierarchy tree",
            description = """
                    Reconstructs and exposes the complete distribution network topology.
                    Tree levels: Zone › Circle › Division › Subdivision › Substation › Feeder › DT.
                    Each node includes its code, name, level type, and aggregated downstream meter count.
                    """
    )
    public ResponseEntity<List<HierarchyTreeNode>> getHierarchyTree() {
        return ResponseEntity.ok(hierarchyService.getNetworkHierarchyTree());
    }
}
