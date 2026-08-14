package org.zstack.compute.cluster;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.cluster.*;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.APIMessage;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 3:41 PM
 * To change this template use File | Settings | File Templates.
 */
@InterceptorForService("cluster")
public class ClusterApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof ClusterMessage) {
            ClusterMessage cmsg = (ClusterMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, ClusterConstant.SERVICE_ID, cmsg.getClusterUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        setServiceId(msg);

        if (msg instanceof APICreateClusterMsg) {
            validate((APICreateClusterMsg) msg);
        } else if (msg instanceof APIDeleteClusterMsg) {
            validate((APIDeleteClusterMsg) msg);
        } else if (msg instanceof APIUpdateClusterOSMsg) {
            validate((APIUpdateClusterOSMsg) msg);
        }

        return msg;
    }

    private void validate(APICreateClusterMsg msg) {
        if ((msg.getType() != null && msg.getType().equals("baremetal") && !msg.getHypervisorType().equals("baremetal")) ||
                (msg.getHypervisorType().equals("baremetal") && msg.getType() != null && !msg.getType().equals("baremetal"))) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "if cluster type is baremetal, then hypervisorType must be baremetal too, or vice versa"
            ));
        }
    }

    private void validate(APIUpdateClusterOSMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                // we can only update os that uses rpm/yum for now, and we assume all KvmHost are using ZStack ISO
                String type = q(ClusterVO.class)
                        .select(ClusterVO_.hypervisorType)
                        .eq(ClusterVO_.uuid, msg.getUuid())
                        .findValue();
                if (type != null && !type.equals("KVM")) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "only kvm hosts' operating system can be updated, for now"
                    ));
                }

                // all hosts in the cluster must not be in the premaintenance state
                Long premaintain = q(HostVO.class)
                        .eq(HostVO_.clusterUuid, msg.getUuid())
                        .eq(HostVO_.state, HostState.PreMaintenance)
                        .count();
                if (premaintain != 0) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "there are hosts in cluster[uuid:%s] in the PreMaintenance state, cannot update cluster os right now",
                            msg.getUuid()
                    ));
                }

                // all hosts in the cluster must be connected
                Long notConnected = q(HostVO.class)
                        .eq(HostVO_.clusterUuid, msg.getUuid())
                        .notEq(HostVO_.status, HostStatus.Connected)
                        .count();
                if (notConnected != 0) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "not all hosts in cluster[uuid:%s] are in the Connected status, cannot update cluster os right now",
                            msg.getUuid()
                    ));
                }
            }
        }.execute();
    }

    private void validate(APIDeleteClusterMsg msg) {
        if (!dbf.isExist(msg.getUuid(), ClusterVO.class)) {
            APIDeleteClusterEvent evt = new APIDeleteClusterEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }
}
