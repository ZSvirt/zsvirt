package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConnectionReestablishExtensionPoint;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.CheckL2NetworkOnHostMsg;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.PrepareL2NetworkOnHostMsg;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.network.l2.vxlan.vtep.PopulateVtepPeersMsg;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by weiwang on 10/05/2017.
 */
public class KVMConnectExtensionForVxlanNetwork implements KVMHostConnectExtensionPoint, HostConnectionReestablishExtensionPoint {

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private KVMRealizeL2VxlanNetworkBackend kvmRealizeL2VxlanNetworkBackend;
    @Autowired
    private CloudBus bus;

    @Transactional(readOnly = true)
    private List<L2NetworkInventory> getL2Networks(String clusterUuid) {
        String sql = "select l2.uuid from L2NetworkVO l2, L2NetworkClusterRefVO ref where l2.uuid = ref.l2NetworkUuid and ref.clusterUuid = :clusterUuid and l2.type = :type";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("clusterUuid", clusterUuid);
        q.setParameter("type", VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE);
        List<String> l2uuids = q.getResultList();
        List<L2NetworkInventory> ret = new ArrayList<L2NetworkInventory>(l2uuids.size());
        for (String l2uuid : l2uuids) {
            VxlanNetworkPoolVO poolvo = dbf.getEntityManager().find(VxlanNetworkPoolVO.class, l2uuid);
            ret.add(L2VxlanNetworkPoolInventory.valueOf(poolvo));
        }
        return ret;
    }

    private void prepareNetwork(final Iterator<String> it, HostInventory hostInventory, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final String l2Uuid = it.next();
        PrepareL2NetworkOnHostMsg pmsg = new PrepareL2NetworkOnHostMsg();
        pmsg.setHost(hostInventory);
        pmsg.setL2NetworkUuid(l2Uuid);
        bus.makeTargetServiceIdByResourceUuid(pmsg, L2NetworkConstant.SERVICE_ID, l2Uuid);
        bus.send(pmsg, new CloudBusCallBack(completion) {
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    prepareNetwork(it, hostInventory, completion);
                }

            }
        });
    }

    @Override
    public void connectionReestablished(HostInventory inv) throws HostException {
        //TODO: make connect async
        List<String> l2s = getL2Networks(inv.getClusterUuid()).stream().map(L2NetworkInventory::getUuid).collect(Collectors.toList());
        if (l2s.isEmpty()) {
            return;
        }

        FutureCompletion completion = new FutureCompletion(null);
        prepareNetwork(l2s.iterator(), inv, completion);
        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    @Override
    public HypervisorType getHypervisorTypeForReestablishExtensionPoint() {
        return new HypervisorType(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "prepare-vxlan-network";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                List<String> l2s = getL2Networks(context.getInventory().getClusterUuid()).stream().map(L2NetworkInventory::getUuid).collect(Collectors.toList());
                if (l2s.isEmpty()) {
                    trigger.next();
                    return;
                }

                prepareNetwork(l2s.iterator(), context.getInventory(), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        };
    }
}
