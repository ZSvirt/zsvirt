package org.zstack.snmp.agent.mib.kvmhost;

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
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostAddExtensionPoint;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostInventory;
import org.zstack.kvm.KVMHostVO;
import org.zstack.snmp.SnmpConstants;
import org.zstack.snmp.agent.SnmpAgentVO;
import org.zstack.snmp.agent.mib.MOTableFactory;
import org.zstack.snmp.agent.mib.SnmpAgentStatus;
import org.zstack.zwatch.namespace.KVMHostNamespace;

import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 *
 * @Author : jingwang
 * @create 2023/7/28 11:23 AM
 */
public class KVMHostMOTableFactory implements MOTableFactory, HostAddExtensionPoint, Component {
    @Autowired
    private EventFacade evtf;
    @Autowired
    private KVMHostMOColumnFactory columnsFactory;
    @Autowired
    private DatabaseFacade dbf;

    private KVMHostMOEntry moTable;

    public KVMHostMOTableFactory() {
    }

    @Override
    public KVMHostMOEntry createMOTable() {
        final OID oid = new OID(SnmpConstants.CLOUD_SNMP_KVM_HOST_ENTRY_OID);;
        final MOTableIndex index = new MOTableIndex(new MOTableSubIndex[]
                {new MOTableSubIndex(SMIConstants.SYNTAX_OCTET_STRING, 32, 32)},
                false);
        final MOColumn[] columns = columnsFactory.getColumns();
        KVMHostMOEntry table =  new KVMHostMOEntry(oid, index, columns);
        List<KVMHostVO> hosts = Q.New(KVMHostVO.class).list();
        for (KVMHostVO host : hosts) {
            table.addRow(host.getUuid());
        }
        moTable = table;
        return moTable;
    }

    @Override
    public String getType() {
        return KVMHostNamespace.NAME;
    }

    @Override
    public String getOID() {
        return SnmpConstants.CLOUD_SNMP_KVM_HOST_ENTRY_OID;
    }

    @Override
    public void beforeAddHost(HostInventory host, Completion completion) {
        completion.success();
    }

    @Override
    public void afterAddHost(HostInventory host, Completion completion) {
        completion.success();
        if (!Objects.equals(host.getHypervisorType(), "KVM")) {
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
        asyncAddKvmHostToMib(host);
    }

    @AsyncThread
    void asyncAddKvmHostToMib(HostInventory host) {
        moTable.addRow(host.getUuid());
    }

    @Override
    public boolean start() {
        listenOnHostDeleteEvent();
        return true;
    }

    private void listenOnHostDeleteEvent() {
        evtf.on(HostCanonicalEvents.HOST_DELETED_PATH, new EventCallback() {
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
                HostCanonicalEvents.HostDeletedData d = (HostCanonicalEvents.HostDeletedData) data;
                HostInventory inv = d.getInventory();
                if (!inv.getHypervisorType().equals("KVM")) {
                    return;
                }
                moTable.removeRow(inv.getUuid());
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }
}
