package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.APICreateL3NetworkMsg;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO_;
import org.zstack.utils.network.NetworkUtils;

import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.*;

/**
 * Created by weiwang on 02/05/2017.
 */
public class VxlanNetworkCheckerImpl implements VxlanNetworkChecker {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            validate((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APICreateL3NetworkMsg) {
            validate((APICreateL3NetworkMsg) msg);
        } else if (msg instanceof APIUpdateL2NetworkVirtualNetworkIdMsg) {
            validate((APIUpdateL2NetworkVirtualNetworkIdMsg) msg);
        }

        return msg;
    }

    private void validate(APIUpdateL2NetworkVirtualNetworkIdMsg msg) {
        VxlanNetworkVO vxlanVO = Q.New(VxlanNetworkVO.class).eq(VxlanNetworkVO_.uuid, msg.getL2NetworkUuid()).find();
        if (vxlanVO == null) {
            return;
        }
        if (!NetworkUtils.isValidVni(msg.getVirtualNetworkId())) {
            throw new ApiMessageInterceptionException(argerr("vni[%s] for vxlan is invalid", msg.getVirtualNetworkId()));
        }

        boolean duplicated = Q.New(VxlanNetworkVO.class)
                .eq(VxlanNetworkVO_.vni, msg.getVirtualNetworkId())
                .notEq(VxlanNetworkVO_.uuid, vxlanVO.getUuid())
                .eq(VxlanNetworkVO_.poolUuid, vxlanVO.getPoolUuid()).isExists();
        if (duplicated) {
            throw new OperationFailureException(Platform.err(L2Errors.ALLOCATE_VNI_ERROR,
                    "cannot allocate vni[%s] in vxlan network[uuid:%s] which is already allocated",
                    msg.getVirtualNetworkId(), msg.getL2NetworkUuid()));
        }
    }

    private void validate(APIAttachL2NetworkToClusterMsg msg) {
        L2NetworkVO l2NetworkVO = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, msg.getL2NetworkUuid()).find();
        if (!l2NetworkVO.getType().equals(VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE)) {
            return;
        }

        if (msg.getSystemTags() == null) {
            throw new ApiMessageInterceptionException(argerr("need to input one system tag like : [%s]",
                    VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.getTagFormat()));
        }

        validateSystemTagFormat(msg.getSystemTags());

        validateVniRangeOverlap(L2NetworkInventory.valueOf(l2NetworkVO), msg.getClusterUuid());
    }

    public void validateSystemTagFormat(List<String> systemTags) {
        for (String tag : systemTags) {
            if (!VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.isMatch(tag)) {
                throw new ApiMessageInterceptionException(argerr("wrong system tag [%s], should be like : [%s]",
                        tag, VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.getTagFormat()));
            }
            List<String> cidr = Arrays.asList(VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.getTokenByTag(tag, VxlanSystemTags.VTEP_CIDR_TOKEN).split("[{}]"));
            boolean isCidr = cidr.size() > 1 && NetworkUtils.isCidr(cidr.get(1));
            if (!isCidr) {
                throw new ApiMessageInterceptionException(argerr("wrong cidr format in system tag [%s]", tag));
            }
        }
    }


    public void validateVniRangeOverlap(L2NetworkInventory inv, String clusterUuid) {
        String overlappedPool = getOverlapVniRangePool(inv, clusterUuid);
        if (overlappedPool != null) {
            throw new ApiMessageInterceptionException(argerr("overlap vni range with %s [%s]", inv.getType(), overlappedPool));
        }
    }

    public String getOverlapVniRangePool(L2NetworkInventory inv, String clusterUuid) {
        List<VniRangeVO> checkRanges = Q.New(VniRangeVO.class).eq(VniRangeVO_.l2NetworkUuid, inv.getUuid()).list();
        List<String> l2Uuids = Q.New(L2NetworkClusterRefVO.class).select(L2NetworkClusterRefVO_.l2NetworkUuid)
                .eq(L2NetworkClusterRefVO_.clusterUuid, clusterUuid).notEq(L2NetworkClusterRefVO_.l2NetworkUuid, inv.getUuid()).listValues();

        for (String l2Uuid : l2Uuids) {
            L2NetworkVO l2 = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l2Uuid).find();
            if (!l2.getType().equals(inv.getType())){
                continue;
            }

            List<VniRangeVO> ranges = Q.New(VniRangeVO.class).eq(VniRangeVO_.l2NetworkUuid, l2Uuid).list();
            for (VniRangeVO range : ranges) {
                for (VniRangeVO cRange: checkRanges) {
                    if (isVniRangeOverlap(range.getStartVni(), range.getEndVni(), cRange.getStartVni(), cRange.getEndVni())) {
                        return l2Uuid;
                    }
                }
            }
        }

        return null;
    }

    private static boolean isVniRangeOverlap(Integer startVni1, Integer endVni1, Integer startVni2, Integer endVni2) {
        return (startVni1 >= startVni2 && startVni1 <= endVni2) || (startVni1 <= startVni2 && startVni2 <= endVni1);
    }

    private void validate(APICreateL3NetworkMsg msg) {
        if (msg.getL2NetworkUuid() == null) {
            return;
        }

        String type = Q.New(L2NetworkVO.class).select(L2NetworkVO_.type).eq(L2NetworkVO_.uuid, msg.getL2NetworkUuid()).findValue();
        if (type.equals(VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE)) {
            throw new ApiMessageInterceptionException(argerr("vxlan network pool doesn't support create l3 network"));
        }
    }
}
