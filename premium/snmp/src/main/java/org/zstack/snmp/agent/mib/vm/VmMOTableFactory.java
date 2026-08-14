package org.zstack.snmp.agent.mib.vm;

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
import org.zstack.header.vm.*;
import org.zstack.snmp.SnmpConstants;
import org.zstack.snmp.agent.SnmpAgentVO;
import org.zstack.snmp.agent.mib.MOTableFactory;
import org.zstack.snmp.agent.mib.SnmpAgentStatus;
import org.zstack.zwatch.namespace.VmNamespace;

import java.util.List;

public class VmMOTableFactory implements MOTableFactory, Component, VmAfterExpungeExtensionPoint, VmInstanceStartNewCreatedVmExtensionPoint {
    @Autowired
    private EventFacade evtf;
    @Autowired
    private VmMOColumnFactory columnsFactory;
    @Autowired
    private DatabaseFacade dbf;

    private VmMOEntry moTable;

    public VmMOTableFactory() {
    }

    @Override
    public VmMOEntry createMOTable() {
        final OID oid = new OID(SnmpConstants.CLOUD_SNMP_VM_ENTRY_OID);;
        final MOTableIndex index = new MOTableIndex(new MOTableSubIndex[]
                {new MOTableSubIndex(SMIConstants.SYNTAX_OCTET_STRING, 32, 32)},
                false);
        final MOColumn[] columns = columnsFactory.getColumns();
        VmMOEntry table =  new VmMOEntry(oid, index, columns);
        List<VmInstanceVO> vmList = Q.New(VmInstanceVO.class).list();
        for (VmInstanceVO vm : vmList) {
            table.addRow(vm.getUuid());
        }
        moTable = table;
        return moTable;
    }

    @Override
    public String getType() {
        return VmNamespace.NAME;
    }

    @Override
    public String getOID() {
        return SnmpConstants.CLOUD_SNMP_VM_ENTRY_OID;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void vmAfterExpunge(VmInstanceInventory inv) {
        if (!inv.getType().equals(VmInstanceConstant.USER_VM_TYPE)) {
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
        moTable.removeRow(inv.getUuid());
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
        if (!inv.getType().equals(VmInstanceConstant.USER_VM_TYPE)) {
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
}
