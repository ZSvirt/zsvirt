package org.zstack.snmp.agent.mib.vm;

import org.snmp4j.agent.mo.DefaultMOMutableTableModel;
import org.snmp4j.agent.mo.DefaultMOTable;
import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.agent.mo.MOTableIndex;
import org.snmp4j.smi.OID;
import org.zstack.snmp.SnmpUtils;

public class VmMOEntry extends DefaultMOTable<VmMOTableRow, MOColumn,
        DefaultMOMutableTableModel<VmMOTableRow>>{

    public VmMOEntry(OID oid, MOTableIndex indexDef, MOColumn[] columns) {
        super(oid, indexDef, columns);
    }

    public boolean addRow(String uuid) {
        VmMOTableRow row = this.getModel().getRow(SnmpUtils.transformUuidToIndexOID(uuid));
        if (row != null) {
            return true;
        }
        return this.addRow(new VmMOTableRow(SnmpUtils.transformUuidToIndexOID(uuid)));
    }

    public boolean removeRow(String uuid) {
        return this.removeRow(SnmpUtils.transformUuidToIndexOID(uuid)) != null;
    }
}
