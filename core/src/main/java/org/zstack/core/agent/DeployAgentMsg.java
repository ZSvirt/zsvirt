package org.zstack.core.agent;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by frank on 12/5/2015.
 */
public class DeployAgentMsg extends NeedReplyMessage {
    private String owner;
    private String ip;
    private String username;
    @NoLogging
    private String password;
    private Integer sshPort;
    private Integer agentPort;
    private boolean deployAnyway;
    private Map<String, Object> config;

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public boolean isDeployAnyway() {
        return deployAnyway;
    }

    public void setDeployAnyway(boolean deployAnyway) {
        this.deployAnyway = deployAnyway;
    }

    public Integer getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(Integer agentPort) {
        this.agentPort = agentPort;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }
}
