package com.flockenergy.urja.model;

public record MeterHierarchy(
        HierarchyLevel zone,
        HierarchyLevel circle,
        HierarchyLevel division,
        HierarchyLevel subdivision,
        HierarchyLevel substation,
        HierarchyLevel feeder,
        HierarchyLevel dt
) {
    public String getFormattedBreadcrumb() {
        StringBuilder sb = new StringBuilder();
        if (zone != null) sb.append(zone.name());
        if (circle != null) sb.append(" › ").append(circle.name());
        if (division != null) sb.append(" › ").append(division.name());
        if (subdivision != null) sb.append(" › ").append(subdivision.name());
        if (substation != null) sb.append(" › ").append(substation.name());
        if (feeder != null) sb.append(" › ").append(feeder.name());
        if (dt != null) sb.append(" › ").append(dt.name());
        return sb.toString();
    }
}
