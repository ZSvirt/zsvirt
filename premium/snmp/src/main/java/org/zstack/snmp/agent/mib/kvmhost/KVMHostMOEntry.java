package org.zstack.snmp.agent.mib.kvmhost;

import org.snmp4j.agent.mo.DefaultMOMutableTableModel;
import org.snmp4j.agent.mo.DefaultMOTable;
import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.agent.mo.MOTableIndex;
import org.snmp4j.smi.OID;
import org.zstack.snmp.SnmpUtils;

/**
 *
 * @Author : jingwang
 * @create 2023/8/10 17:45
 */
public class KVMHostMOEntry extends DefaultMOTable<KVMHostMOTableRow, MOColumn,
        DefaultMOMutableTableModel<KVMHostMOTableRow>>{

    public KVMHostMOEntry(OID oid, MOTableIndex indexDef, MOColumn[] columns) {
        super(oid, indexDef, columns);
    }

    public boolean addRow(String uuid) {
        return this.addRow(new KVMHostMOTableRow(SnmpUtils.transformUuidToIndexOID(uuid)));
    }

    public boolean removeRow(String uuid) {
        return this.removeRow(SnmpUtils.transformUuidToIndexOID(uuid)) != null;
    }
}
