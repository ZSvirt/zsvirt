package org.zstack.network.service.vipQos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO_;
import org.zstack.header.vipQos.*;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.Set;

import static org.zstack.core.Platform.operr;

/**
 * Created by liangbo.zhou on 17-6-22.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
@InterceptorForService("VipQos")
public class VipQosApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(VipQosApiInterceptor.class);

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APISetVipQosMsg) {
            validate((APISetVipQosMsg) msg);
        } else if (msg instanceof APIDeleteVipQosMsg) {
            validate((APIDeleteVipQosMsg) msg);
        }

        return msg;
    }

    private void validate(APISetVipQosMsg msg){
        VipVO vipVO = Q.New(VipVO.class).eq(VipVO_.uuid, msg.getVipUuid()).find();
        if (!NetworkUtils.isIpv4Address(vipVO.getIp())) {
            throw new ApiMessageInterceptionException(operr("VipQos for ipv6 wil be added soon"));
        }

        if (msg.getPort() != null) {
            if (Q.New(VipQosVO.class).eq(VipQosVO_.vipUuid, msg.getVipUuid()).eq(VipQosVO_.port, msg.getPort()).isExists() == true) {
                throw new ApiMessageInterceptionException(operr("VipQos for Vip [uuid: %s] port %s already existed",
                        msg.getVipUuid(), Integer.toString(msg.getPort())));
            }
        } else {
            if (Q.New(VipQosVO.class).eq(VipQosVO_.vipUuid, msg.getVipUuid()).eq(VipQosVO_.port, 0).isExists() == true) {
                throw new ApiMessageInterceptionException(operr("VipQos for Vip [uuid: %s] already existed",
                        msg.getVipUuid()));
            }
        }

        if (msg.getInboundBandwidth() == null && msg.getOutboundBandwidth() == null) {
            throw new ApiMessageInterceptionException(operr("SetVipQos MUST set InboundBandwidth or OutboundBandwidth"));
        }

        Set<String> peerL3NetworkUuids = vipVO.getPeerL3NetworkUuids();
        if (peerL3NetworkUuids != null && peerL3NetworkUuids.size() != 0) {
            long vipQosCount = Q.New(NetworkServiceL3NetworkRefVO.class).in(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, peerL3NetworkUuids)
                    .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, VipQosConstants.VIPQOS_NETWORK_SERVICE_TYPE.toString()).count();
            if (vipQosCount != peerL3NetworkUuids.size()) {
                throw new ApiMessageInterceptionException(operr("Cannot set Qos for this Vip. Not all peer l3networks provide VipQos service."));
            }
        }
    }

    private void validate(APIDeleteVipQosMsg msg){
        if (msg.getPort() != null) {
            if (Q.New(VipQosVO.class).eq(VipQosVO_.vipUuid, msg.getUuid()).eq(VipQosVO_.port, msg.getPort()).isExists() == false) {
                throw new ApiMessageInterceptionException(operr("VipQos for Vip [uuid: %s] port %s does not exist",
                            msg.getUuid(), Integer.toString(msg.getPort())));
            }
        } else {
            if (Q.New(VipQosVO.class).eq(VipQosVO_.vipUuid, msg.getUuid()).eq(VipQosVO_.port, 0).isExists() == false) {
                throw new ApiMessageInterceptionException(operr("VipQos for Vip [uuid: %s] does not exist",
                        msg.getUuid()));
            }
        }
    }
}
