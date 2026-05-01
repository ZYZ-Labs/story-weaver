package com.storyweaver.domain.dto;

public class NodeActionRequestDTO {

    private String nodeId;
    private String checkpointId;
    private String selectedOptionId;
    private String customAction;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getCheckpointId() {
        return checkpointId;
    }

    public void setCheckpointId(String checkpointId) {
        this.checkpointId = checkpointId;
    }

    public String getSelectedOptionId() {
        return selectedOptionId;
    }

    public void setSelectedOptionId(String selectedOptionId) {
        this.selectedOptionId = selectedOptionId;
    }

    public String getCustomAction() {
        return customAction;
    }

    public void setCustomAction(String customAction) {
        this.customAction = customAction;
    }
}
