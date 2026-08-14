package org.zstack.core.agent;

/**
 * Created by frank on 12/5/2015.
 */
public class AgentStruct {
    private String fileFolder;
    private String ansibleYaml;
    private String agentId;
    private String agentOwner;

    public String getAgentOwner() {
        return agentOwner;
    }

    public void setAgentOwner(String agentOwner) {
        this.agentOwner = agentOwner;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getFileFolder() {
        return fileFolder;
    }

    public void setFileFolder(String fileFolder) {
        this.fileFolder = fileFolder;
    }

    public String getAnsibleYaml() {
        return ansibleYaml;
    }

    public void setAnsibleYaml(String ansibleYaml) {
        this.ansibleYaml = ansibleYaml;
    }
}
