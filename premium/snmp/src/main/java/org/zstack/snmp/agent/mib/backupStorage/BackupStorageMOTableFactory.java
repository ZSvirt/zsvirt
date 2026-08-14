package org.zstack.snmp.agent.mib.backupStorage;

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
import org.zstack.header.storage.backup.*;
import org.zstack.snmp.SnmpConstants;
import org.zstack.snmp.agent.SnmpAgentVO;
import org.zstack.snmp.agent.mib.MOTableFactory;
import org.zstack.snmp.agent.mib.SnmpAgentStatus;
import org.zstack.zwatch.namespace.BackupStorageNamespace;

import java.util.List;

public class BackupStorageMOTableFactory implements MOTableFactory, Component, AddBackupStorageExtensionPoint, BackupStorageDeleteExtensionPoint {
    @Autowired
    private EventFacade evtf;
    @Autowired
    private BackupStorageMOColumnFactory columnsFactory;
    @Autowired
    private DatabaseFacade dbf;

    private BackupStorageMOEntry moTable;

    public BackupStorageMOTableFactory() {
    }

    @Override
    public BackupStorageMOEntry createMOTable() {
        final OID oid = new OID(SnmpConstants.CLOUD_SNMP_BACKUP_STORAGE_ENTRY_OID);;
        final MOTableIndex index = new MOTableIndex(new MOTableSubIndex[]
                {new MOTableSubIndex(SMIConstants.SYNTAX_OCTET_STRING, 32, 32)},
                false);
        final MOColumn[] columns = columnsFactory.getColumns();
        BackupStorageMOEntry table =  new BackupStorageMOEntry(oid, index, columns);
        List<BackupStorageVO> bsList = Q.New(BackupStorageVO.class).list();
        for (BackupStorageVO bs : bsList) {
            table.addRow(bs.getUuid());
        }
        moTable = table;
        return moTable;
    }

    @Override
    public String getType() {
        return BackupStorageNamespace.NAME;
    }

    @Override
    public String getOID() {
        return SnmpConstants.CLOUD_SNMP_BACKUP_STORAGE_ENTRY_OID;
    }

    @Override
    public boolean start() {
        listenOnBsDeleteEvent();
        return true;
    }

    private void listenOnBsDeleteEvent() {
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void preAddBackupStorage(AddBackupStorageStruct backupStorage) {
    }

    @Override
    public void beforeAddBackupStorage(AddBackupStorageStruct backupStorage) {
    }

    @Override
    public void afterAddBackupStorage(AddBackupStorageStruct backupStorage) {
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
        moTable.addRow(backupStorage.getBackupStorageInventory().getUuid());
    }

    @Override
    public void failedToAddBackupStorage(AddBackupStorageStruct backupStorage, ErrorCode err) {
    }

    @Override
    public void preDeleteSecondaryStorage(BackupStorageInventory inv) throws BackupStorageException {
    }

    @Override
    public void beforeDeleteSecondaryStorage(BackupStorageInventory inv) {
    }

    @Override
    public void afterDeleteSecondaryStorage(BackupStorageInventory inv) {
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
}
