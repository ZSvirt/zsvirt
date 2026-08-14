package org.zstack.snmp.agent.mib.vrouter;

import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.agent.mo.MOTableIndex;
import org.snmp4j.agent.mo.MOTableSubIndex;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.SMIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceDestroyExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceStartNewCreatedVmExtensionPoint;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO_;
import org.zstack.snmp.SnmpConstants;
import org.zstack.snmp.agent.SnmpAgentVO;
import org.zstack.snmp.agent.mib.MOTableFactory;
import org.zstack.snmp.agent.mib.SnmpAgentStatus;
import org.zstack.zwatch.namespace.VRouterNamespace;

import java.util.List;

public class VRouterMOTableFactory implements MOTableFactory, Component, VmInstanceDestroyExtensionPoint, VmInstanceStartNewCreatedVmExtensionPoint {
    @Autowired
    private EventFacade evtf;
    @Autowired
    private VRouterMOColumnFactory columnsFactory;
    @Autowired
    private DatabaseFacade dbf;

    private VRouterMOEntry moTable;

    public VRouterMOTableFactory() {
    }

    @Override
    public VRouterMOEntry createMOTable() {
        final OID oid = new OID(SnmpConstants.CLOUD_SNMP_VROUTER_ENTRY_OID);;
        final MOTableIndex index = new MOTableIndex(new MOTableSubIndex[]
                {new MOTableSubIndex(SMIConstants.SYNTAX_OCTET_STRING, 32, 32)},
                false);
        final MOColumn[] columns = columnsFactory.getColumns();
        VRouterMOEntry table =  new VRouterMOEntry(oid, index, columns);
        List<VirtualRouterVmVO> vrList = Q.New(VirtualRouterVmVO.class).list();
        for (VirtualRouterVmVO vr : vrList) {
            table.addRow(vr.getUuid());
        }
        moTable = table;
        return moTable;
    }

    @Override
    public String getType() {
        return VRouterNamespace.NAME;
    }

    @Override
    public String getOID() {
        return SnmpConstants.CLOUD_SNMP_VROUTER_ENTRY_OID;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private boolean isVirtualRouterVm(String vmUuid) {
        return Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vmUuid).isExists();
    }

    @Override
    public String preStartNewCreatedVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStartNewCreatedVm(VmInstanceInventory inv) {

    }

    @Override
    public void afterStartNewCreatedVm(VmInstanceInventory inv) {
        if (!isVirtualRouterVm(inv.getUuid())) {
            return;
        }
        if (moTable == null) {
            return;
        }
        List<SnmpAgentVO> lst = dbf.listAll(SnmpAgentVO.class);
        if (lst.isEmpty()) {
            return;
        }
        if (lst.get(0).getStatus() == SnmpAgentStatus.Disable) {
            return;
        }
        moTable.addRow(inv.getUuid());
    }

    @Override
    public void failedToStartNewCreatedVm(VmInstanceInventory inv, ErrorCode reason) {

    }

    @Override
    public String preDestroyVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeDestroyVm(VmInstanceInventory inv) {

    }

    @Override
    public void afterDestroyVm(VmInstanceInventory inv) {
        if (moTable == null) {
            return;
        }
        List<SnmpAgentVO> lst = dbf.listAll(SnmpAgentVO.class);
        if (lst.isEmpty()) {
            return;
        }
        if (lst.get(0).getStatus() == SnmpAgentStatus.Disable) {
            return;
        }
        moTable.removeRow(inv.getUuid());
    }

    @Override
    public void failedToDestroyVm(VmInstanceInventory inv, ErrorCode reason) {

    }
}
