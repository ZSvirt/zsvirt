package org.zstack.network.service.vip;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.identity.Session;

/**
 * Created by shixin on 2019/05/17.
 */
public class CreateVipMsg extends NeedReplyMessage {
    private String name;
    private String description;
    private String l3NetworkUuid;
    private String allocatorStrategy;
    private String requiredIp;
    private SessionInventory session;
    private boolean system;
    private Integer ipVersion;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public void setAllocatorStrategy(String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
    }

    public String getRequiredIp() {
        return requiredIp;
    }

    public void setRequiredIp(String requiredIp) {
        this.requiredIp = requiredIp;
    }

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }
}
