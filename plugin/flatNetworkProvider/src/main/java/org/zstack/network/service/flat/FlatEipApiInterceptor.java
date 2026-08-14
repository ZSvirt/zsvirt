package org.zstack.network.service.flat;


import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.eip.*;
import org.zstack.network.service.vip.Vip;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.argerr;

/**
 * Created by MaJin on 2017/12/21.
 */
public class FlatEipApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private NetworkServiceManager nwServiceMgr;

    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(APICreateEipMsg.class, APIAttachEipMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateEipMsg) {
            validate((APICreateEipMsg) msg);
        } else if (msg instanceof APIAttachEipMsg) {
            validate((APIAttachEipMsg) msg);
        }

        return msg;
    }

    void validateNicGateway(String vipUuid, String nicUuid) {
        VipVO vipVO = Q.New(VipVO.class).eq(VipVO_.uuid, vipUuid).find();
        VmNicVO nicVO = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, nicUuid).find();

        String gateway = null;
        if (NetworkUtils.isIpv4Address(vipVO.getIp())) {
            for (UsedIpVO ip : nicVO.getUsedIps()) {
                if (ip.getIpVersion() == IPv6Constants.IPv4) {
                    gateway = ip.getGateway();
                }
            }
        } else {
            for (UsedIpVO ip : nicVO.getUsedIps()) {
                if (ip.getIpVersion() == IPv6Constants.IPv6) {
                    gateway = ip.getGateway();
                }
            }
        }

        if (StringUtils.isEmpty(gateway)) {
            throw new ApiMessageInterceptionException(argerr("could not attach eip because there is no gateway for nic[uuid:%s]", nicUuid));
        }
    }

    @Transactional(readOnly = true)
    protected void validate(APICreateEipMsg msg) {
        if (msg.getVmNicUuid() == null) {
            return;
        }

        String privateL3Uuid = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).eq(VmNicVO_.uuid, msg.getVmNicUuid()).findValue();
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(privateL3Uuid, EipConstant.EIP_TYPE);
        if (!providerType.toString().equals(FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING)) {
            return;
        }

        validateNicGateway(msg.getVipUuid(), msg.getVmNicUuid());

        String pubL3Uuid = Q.New(VipVO.class).select(VipVO_.l3NetworkUuid).eq(VipVO_.uuid, msg.getVipUuid()).findValue();
        checkVipPublicL3Network(msg.getVmNicUuid(), pubL3Uuid);
        checkFlatVmNicAlreadyHasEip(msg.getVmNicUuid(), null, msg.getVipUuid());
    }

    @Transactional(readOnly = true)
    protected void validate(APIAttachEipMsg msg) {
        String privateL3Uuid = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).eq(VmNicVO_.uuid, msg.getVmNicUuid()).findValue();
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(privateL3Uuid, EipConstant.EIP_TYPE);

        /* TODO: this is temp limitation, ipv6 eip can be only attached to flat eip */
        EipVO eip = Q.New(EipVO.class).eq(EipVO_.uuid, msg.getEipUuid()).find();
        if (IPv6NetworkUtils.isIpv6Address(eip.getVipIp()) && !providerType.toString().equals(FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING)) {
            throw new ApiMessageInterceptionException(argerr("could not attach eip because ipv6 eip can ONLY be attached to flat network"));
        }

        if (!providerType.toString().equals(FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING)) {
            return;
        }

        validateNicGateway(eip.getVipUuid(), msg.getVmNicUuid());

        String pubL3Uuid = SQL.New("select vip.l3NetworkUuid from EipVO eip, VipVO vip" +
                " where eip.uuid = :eipUuid" +
                " and eip.vipUuid = vip.uuid", String.class)
                .param("eipUuid", msg.getEipUuid())
                .find();
        checkVipPublicL3Network(msg.getVmNicUuid(), pubL3Uuid);
        checkFlatVmNicAlreadyHasEip(msg.getVmNicUuid(), msg.getEipUuid(), null);
    }

    @Transactional(readOnly = true)
    private void checkVipPublicL3Network(String vmNicUuid, String pubL3Uuid){
        boolean isPublicL2NetworkAttachedVmCluster = (Long) SQL.New("select count(l3)" +
                " from VmInstanceVO vm, VmNicVO nic, L2NetworkClusterRefVO ref, L3NetworkVO l3" +
                " where nic.uuid = :nicUuid" +
                " and vm.uuid = nic.vmInstanceUuid" +
                " and ref.clusterUuid = vm.clusterUuid" +
                " and ref.l2NetworkUuid = l3.l2NetworkUuid" +
                " and l3.uuid = :publicL3Uuid", Long.class)
                .param("nicUuid", vmNicUuid)
                .param("publicL3Uuid", pubL3Uuid)
                .find() > 0;
        if (!isPublicL2NetworkAttachedVmCluster){
            throw new ApiMessageInterceptionException(argerr("L2Network where vip's L3Network based hasn't attached" +
                    " the cluster where vmNic[uuid:%s] located", vmNicUuid));
        }
    }

    @Transactional(readOnly = true)
    private void checkFlatVmNicAlreadyHasEip(String vmNicUuid, String eipUuid, String vipUuid){
        VipVO newVipVO;
        if (vipUuid != null) {
            newVipVO = Q.New(VipVO.class).eq(VipVO_.uuid, vipUuid).find();
        } else {
            String uuid = Q.New(EipVO.class).eq(EipVO_.uuid, eipUuid).select(EipVO_.vipUuid).findValue();
            newVipVO = Q.New(VipVO.class).eq(VipVO_.uuid, uuid).find();
        }
        boolean newVipVersion = NetworkUtils.isIpv4Address(newVipVO.getIp());

        List<String> oldVipIps = Q.New(EipVO.class).eq(EipVO_.vmNicUuid, vmNicUuid).select(EipVO_.vipIp).listValues();
        if (oldVipIps.isEmpty()) {
            return;
        }

        for (String oldVipIp : oldVipIps) {
            boolean oldVipVersion = NetworkUtils.isIpv4Address(oldVipIp);
            if (oldVipVersion == newVipVersion) {
                String version = oldVipVersion ? "ipv4" : "ipv6";
                throw new ApiMessageInterceptionException(argerr("can not bound more than 1 %s eip to a vm nic[uuid:%s] of flat ",
                        version, vmNicUuid));
            }
        }
    }
}
