package com.flockenergy.urja.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawMeterHierarchy(
        RawHierarchyUnit zone,
        RawHierarchyUnit circle,
        RawHierarchyUnit division,
        RawHierarchyUnit subdivision,
        RawHierarchyUnit substation,
        RawHierarchyUnit feeder,
        RawHierarchyUnit dt
) {}
