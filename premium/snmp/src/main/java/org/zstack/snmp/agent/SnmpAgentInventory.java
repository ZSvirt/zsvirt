package org.zstack.snmp.agent;

import com.google.common.base.Objects;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = SnmpAgentVO.class, collectionValueOfMethod = "valueOf1")
public class SnmpAgentInventory implements Serializable {
    private String uuid;
    private String version;
    private String readCommunity;
    private String userName;
    private String authAlgorithm;
    private String authPassword;
    private String privacyAlgorithm;
    private String privacyPassword;
    private Integer port;
    private String status;
    private String securityLevel;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    protected SnmpAgentInventory(SnmpAgentVO vo) {
        this.setUuid(vo.getUuid());
        this.setVersion(vo.getVersion().name());
        this.setPort(vo.getPort());
        this.setSecurityLevel(vo.getSecurityLevel().name());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setStatus(vo.getStatus().toString());
        if (vo.getVersion().equals(SnmpAgentVersion.v2c)) {
            this.setReadCommunity(vo.getReadCommunity());
            return;
        }
        this.setUserName(vo.getUserName());
        if (vo.getSecurityLevel() == SecurityLevel.authPriv) {
            this.setAuthAlgorithm(vo.getAuthAlgorithm().name());
            this.setAuthPassword(vo.getAuthPassword());
            this.setPrivacyAlgorithm(vo.getPrivacyAlgorithm().name);
            this.setPrivacyPassword(vo.getPrivacyPassword());
        }
        if (vo.getSecurityLevel() == SecurityLevel.authNoPriv) {
            this.setAuthAlgorithm(vo.getAuthAlgorithm().name());
            this.setAuthPassword(vo.getAuthPassword());
        }
    }

    public static SnmpAgentInventory valueOf(SnmpAgentVO vo) {
        return new SnmpAgentInventory(vo);
    }

    public static List<SnmpAgentInventory> valueOf1(Collection<SnmpAgentVO> vos) {
        List<SnmpAgentInventory> invs = new ArrayList<SnmpAgentInventory>(vos.size());
        for (SnmpAgentVO vo : vos) {
            invs.add(SnmpAgentInventory.valueOf(vo));
        }
        return invs;
    }

    public SnmpAgentInventory() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String $paramName) {
        uuid = $paramName;
    }

    public String getReadCommunity() {
        return readCommunity;
    }

    public void setReadCommunity(String $paramName) {
        readCommunity = $paramName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String $paramName) {
        userName = $paramName;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String $paramName) {
        authPassword = $paramName;
    }

    public String getPrivacyPassword() {
        return privacyPassword;
    }

    public void setPrivacyPassword(String $paramName) {
        privacyPassword = $paramName;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer $paramName) {
        port = $paramName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthAlgorithm() {
        return authAlgorithm;
    }

    public void setAuthAlgorithm(String authAlgorithm) {
        this.authAlgorithm = authAlgorithm;
    }

    public String getPrivacyAlgorithm() {
        return privacyAlgorithm;
    }

    public void setPrivacyAlgorithm(String privacyAlgorithm) {
        this.privacyAlgorithm = privacyAlgorithm;
    }

    public String getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnmpAgentInventory inventory = (SnmpAgentInventory) o;
        return port == inventory.port && Objects.equal(uuid, inventory.uuid) && Objects.equal(version, inventory.version) && Objects.equal(readCommunity, inventory.readCommunity) && Objects.equal(userName, inventory.userName) && Objects.equal(authAlgorithm, inventory.authAlgorithm) && Objects.equal(authPassword, inventory.authPassword) && Objects.equal(privacyAlgorithm, inventory.privacyAlgorithm) && Objects.equal(privacyPassword, inventory.privacyPassword) && Objects.equal(securityLevel, inventory.securityLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid, version, readCommunity, userName, authAlgorithm, authPassword, privacyAlgorithm, privacyPassword, port, securityLevel);
    }
}
