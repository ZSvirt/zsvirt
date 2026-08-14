package org.zstack.snmp.agent.mib.vrouter;

import org.snmp4j.agent.mo.DefaultMOMutableTableModel;
import org.snmp4j.agent.mo.DefaultMOTable;
import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.agent.mo.MOTableIndex;
import org.snmp4j.smi.OID;
import org.zstack.snmp.SnmpUtils;

public class VRouterMOEntry extends DefaultMOTable<VRouterMOTableRow, MOColumn,
        DefaultMOMutableTableModel<VRouterMOTableRow>>{

    public VRouterMOEntry(OID oid, MOTableIndex indexDef, MOColumn[] columns) {
        super(oid, indexDef, columns);
    }

    public boolean addRow(String uuid) {
        VRouterMOTableRow row = this.getModel().getRow(SnmpUtils.transformUuidToIndexOID(uuid));
        if (row != null) {
            return true;
        }
        return this.addRow(new VRouterMOTableRow(SnmpUtils.transformUuidToIndexOID(uuid)));
    }

    public boolean removeRow(String uuid) {
        return this.removeRow(SnmpUtils.transformUuidToIndexOID(uuid)) != null;
    }
}
