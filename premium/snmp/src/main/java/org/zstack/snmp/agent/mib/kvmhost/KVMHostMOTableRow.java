package org.zstack.snmp.agent.mib.kvmhost;

import org.snmp4j.agent.mo.MOTableRow;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.snmp.SnmpUtils;
import org.zstack.snmp.agent.mib.SnmpGetHandler;
import org.zstack.zwatch.namespace.KVMHostNamespace;

/**
 * @Author : jingwang
 * @create 2023/8/10 17:47
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class KVMHostMOTableRow implements MOTableRow<OctetString> {
    @Autowired
    TimeHelper timeHelper;
    @Autowired
    KVMHostMOColumnFactory columnFactory;
    private OID index;

    public KVMHostMOTableRow(OID index) {
        this.index = index;
    }
    @Override
    public OID getIndex() {
        return index;
    }

    @Override
    public OctetString getValue(int column) {
        String metricName = columnFactory.getColumnName(column);
        String uuid = SnmpUtils.transformIndexOIDToUuid(index);
        SnmpGetHandler handler = columnFactory.getHandler(metricName);
        try {
            return handler.handle(KVMHostNamespace.NAME, metricName, uuid);
        } catch (Exception e) {
            return new OctetString(String.format("Get ZStack/%s/%s[%s] failed.", KVMHostNamespace.NAME, metricName, uuid));
        }
    }

    @Override
    public MOTableRow getBaseRow() {
        return null;
    }

    @Override
    public void setBaseRow(MOTableRow baseRow) {

    }

    @Override
    public int size() {
        return columnFactory.getColumns().length;
    }
}
