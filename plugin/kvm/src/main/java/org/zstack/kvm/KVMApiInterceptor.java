package org.zstack.kvm;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.APIAttachL2NetworkToClusterMsg;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2VlanNetworkVO;
import org.zstack.utils.network.NetworkUtils;

import java.util.List;

import static org.zstack.core.Platform.argerr;

/**
 */
public class KVMApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddKVMHostMsg) {
            validate((APIAddKVMHostMsg) msg);
        } else if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            validate((APIAttachL2NetworkToClusterMsg) msg);
        }
        return msg;
    }


    private void validate(APIAddKVMHostMsg msg) {
        SimpleQuery<KVMHostVO> q = dbf.createQuery(KVMHostVO.class);
        q.add(KVMHostVO_.managementIp, Op.EQ, msg.getManagementIp());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(argerr("there has been a kvm host having management ip[%s]", msg.getManagementIp()));
        }
    }

    // if attach l2 vlan network to kvm cluster, then check its interface length
    private void validate(APIAttachL2NetworkToClusterMsg msg) {
        ClusterVO cls = dbf.findByUuid(msg.getClusterUuid(), ClusterVO.class);
        if (cls != null && !cls.getHypervisorType().equals(KVMConstant.KVM_HYPERVISOR_TYPE)) {
            return;
        }

        L2VlanNetworkVO l2 = dbf.findByUuid(msg.getL2NetworkUuid(), L2VlanNetworkVO.class);
        if (l2 == null) {
            return;
        }

        if (NetworkUtils.generateVlanDeviceName(l2.getPhysicalInterface(), l2.getVlan()).length()
                > L2NetworkConstant.LINUX_IF_NAME_MAX_SIZE) {
            throw new ApiMessageInterceptionException(argerr("cannot create vlan-device on %s because it's too long"
                    , l2.getPhysicalInterface()));
        }
    }


    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
            APIAttachL2NetworkToClusterMsg.class,
            APIAddKVMHostMsg.class
        );
    }

    @Override
    public GlobalApiMessageInterceptor.InterceptorPosition getPosition() {
        return GlobalApiMessageInterceptor.InterceptorPosition.END;
    }

}
