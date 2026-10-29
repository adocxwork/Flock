package com.flockenergy.urja.controller;

import com.flockenergy.urja.model.Transformer;
import com.flockenergy.urja.model.TransformerDetail;
import com.flockenergy.urja.service.TransformerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transformers")
@Tag(name = "Transformers", description = "Distribution transformers, loading, and connected smart meter inventory")
public class TransformerController {

    private final TransformerService transformerService;

    public TransformerController(TransformerService transformerService) {
        this.transformerService = transformerService;
    }

    @GetMapping
    @Operation(
            summary = "List all distribution transformers",
            description = "Retrieves all distribution transformers in the network with their capacity (kVA), feeder affiliation, and connected meter counts (installed, faulty, decommissioned)."
    )
    public ResponseEntity<List<Transformer>> getTransformers() {
        return ResponseEntity.ok(transformerService.getAllTransformers());
    }

    @GetMapping("/{code}")
    @Operation(
            summary = "Get transformer details",
            description = "Retrieves detailed information for a specific distribution transformer, including all smart meters physically connected to it."
    )
    public ResponseEntity<TransformerDetail> getTransformerDetail(
            @Parameter(description = "Transformer code (e.g. DT-001)", required = true)
            @PathVariable String code
    ) {
        return ResponseEntity.ok(transformerService.getTransformerDetail(code));
    }
}
