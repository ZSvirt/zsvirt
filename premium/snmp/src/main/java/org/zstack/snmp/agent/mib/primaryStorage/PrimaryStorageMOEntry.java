package org.zstack.snmp.agent.mib.primaryStorage;

import org.snmp4j.agent.mo.DefaultMOMutableTableModel;
import org.snmp4j.agent.mo.DefaultMOTable;
import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.agent.mo.MOTableIndex;
import org.snmp4j.smi.OID;
import org.zstack.snmp.SnmpUtils;

public class PrimaryStorageMOEntry extends DefaultMOTable<PrimaryStorageMOTableRow, MOColumn,
        DefaultMOMutableTableModel<PrimaryStorageMOTableRow>>{

    public PrimaryStorageMOEntry(OID oid, MOTableIndex indexDef, MOColumn[] columns) {
        super(oid, indexDef, columns);
    }

    public boolean addRow(String uuid) {
        PrimaryStorageMOTableRow row = this.getModel().getRow(SnmpUtils.transformUuidToIndexOID(uuid));
        if (row != null) {
            return true;
        }
        return this.addRow(new PrimaryStorageMOTableRow(SnmpUtils.transformUuidToIndexOID(uuid)));
    }

    public boolean removeRow(String uuid) {
        return this.removeRow(SnmpUtils.transformUuidToIndexOID(uuid)) != null;
    }
}
