package org.zstack.kvm;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.GsonTransient;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = KVMHostVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = HostInventory.class, type = KVMConstant.KVM_HYPERVISOR_TYPE)})
public class KVMHostInventory extends HostInventory {
    /**
     * @ignore
     */
    private String username;
    /**
     * @ignore
     */
    @GsonTransient
    @APINoSee
    private String password;

    private Integer sshPort;

    private String osDistribution;

    private String osRelease;

    private String osVersion;

    private String iscsiInitiatorName;

    protected KVMHostInventory(KVMHostVO vo) {
        super(vo);
        this.setUsername(vo.getUsername());
        this.setPassword(vo.getPassword());
        this.setSshPort(vo.getPort());
        this.setOsDistribution(vo.getOsDistribution());
        this.setOsRelease(vo.getOsRelease());
        this.setOsVersion(vo.getOsVersion());
        this.setIscsiInitiatorName(vo.getIscsiInitiatorName());
    }

    public KVMHostInventory() {
    }

    public static KVMHostInventory valueOf(KVMHostVO vo) {
        return new KVMHostInventory(vo);
    }

    public static List<KVMHostInventory> valueOf1(Collection<KVMHostVO> vos) {
        List<KVMHostInventory> invs = new ArrayList<KVMHostInventory>();
        for (KVMHostVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
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
