package org.zstack.vpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.network.l3.*;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.network.l3.L3BasicNetworkFactory;
import org.zstack.network.service.virtualrouter.VirtualRouterNicMetaData;

import java.util.List;

/**
 * Created by weiwang on 18/09/2017
 */
public class VpcL3NetworkFactory extends L3BasicNetworkFactory implements AfterAllocateRequiredIpExtensionPoint,
        UsedIpNotAccountMetaDataExtensionPoint {
    private static final L3NetworkType type = new L3NetworkType.L3NetworkTypeBuilder()
            .typeName(VpcConstants.VPC_L3_NETWORK_TYPE)
            .build();

    @Override
    public L3NetworkType getType() {
        return type;
    }

    @Override
    public IpRangeVO afterAllocateRequiredIp(IpAllocateMessage msg, IpRangeVO allocatedIpRangeVO, List<IpRangeVO> l3IpRanges) {
        L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        // TODO(WEIW): need support router interface specify
        if (l3NetworkVO.getType().equals(type.toString()) &&
                allocatedIpRangeVO == null &&
                l3IpRanges != null &&
                !l3IpRanges.isEmpty() &&
                msg.getRequiredIp().equals(l3IpRanges.get(0).getGateway())) {
            return l3IpRanges.get(0);
        }
        return allocatedIpRangeVO;
    }

    @Override
    public String usedIpNotAccountMetaData() {
        return VirtualRouterNicMetaData.GUEST_NIC_MASK.toString();
    }

    @Override
    public boolean applyNetworkServiceWhenVmStateChange() {
        if (VpcGlobalProperty.UPGRADE_VPC_NETWORK_SERVICE_UT) {
            /* this branch is used for unit test */
            return true;
        }

        return false;
    }
}
