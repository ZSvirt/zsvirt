package org.zstack.network.l2;

import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 4:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2NetworkApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof L2NetworkMessage) {
            L2NetworkMessage l2msg = (L2NetworkMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, l2msg.getL2NetworkUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateL2NetworkMsg) {
            validate((APICreateL2NetworkMsg) msg);
        } else if (msg instanceof APIDeleteL2NetworkMsg) {
            validate((APIDeleteL2NetworkMsg) msg);
        } else if (msg instanceof APIDetachL2NetworkFromClusterMsg) {
            validate((APIDetachL2NetworkFromClusterMsg) msg);
        } else if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            validate((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APIAttachL2NetworkToHostMsg) {
            validate((APIAttachL2NetworkToHostMsg) msg);
        } else if (msg instanceof APIDetachL2NetworkFromHostMsg) {
            validate((APIDetachL2NetworkFromHostMsg) msg);
        } else if (msg instanceof APIUpdateL2NetworkVirtualNetworkIdMsg) {
            validate((APIUpdateL2NetworkVirtualNetworkIdMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    @SuppressWarnings("unchecked")
    private void validate(final APIAttachL2NetworkToClusterMsg msg) {
        SimpleQuery<L2NetworkClusterRefVO> q = dbf.createQuery(L2NetworkClusterRefVO.class);
        q.add(L2NetworkClusterRefVO_.clusterUuid, Op.EQ, msg.getClusterUuid());
        q.add(L2NetworkClusterRefVO_.l2NetworkUuid, Op.EQ, msg.getL2NetworkUuid());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr("l2Network[uuid:%s] has attached to cluster[uuid:%s], can't attach again", msg.getL2NetworkUuid(), msg.getClusterUuid()));
        }

        /* current ovs only support vlan, vxlan*/
        L2NetworkVO l2 = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkVO.class);
        /* find l2 network with same physical interface, but different vswitch Type */
        List<String> otherL2s = Q.New(L2NetworkVO.class).select(L2NetworkVO_.uuid)
                .eq(L2NetworkVO_.physicalInterface, l2.getPhysicalInterface())
                .notEq(L2NetworkVO_.vSwitchType, l2.getvSwitchType()).listValues();
        if (!otherL2s.isEmpty()) {
            if (Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.clusterUuid, msg.getClusterUuid())
                    .in(L2NetworkClusterRefVO_.l2NetworkUuid, otherL2s).isExists()) {
                throw new ApiMessageInterceptionException(argerr("could not attach l2 network, because there " +
                                "is another network [uuid:%s] on physical interface [%s] with different vswitch type",
                        otherL2s.get(0), l2.getPhysicalInterface()));
            }
        }

        //TODO: UI for ovs and macVlan has not use l2ProviderType yet, set to value of vSwitchType
        if (msg.getL2ProviderType() == null && !L2NetworkConstant.VSWITCH_TYPE_LINUX_BRIDGE.equals(l2.getvSwitchType())) {
            msg.setL2ProviderType(l2.getvSwitchType());
        }

        if (msg.getL2ProviderType() != null && !L2ProviderType.hasType(msg.getL2ProviderType())) {
            throw new ApiMessageInterceptionException(argerr("unsupported l2Network provider type[%s]", msg.getL2ProviderType()));
        }

        if (!StringUtils.isEmpty(msg.getHostParams())) {
            List<HostParam> hostParams;
            try {
                hostParams = JSONObjectUtil.toCollection(msg.getHostParams(), ArrayList.class, HostParam.class);
            } catch (JsonSyntaxException e) {
                throw new ApiMessageInterceptionException(argerr("invalid json format, causes: %s", e.getMessage()));
            }

            ErrorCode err = L2NetworkHostUtils.validateHostParams(hostParams, msg.getClusterUuid(), null);
            if (err != null) {
                throw new ApiMessageInterceptionException(err);
            }
        }
    }

    private void validate(APIDetachL2NetworkFromClusterMsg msg) {
        SimpleQuery<L2NetworkClusterRefVO> q = dbf.createQuery(L2NetworkClusterRefVO.class);
        q.add(L2NetworkClusterRefVO_.clusterUuid, Op.EQ, msg.getClusterUuid());
        q.add(L2NetworkClusterRefVO_.l2NetworkUuid, Op.EQ, msg.getL2NetworkUuid());
        if (!q.isExists()) {
            throw new ApiMessageInterceptionException(operr("l2Network[uuid:%s] has not attached to cluster[uuid:%s]", msg.getL2NetworkUuid(), msg.getClusterUuid()));
        }
    }

    private void validate(APIAttachL2NetworkToHostMsg msg) {
        boolean ifL2AttachedCluster = SQL.New("select l2.uuid from L2NetworkVO l2, L2NetworkClusterRefVO ref, HostVO host" +
                        " where l2.uuid = ref.l2NetworkUuid" +
                        " and ref.clusterUuid = host.clusterUuid" +
                        " and l2.uuid = :l2Uuid" +
                        " and host.uuid = :hostUuid")
                .param("l2Uuid", msg.getL2NetworkUuid())
                .param("hostUuid", msg.getHostUuid())
                .find() != null;

        if (!ifL2AttachedCluster) {
            throw new ApiMessageInterceptionException(operr("l2Network[uuid:%s] has not attached to cluster of host[uuid:%s]", msg.getL2NetworkUuid(), msg.getHostUuid()));
        }

        String l2Type = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, msg.getL2NetworkUuid()).select(L2NetworkVO_.type).findValue();
        if (L2NetworkType.valueOf(l2Type).isAttachToAllHosts()) {
            throw new ApiMessageInterceptionException(operr("type[%s] should be attached to all host", l2Type));
        }

        HostVO host = dbf.findByUuid(msg.getHostUuid(), HostVO.class);
        if (asList(HostState.PreMaintenance, HostState.Maintenance).contains(host.getState())) {
            throw new ApiMessageInterceptionException(operr("could not attach l2Network[uuid:%s] to host[uuid:%s] " +
                    "which is in the premaintenance or maintenance state", msg.getL2NetworkUuid(), msg.getHostUuid()));
        }

        if (!StringUtils.isEmpty(msg.getHostParam())) {
            HostParam hostParam;
            try {
                hostParam = JSONObjectUtil.toObject(msg.getHostParam(), HostParam.class);
            } catch (JsonSyntaxException e) {
                throw new ApiMessageInterceptionException(argerr("invalid json format, causes: %s", e.getMessage()));
            }


            ErrorCode err = L2NetworkHostUtils.validateHostParams(Collections.singletonList(hostParam), null, msg.getHostUuid());
            if (err != null) {
                throw new ApiMessageInterceptionException(err);
            }
        }
    }

    private void validate(APIDetachL2NetworkFromHostMsg msg) {
        boolean ifL2AttachedCluster = SQL.New("select l2.uuid from L2NetworkVO l2, L2NetworkClusterRefVO ref, HostVO host" +
                        " where l2.uuid = ref.l2NetworkUuid" +
                        " and ref.clusterUuid = host.clusterUuid" +
                        " and l2.uuid = :l2Uuid" +
                        " and host.uuid = :hostUuid")
                .param("l2Uuid", msg.getL2NetworkUuid())
                .param("hostUuid", msg.getHostUuid())
                .find() != null;

        if (!ifL2AttachedCluster) {
            throw new ApiMessageInterceptionException(operr("l2Network[uuid:%s] has not attached to cluster of host[uuid:%s]", msg.getL2NetworkUuid(), msg.getHostUuid()));
        }

        if (!L2NetworkHostUtils.checkIfL2AttachedToHost(msg.getL2NetworkUuid(), msg.getHostUuid())) {
            throw new ApiMessageInterceptionException(operr("l2Network[uuid:%s] has not attached to host[uuid:%s]", msg.getL2NetworkUuid(), msg.getHostUuid()));
        }
    }

    private void validate(APIDeleteL2NetworkMsg msg) {
        if (!dbf.isExist(msg.getUuid(), L2NetworkVO.class)) {
            APIDeleteL2NetworkEvent evt = new APIDeleteL2NetworkEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APICreateL2NetworkMsg msg) {
        if (!L2NetworkType.hasType(msg.getType())) {
            throw new ApiMessageInterceptionException(argerr("unsupported l2Network type[%s]", msg.getType()));
        }

        if (!VSwitchType.hasType(msg.getvSwitchType())) {
            throw new ApiMessageInterceptionException(argerr("unsupported vSwitch type[%s]", msg.getvSwitchType()));
        }
    }

    private void validate(APIUpdateL2NetworkVirtualNetworkIdMsg msg) {
        L2NetworkVO l2 = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkVO.class);
        if (L2NetworkConstant.L2_VLAN_NETWORK_TYPE.equals(l2.getType()) ||
                L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE.equals(l2.getType())) {
            throw new ApiMessageInterceptionException(operr("l2 network[type:%s] does not support update virtual network id", l2.getType()));
        }

        l2.getAttachedClusterRefs().forEach(ref -> {
            if (!Q.New(ClusterVO.class).eq(ClusterVO_.uuid, ref.getClusterUuid())
                    .eq(ClusterVO_.hypervisorType, VmInstanceConstant.KVM_HYPERVISOR_TYPE).isExists()) {
                throw new ApiMessageInterceptionException(operr("cannot update virtual network id for l2Network[uuid:%s]" +
                        " because it only supports an L2Network that is exclusively attached to a kvm cluster", l2.getUuid()));
            }
        });
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return list(
            APICreateL2NoVlanNetworkMsg.class,
            APIDeleteL2NetworkMsg.class,
            APIAttachL2NetworkToClusterMsg.class,
            APIAttachL2NetworkToHostMsg.class,
            APIDetachL2NetworkFromClusterMsg.class,
            APIDetachL2NetworkFromHostMsg.class,
            APICreateL2VlanNetworkMsg.class,
            APIQueryL2NetworkMsg.class,
            APIGetL2NetworkTypesMsg.class,
            APIGetVSwitchTypesMsg.class,
            APIUpdateL2NetworkMsg.class,
            APIUpdateL2NetworkVirtualNetworkIdMsg.class,
            APIGetCandidateL2NetworksForAttachingClusterMsg.class,
            APIGetCandidateClustersForAttachingL2NetworkMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.DEFAULT;
    }
}
