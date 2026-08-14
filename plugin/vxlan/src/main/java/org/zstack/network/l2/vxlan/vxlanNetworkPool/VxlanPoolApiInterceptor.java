package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.network.l2.vxlan.vtep.APICreateVxlanVtepMsg;
import org.zstack.network.l2.vxlan.vtep.APIQueryVtepMsg;
import org.zstack.network.l2.vxlan.vtep.VtepVO;
import org.zstack.network.l2.vxlan.vtep.VtepVO_;
import org.zstack.network.l2.vxlan.vxlanNetwork.APICreateL2VxlanNetworkMsg;
import org.zstack.network.l2.vxlan.vxlanNetwork.APIQueryL2VxlanNetworkMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;

/**
 * Created by weiwang on 02/05/2017.
 */
public class VxlanPoolApiInterceptor implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(VxlanNetworkPool.class);
    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVniRangeMsg) {
            validate((APICreateVniRangeMsg) msg);
        } else if (msg instanceof APICreateL2VxlanNetworkMsg) {
            validate((APICreateL2VxlanNetworkMsg) msg);
        } else if (msg instanceof  APIDeleteVniRangeMsg) {
            validate((APIDeleteVniRangeMsg) msg);
        } else if (msg instanceof  APIUpdateVniRangeMsg) {
            validate((APIUpdateVniRangeMsg) msg);
        } else if (msg instanceof APICreateVxlanVtepMsg) {
            validate((APICreateVxlanVtepMsg) msg);
        } else if (msg instanceof APICreateVxlanPoolRemoteVtepMsg) {
            validate((APICreateVxlanPoolRemoteVtepMsg) msg);
        } else if (msg instanceof APIDeleteVxlanPoolRemoteVtepMsg) {
            validate((APIDeleteVxlanPoolRemoteVtepMsg) msg);
        }
        return msg;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(
                APICreateL2VxlanNetworkPoolMsg.class,
                APICreateVniRangeMsg.class,
                APICreateVxlanPoolRemoteVtepMsg.class,
                APIDeleteVxlanPoolRemoteVtepMsg.class,
                APICreateL2VxlanNetworkMsg.class,
                APICreateVxlanVtepMsg.class,
                APIQueryL2VxlanNetworkPoolMsg.class,
                APIQueryVniRangeMsg.class,
                APIQueryL2VxlanNetworkMsg.class,
                APIDeleteVniRangeMsg.class,
                APIUpdateVniRangeMsg.class,
                APIQueryVtepMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.DEFAULT;
    }

    private void validate(APICreateVxlanPoolRemoteVtepMsg msg) {
        boolean isIpv4 = NetworkUtils.isIpv4Address(msg.getRemoteVtepIp());
        if (!isIpv4) {
            throw new ApiMessageInterceptionException(argerr("%s:is not ipv4", msg.getRemoteVtepIp()));
        }

        SimpleQuery<VtepVO> rqv = dbf.createQuery(VtepVO.class);
        rqv.add(VtepVO_.clusterUuid, SimpleQuery.Op.EQ, msg.getClusterUuid());
        rqv.add(VtepVO_.poolUuid, SimpleQuery.Op.EQ, msg.getL2NetworkUuid());
        rqv.add(VtepVO_.vtepIp, SimpleQuery.Op.EQ, msg.getRemoteVtepIp());
        long count = rqv.count();
        if (count > 0) {
            throw new ApiMessageInterceptionException(argerr("ip[%s] l2NetworkUuid[%s] clusterUuid[%s] ip exist in local vtep", msg.getRemoteVtepIp(), msg.getL2NetworkUuid(), msg.getClusterUuid()));
        }

    }

    private void validate(APIDeleteVxlanPoolRemoteVtepMsg msg) {
        boolean isIpv4 = NetworkUtils.isIpv4Address(msg.getRemoteVtepIp());
        if (!isIpv4) {
            throw new ApiMessageInterceptionException(argerr("%s:is not ipv4", msg.getRemoteVtepIp()));
        }

    }

    private void validate(APICreateVxlanVtepMsg msg) {
        long count = Q.New(VtepVO.class).eq(VtepVO_.hostUuid, msg.getHostUuid()).eq(VtepVO_.poolUuid, msg.getPoolUuid()).count();
        if (count > 0) {
            throw new ApiMessageInterceptionException(argerr("vxlan vtep address for host [uuid : %s] and pool [uuid : %s] pair already existed",
                            msg.getHostUuid(), msg.getPoolUuid())
            );
        }
    }

    private void validate(APIDeleteVniRangeMsg msg) {
        VniRangeVO vo = Q.New(VniRangeVO.class).eq(VniRangeVO_.uuid, msg.getUuid()).find();
        msg.setL2NetworkUuid(vo.getL2NetworkUuid());
    }

    private void validate(APIUpdateVniRangeMsg msg) {
        VniRangeVO vo = Q.New(VniRangeVO.class).eq(VniRangeVO_.uuid, msg.getUuid()).find();
        msg.setL2NetworkUuid(vo.getL2NetworkUuid());
    }

    private void validate(APICreateL2VxlanNetworkMsg msg) {
        VxlanNetworkPoolVO vo = Q.New(VxlanNetworkPoolVO.class).eq(VxlanNetworkPoolVO_.uuid, msg.getPoolUuid()).find();
        if (msg.getZoneUuid() != null && !msg.getZoneUuid().equals(vo.getZoneUuid()))  {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("the zone uuid provided not equals to zone uuid of pool [%s], please correct it or do not fill it",
                            msg.getPoolUuid())
            ));
        } else if (msg.getZoneUuid() == null ) {
            msg.setZoneUuid(vo.getZoneUuid());
        }
    }

    private void validate(APICreateVniRangeMsg msg) {
        if (msg.getStartVni() > msg.getEndVni()) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("start number [%s] of vni range is bigger than end number [%s]",
                            msg.getStartVni(), msg.getStartVni())
            ));
        }

        VxlanNetworkPoolVO pool = dbf.findByUuid(msg.getL2NetworkUuid(), VxlanNetworkPoolVO.class);

        if ( pool == null ) {
            throw new ApiMessageInterceptionException(argerr("unable create vni range, because l2 uuid[%s] is not vxlan network pool",msg.getL2NetworkUuid()));
        }

        List<Map<String, String>> tokenList = VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.getTokensOfTagsByResourceUuid(msg.getL2NetworkUuid());
        Map<String, String> attachedClusters = new HashMap<>();
        for (Map<String, String> tokens : tokenList) {
            attachedClusters.put(tokens.get(VxlanSystemTags.CLUSTER_UUID_TOKEN),
                    tokens.get(VxlanSystemTags.VTEP_CIDR_TOKEN).split("[{}]")[1]);
        }

        if (pool.getAttachedClusterRefs() != null && !pool.getAttachedClusterRefs().isEmpty()) {
            List<VxlanNetworkPoolVO> pools = SQL.New("select pool from VxlanNetworkPoolVO pool where uuid in " +
                    "(select l2NetworkUuid from L2NetworkClusterRefVO ref where ref.clusterUuid in (:clusterUuids))", VxlanNetworkPoolVO.class)
                    .param("clusterUuids", pool.getAttachedClusterRefs().stream().map(L2NetworkClusterRefVO::getClusterUuid)
                            .collect(Collectors.toList())).list();

            for (VxlanNetworkPoolVO p : pools) {
                if (!p.getType().equals(L2NetworkConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE)) {

                    boolean sameCidr = false;
                    List<Map<String, String>> list = VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.getTokensOfTagsByResourceUuid(p.getUuid());
                    for (Map<String, String> tokens : list) {
                        String clusterUuid = tokens.get(VxlanSystemTags.CLUSTER_UUID_TOKEN);
                        String cidr = tokens.get(VxlanSystemTags.VTEP_CIDR_TOKEN).split("[{}]")[1];
                        if (attachedClusters.get(clusterUuid) != null) {
                            if (NetworkUtils.isSameCidr(cidr, attachedClusters.get(clusterUuid))) {
                                sameCidr = true;
                                break;
                            }
                        }
                    }

                    if (!sameCidr) {
                        continue;
                    }
                }

                for (VniRangeVO e : p.getAttachedVniRanges()) {
                    if (checkOverlap(msg.getStartVni(), msg.getEndVni(), e.getStartVni(), e.getEndVni()) == true) {
                        throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                                String.format("this vni range[start:%s, end:%s] has overlapped with vni range [%s], which start vni is [%s], end vni is [%s]",
                                        msg.getStartVni(), msg.getEndVni(), e.getUuid(), e.getStartVni(), e.getEndVni())
                        ));
                    }
                }
            }
        } else if (pool.getAttachedVniRanges() != null && !pool.getAttachedVniRanges().isEmpty()) {
            for (VniRangeVO e : pool.getAttachedVniRanges()) {
                if (checkOverlap(msg.getStartVni(), msg.getEndVni(), e.getStartVni(), e.getEndVni()) == true) {
                    throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                            String.format("this vni range[start:%s, end:%s] has overlapped with vni range [%s], which start vni is [%s], end vni is [%s]",
                                    msg.getStartVni(), msg.getEndVni(), e.getUuid(), e.getStartVni(), e.getEndVni())
                    ));
                }
            }
        }

    }

    private boolean checkOverlap(Integer checktart, Integer checkEnd, Integer existStart, Integer existEnd){
        return (checktart >= existStart && checktart <= existEnd) || (checktart <= existStart && existStart <= checkEnd);
    }
}
