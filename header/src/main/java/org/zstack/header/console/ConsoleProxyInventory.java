package org.zstack.header.console;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 7:26 PM
 * To change this template use File | Settings | File Templates.
 */
@Inventory(mappingVOClass = ConsoleProxyVO.class)
@PythonClassInventory
public class ConsoleProxyInventory {
    private String uuid;
    private String vmInstanceUuid;
    private String agentIp;
    private String token;
    private String agentType;
    private String proxyHostname;
    private Integer proxyPort;
    private String targetSchema;
    private String targetHostname;
    private Integer targetPort;
    private String scheme;
    private String proxyIdentity;
    private String status;
    private String version;
    private Timestamp expiredDate;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ConsoleProxyInventory valueOf(ConsoleProxyVO vo) {
        ConsoleProxyInventory inv = new ConsoleProxyInventory();
        inv.setUuid(vo.getUuid());
        inv.setVmInstanceUuid(vo.getVmInstanceUuid());
        inv.setAgentIp(vo.getAgentIp());
        inv.setAgentType(vo.getAgentType());
        inv.setScheme(vo.getScheme());
        inv.setToken(vo.getToken());
        inv.setProxyHostname(vo.getProxyHostname());
        inv.setProxyPort(vo.getProxyPort());
        inv.setTargetSchema(vo.getTargetSchema());
        inv.setTargetHostname(vo.getTargetHostname());
        inv.setTargetPort(vo.getTargetPort());
        inv.setProxyIdentity(vo.getProxyIdentity());
        inv.setCreateDate(vo.getCreateDate());
        inv.setVersion(vo.getVersion());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setStatus(vo.getStatus().toString());
        inv.setExpiredDate(vo.getExpiredDate());
        return inv;
    }

    public static List<ConsoleProxyInventory> valueOf(Collection<ConsoleProxyVO> vos) {
        List<ConsoleProxyInventory> invs = new ArrayList<ConsoleProxyInventory>();
        for (ConsoleProxyVO vo : vos) {
            invs.add(ConsoleProxyInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getProxyIdentity() {
        return proxyIdentity;
    }

    public void setProxyIdentity(String proxyIdentity) {
        this.proxyIdentity = proxyIdentity;
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

    public String getAgentIp() {
        return agentIp;
    }

    public void setAgentIp(String agentIp) {
        this.agentIp = agentIp;
    }

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getProxyHostname() {
        return proxyHostname;
    }

    public void setProxyHostname(String proxyHostname) {
        this.proxyHostname = proxyHostname;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getTargetSchema() {
        return targetSchema;
    }

    public void setTargetSchema(String targetSchema) {
        this.targetSchema = targetSchema;
    }

    public String getTargetHostname() {
        return targetHostname;
    }

    public void setTargetHostname(String targetHostname) {
        this.targetHostname = targetHostname;
    }

    public Integer getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(Integer targetPort) {
        this.targetPort = targetPort;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Timestamp getExpiredDate() {
        return expiredDate;
    }

    public void setExpiredDate(Timestamp expiredDate) {
        this.expiredDate = expiredDate;
    }
}
