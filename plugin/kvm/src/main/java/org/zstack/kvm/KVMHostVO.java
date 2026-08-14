package org.zstack.kvm;

import org.zstack.core.convert.PasswordConverter;
import org.zstack.header.agent.ProxyHardware;
import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.vo.EO;

import javax.persistence.*;

@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = HostEO.class, needView = false)
public class KVMHostVO extends HostVO implements ProxyHardware {
    @Column
    private String username;
    
    @Column
    @Convert(converter = PasswordConverter.class)
    private String password;

    @Column
    private Integer port;

    @Column
    private String osDistribution;

    @Column
    private String osRelease;

    @Column
    private String osVersion;

    @Column
    private String iscsiInitiatorName;

    public KVMHostVO() {
    }

    public KVMHostVO(HostVO vo) {
        super(vo);
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

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getOsDistribution() {
        return osDistribution;
    }

    public void setOsDistribution(String osDistribution) {
        this.osDistribution = osDistribution;
    }

    public String getOsRelease() {
        return osRelease;
    }

    public void setOsRelease(String osRelease) {
        this.osRelease = osRelease;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getIscsiInitiatorName() {
        return iscsiInitiatorName;
    }

    public void setIscsiInitiatorName(String iscsiInitiatorName) {
        this.iscsiInitiatorName = iscsiInitiatorName;
    }
}

