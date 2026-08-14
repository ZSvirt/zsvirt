package org.zstack.storage.primary.block.vendor.xstor;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2023/2/15 18:07
 */
public class Initiator {
    public String alias;
    public String hostGroupName;
    public String hostName;
    public Integer host_id;
    public Integer id;
    public String name;
    public Integer version;

    public void setId(Integer id) {
        this.id = id;
    }

    public void setHostId(Integer host_id) {
        this.host_id = host_id;
    }

    public Integer getHostId() {
        return host_id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Integer getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Integer getVersion() {
        return version;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostGroupName(String hostGroupName) {
        this.hostGroupName = hostGroupName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
