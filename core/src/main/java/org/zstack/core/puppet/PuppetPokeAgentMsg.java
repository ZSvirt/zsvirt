package org.zstack.core.puppet;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

import java.io.Serializable;

public class PuppetPokeAgentMsg extends NeedReplyMessage {
    private String hostname;
    private String username;
    @NoLogging
    private String password;
    private Integer sshPort;
    private String nodeName;
    
    public String getHostname() {
        return hostname;
    }
    public void setHostname(String hostname) {
        this.hostname = hostname;
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
    public String getNodeName() {
        return nodeName;
    }
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
	public Integer getSshPort() {
		return sshPort;
	}
	public void setSshPort(Integer sshPort) {
		this.sshPort = sshPort;
	}
}
