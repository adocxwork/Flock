package com.flockenergy.urja.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HierarchyTreeNode {

    private String id;
    private String name;
    private String level;
    private int meterCount;
    private List<HierarchyTreeNode> children = new ArrayList<>();

    public HierarchyTreeNode() {}

    public HierarchyTreeNode(String id, String name, String level) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.meterCount = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public int getMeterCount() {
        return meterCount;
    }

    public void setMeterCount(int meterCount) {
        this.meterCount = meterCount;
    }

    public void incrementMeterCount() {
        this.meterCount++;
    }

    public List<HierarchyTreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<HierarchyTreeNode> children) {
        this.children = children;
    }
}
