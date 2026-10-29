package com.flockenergy.urja.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransformerDetail(
        Transformer transformer,
        List<Meter> connectedMeters
) {}
