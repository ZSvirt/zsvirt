package org.zstack.snmp.agent.mib.primaryStorage;

import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.agent.mo.MOTableIndex;
import org.snmp4j.agent.mo.MOTableSubIndex;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.SMIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.storage.primary.*;
import org.zstack.snmp.SnmpConstants;
import org.zstack.snmp.agent.SnmpAgentVO;
import org.zstack.snmp.agent.mib.MOTableFactory;
import org.zstack.snmp.agent.mib.SnmpAgentStatus;
import org.zstack.zwatch.namespace.PrimaryStorageNamespace;

import java.util.List;
import java.util.Map;

public class PrimaryStorageMOTableFactory implements MOTableFactory, Component, PrimaryStorageAttachExtensionPoint {
    @Autowired
    private EventFacade evtf;
    @Autowired
    private PrimaryStorageMOColumnFactory columnsFactory;
    @Autowired
    private DatabaseFacade dbf;

    private PrimaryStorageMOEntry moTable;

    public PrimaryStorageMOTableFactory() {
    }

    @Override
    public PrimaryStorageMOEntry createMOTable() {
        final OID oid = new OID(SnmpConstants.CLOUD_SNMP_PRIMARY_STORAGE_ENTRY_OID);;
        final MOTableIndex index = new MOTableIndex(new MOTableSubIndex[]
                {new MOTableSubIndex(SMIConstants.SYNTAX_OCTET_STRING, 32, 32)},
                false);
        final MOColumn[] columns = columnsFactory.getColumns();
        PrimaryStorageMOEntry table =  new PrimaryStorageMOEntry(oid, index, columns);
        List<PrimaryStorageVO> psList = Q.New(PrimaryStorageVO.class).list();
        for (PrimaryStorageVO ps : psList) {
            table.addRow(ps.getUuid());
        }
        moTable = table;
        return moTable;
    }

    @Override
    public String getType() {
        return PrimaryStorageNamespace.NAME;
    }

    @Override
    public String getOID() {
        return SnmpConstants.CLOUD_SNMP_PRIMARY_STORAGE_ENTRY_OID;
    }

    @Override
    public boolean start() {
        listenOnPsDeleteEvent();
        return true;
    }

    private void listenOnPsDeleteEvent() {
        evtf.on(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_DELETED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
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
                PrimaryStorageCanonicalEvent.PrimaryStorageDeletedData d = (PrimaryStorageCanonicalEvent.PrimaryStorageDeletedData) data;
                moTable.removeRow(d.getInventory().getUuid());
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void preAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) throws PrimaryStorageException {

    }

    @Override
    public void beforeAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void failToAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void afterAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
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
        moTable.addRow(inventory.getUuid());
    }
}
