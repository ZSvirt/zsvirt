package org.zstack.header.host;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostPowerStatus;
import org.zstack.header.message.GsonTransient;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = HostIpmiVO.class, collectionValueOfMethod = "valueOf1")
public class HostIpmiInventory implements Serializable {
    private String uuid;
    private String ipmiAddress;
    private String ipmiUsername;
    private int ipmiPort;

    @GsonTransient
    @APINoSee
    private String ipmiPassword;
    private String ipmiPowerStatus;

    protected HostIpmiInventory(HostIpmiVO vo) {
        this.setUuid(vo.getUuid());
        this.setIpmiAddress(vo.getIpmiAddress());
        this.setIpmiUsername(vo.getIpmiUsername());
        this.setIpmiPort(vo.getIpmiPort());
        this.setIpmiPassword(vo.getIpmiPassword());
        this.setIpmiPowerStatus(vo.getIpmiPowerStatus().toString());
    }

    public static HostIpmiInventory valueOf(HostIpmiVO vo) {
        return new HostIpmiInventory(vo);
    }

    public static List<HostIpmiInventory> valueOf1(Collection<HostIpmiVO> vos) {
        List<HostIpmiInventory> invs = new ArrayList<HostIpmiInventory>(vos.size());
        for (HostIpmiVO vo : vos) {
            invs.add(HostIpmiInventory.valueOf(vo));
        }
        return invs;
    }

    public HostIpmiInventory() {
    }

    public String getUuid() {
        return uuid;
    }

    void setUuid(String $paramName) {
        uuid = $paramName;
    }

    public String getIpmiAddress() {
        return ipmiAddress;
    }

    void setIpmiAddress(String $paramName) {
        ipmiAddress = $paramName;
    }

    public String getIpmiUsername() {
        return ipmiUsername;
    }

    void setIpmiUsername(String $paramName) {
        ipmiUsername = $paramName;
    }

    public int getIpmiPort() {
        return ipmiPort;
    }

    void setIpmiPort(int $paramName) {
        ipmiPort = $paramName;
    }

    public String getIpmiPassword() {
        return ipmiPassword;
    }

    void setIpmiPassword(String $paramName) {
        ipmiPassword = $paramName;
    }

    public String getIpmiPowerStatus() {
        return ipmiPowerStatus;
    }

    void setIpmiPowerStatus(String $paramName) {
        ipmiPowerStatus = $paramName;
    }
}
