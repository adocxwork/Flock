package com.flockenergy.urja.service;

import com.flockenergy.urja.model.HierarchyTreeNode;
import com.flockenergy.urja.model.Meter;
import com.flockenergy.urja.model.MeterHierarchy;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HierarchyService {

    private final MeterDataSyncService syncService;

    public HierarchyService(MeterDataSyncService syncService) {
        this.syncService = syncService;
    }

    @Cacheable(value = "hierarchy")
    public List<HierarchyTreeNode> getNetworkHierarchyTree() {
        List<Meter> meters = syncService.getAllMeters();
        Map<String, HierarchyTreeNode> zoneNodes = new LinkedHashMap<>();

        for (Meter m : meters) {
            MeterHierarchy h = m.hierarchy();
            if (h == null) continue;

            // 1. Zone
            String zoneId = (h.zone() != null && h.zone().code() != null) ? h.zone().code() : "UNKNOWN_ZONE";
            String zoneName = (h.zone() != null && h.zone().name() != null) ? h.zone().name() : zoneId;
            HierarchyTreeNode zoneNode = zoneNodes.computeIfAbsent(zoneId, k -> new HierarchyTreeNode(zoneId, zoneName, "ZONE"));
            zoneNode.incrementMeterCount();

            // 2. Circle
            String circleId = (h.circle() != null && h.circle().code() != null) ? h.circle().code() : "UNKNOWN_CIRCLE";
            String circleName = (h.circle() != null && h.circle().name() != null) ? h.circle().name() : circleId;
            HierarchyTreeNode circleNode = findOrAddChild(zoneNode, circleId, circleName, "CIRCLE");
            circleNode.incrementMeterCount();

            // 3. Division
            String divId = (h.division() != null && h.division().code() != null) ? h.division().code() : "UNKNOWN_DIVISION";
            String divName = (h.division() != null && h.division().name() != null) ? h.division().name() : divId;
            HierarchyTreeNode divNode = findOrAddChild(circleNode, divId, divName, "DIVISION");
            divNode.incrementMeterCount();

            // 4. Subdivision
            String subDivId = (h.subdivision() != null && h.subdivision().code() != null) ? h.subdivision().code() : "UNKNOWN_SUBDIVISION";
            String subDivName = (h.subdivision() != null && h.subdivision().name() != null) ? h.subdivision().name() : subDivId;
            HierarchyTreeNode subDivNode = findOrAddChild(divNode, subDivId, subDivName, "SUBDIVISION");
            subDivNode.incrementMeterCount();

            // 5. Substation
            String ssId = (h.substation() != null && h.substation().code() != null) ? h.substation().code() : "UNKNOWN_SUBSTATION";
            String ssName = (h.substation() != null && h.substation().name() != null) ? h.substation().name() : ssId;
            HierarchyTreeNode ssNode = findOrAddChild(subDivNode, ssId, ssName, "SUBSTATION");
            ssNode.incrementMeterCount();

            // 6. Feeder
            String feederId = (h.feeder() != null && h.feeder().code() != null) ? h.feeder().code() : "UNKNOWN_FEEDER";
            String feederName = (h.feeder() != null && h.feeder().name() != null) ? h.feeder().name() : feederId;
            HierarchyTreeNode feederNode = findOrAddChild(ssNode, feederId, feederName, "FEEDER");
            feederNode.incrementMeterCount();

            // 7. DT
            String dtId = (h.dt() != null && h.dt().code() != null) ? h.dt().code() : (m.dtCode() != null ? m.dtCode() : "UNKNOWN_DT");
            String dtName = (h.dt() != null && h.dt().name() != null) ? h.dt().name() : dtId;
            HierarchyTreeNode dtNode = findOrAddChild(feederNode, dtId, dtName, "DT");
            dtNode.incrementMeterCount();
        }

        return new ArrayList<>(zoneNodes.values());
    }

    private HierarchyTreeNode findOrAddChild(HierarchyTreeNode parent, String childId, String childName, String level) {
        for (HierarchyTreeNode child : parent.getChildren()) {
            if (child.getId().equalsIgnoreCase(childId)) {
                return child;
            }
        }
        HierarchyTreeNode newChild = new HierarchyTreeNode(childId, childName, level);
        parent.getChildren().add(newChild);
        return newChild;
    }
}
