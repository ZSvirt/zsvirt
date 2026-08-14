package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.AutoOffEventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostCanonicalEvents.HostStatusChangedData;
import org.zstack.header.host.HostErrors;
import org.zstack.header.host.HostStatus;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KvmCommandSender;
import org.zstack.kvm.KvmCommandSender.SteppingSendCallback;
import org.zstack.kvm.KvmResponseWrapper;
import org.zstack.network.service.flat.FlatDhcpBackend.DeleteNamespaceCmd;
import org.zstack.network.service.flat.FlatDhcpBackend.DeleteNamespaceRsp;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2016/6/25.
 */
public class FlatDhcpUpgradeExtension implements Component {
    private static final CLogger logger = Utils.getLogger(FlatDhcpUpgradeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private ErrorFacade errf;

    class L3Host {
        String hostUuid;
        L3NetworkInventory l3;
    }

    @Transactional(readOnly = true)
    private List<L3Host> findL3NeedToDeleteDeprecatedNameSpace() {
        String sql = "select l3.uuid from L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO provider" +
                " where l3.uuid = ref.l3NetworkUuid and ref.networkServiceProviderUuid = provider.uuid" +
                " and ref.networkServiceType = :nsType and provider.type = :ptype";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("nsType", NetworkServiceType.DHCP.toString());
        q.setParameter("ptype", FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        List<String> l3Uuids = q.getResultList();

        if (l3Uuids.isEmpty()) {
            return null;
        }

        sql = "select l3, host.uuid from L3NetworkVO l3, HostVO host, L2NetworkClusterRefVO ref where host.hypervisorType = :htype and l3.uuid in (:uuids)" +
                " and ref.l2NetworkUuid = l3.l2NetworkUuid and host.clusterUuid = ref.clusterUuid";
        TypedQuery<Tuple> tq = dbf.getEntityManager().createQuery(sql, Tuple.class);
        tq.setParameter("htype", KVMConstant.KVM_HYPERVISOR_TYPE);
        tq.setParameter("uuids", l3Uuids);
        List<Tuple> ts = tq.getResultList();

        if (ts.isEmpty()) {
            return null;
        }

        List<L3Host> ret = new ArrayList<>();
        for (Tuple t : ts) {
            L3NetworkVO l3 = t.get(0, L3NetworkVO.class);
            String huuid = t.get(1, String.class);

            L3Host lh = new L3Host();
            lh.hostUuid= huuid;
            lh.l3 = L3NetworkInventory.valueOf(l3);
            ret.add(lh);
        }

        return ret;
    }

    @Override
    public boolean start() {
        if (FlatNetworkGlobalProperty.DELETE_DEPRECATED_DHCP_NAME_SPACE) {
            deleteDeprecatedDHCPNameSpace();
        }

        return true;
    }

    private void deleteDeprecatedDHCPNameSpace() {
        List<L3Host> l3Hosts = findL3NeedToDeleteDeprecatedNameSpace();
        if (l3Hosts == null) {
            return;
        }

        logger.debug(String.format("will delete deprecated DHCP namespace on %s hosts", l3Hosts.size()));

        for (L3Host l3Host : l3Hosts) {
            evtf.onLocal(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, new AutoOffEventCallback() {
                @Override
                protected boolean run(Map tokens, Object data) {
                    HostStatusChangedData d = (HostStatusChangedData) data;
                    if (!HostStatus.Connected.toString().equals(d.getNewStatus())) {
                        return false;
                    }

                    L3NetworkInventory l3 = l3Host.l3;

                    String brName = new BridgeNameFinder().findByL3UuidOnHost(l3.getUuid(), l3Host.hostUuid);
                    DeleteNamespaceCmd cmd = new DeleteNamespaceCmd();
                    cmd.bridgeName = brName;
                    cmd.namespaceName = brName;

                    new KvmCommandSender(l3Host.hostUuid).send(cmd, FlatDhcpBackend.DHCP_DELETE_NAMESPACE_PATH, wrapper -> {
                        DeleteNamespaceRsp rsp = wrapper.getResponse(DeleteNamespaceRsp.class);
                        return rsp.isSuccess() ? null : operr("operation error, because:%s", rsp.getError());
                    }, new SteppingSendCallback<KvmResponseWrapper>() {
                        @Override
                        public void success(KvmResponseWrapper w) {
                            logger.debug(String.format("successfully deleted namespace for L3 network[uuid:%s, name:%s] on the " +
                                    "KVM host[uuid:%s]", l3.getUuid(), l3.getName(), getHostUuid()));
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            if (!errorCode.isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE)) {
                                return;
                            }

                            FlatDHCPDeleteNamespaceGC gc = new FlatDHCPDeleteNamespaceGC();
                            gc.hostUuid = getHostUuid();
                            gc.command = cmd;
                            gc.NAME = String.format("gc-namespace-on-host-%s", getHostUuid());
                            gc.submit();
                        }
                    });

                    return true;
                }
            });
        }
    }

    @Override
    public boolean stop() {
        return true;
    }
}
