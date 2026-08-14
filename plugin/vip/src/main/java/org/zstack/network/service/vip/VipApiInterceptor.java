package org.zstack.network.service.vip;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.APIAttachL3NetworkToVmMsg;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

import org.apache.commons.collections.CollectionUtils;
import org.zstack.identity.AccountManager;
import org.zstack.header.identity.AccountConstant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
/**
 */
@InterceptorForService("vip")
public class VipApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private VipManager vipMgr;
    @Autowired
    private AccountManager acntMgr;

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APIDeleteIpAddressMsg.class);
        return ret;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVipMsg) {
            validate((APICreateVipMsg) msg);
        } else if (msg instanceof APIGetVipAvailablePortMsg) {
            validate((APIGetVipAvailablePortMsg) msg);
        } else if (msg instanceof APICheckVipPortAvailabilityMsg) {
            validate((APICheckVipPortAvailabilityMsg) msg);
        } else if (msg instanceof APIDeleteVipMsg) {
            validate((APIDeleteVipMsg) msg);
        } else if (msg instanceof APIDeleteIpAddressMsg) {
            validate((APIDeleteIpAddressMsg) msg);
        }

        return msg;
    }
    private void checkVipBelongToAccount(String vipUuid, String accountUuid) {

        if (!accountUuid.equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)) {
            List<String> accessibleUuids = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, VipVO.class);

            if (accessibleUuids == null || accessibleUuids.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("account have no vips"));
            }
            if (!accessibleUuids.contains(vipUuid)) {
                throw new ApiMessageInterceptionException(argerr("vip can not be accessed by this account"));
            }
        }
    }
   
    private void validate(APICheckVipPortAvailabilityMsg msg) {
        String accountUuid = msg.getSession().getAccountUuid();
        checkVipBelongToAccount(msg.getVipUuid(), accountUuid);
    }

    private void validate(APIGetVipAvailablePortMsg msg) {
        String accountUuid = msg.getSession().getAccountUuid();
        checkVipBelongToAccount(msg.getVipUuid(), accountUuid);
    }

    private void validate(APIDeleteIpAddressMsg msg) {
        for (String uuid : msg.getUsedIpUuids()) {
            UsedIpVO ip = dbf.findByUuid(uuid, UsedIpVO.class);
            if (Q.New(VipVO.class).eq(VipVO_.l3NetworkUuid, ip.getL3NetworkUuid())
                    .eq(VipVO_.ip, ip.getIp()).isExists()) {
                throw new ApiMessageInterceptionException(argerr("cannot delete ip, because ip[%s] is a vip", ip.getIp()));
            }
        }
    }

    private void validate(APIDeleteVipMsg msg) {
        VipVO vipVO = dbf.findByUuid(msg.getVipUuid(), VipVO.class);
        if (vipMgr.isSystemVip(vipVO)) {
            throw new ApiMessageInterceptionException(argerr("system vip can not be deleted by API message"));
        }
    }

    private void validate(APICreateVipMsg msg) {
        if (msg.getAllocatorStrategy() != null && !IpAllocatorType.hasType(msg.getAllocatorStrategy())) {
            throw new ApiMessageInterceptionException(argerr("unsupported ip allocation strategy[%s]", msg.getAllocatorStrategy()));
        }

        L3NetworkVO l3NetworkVO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        if (msg.getRequiredIp() != null) {
            if (NetworkUtils.isIpv4Address(msg.getRequiredIp()) && !l3NetworkVO.getIpVersions().contains(IPv6Constants.IPv4)) {
                throw new ApiMessageInterceptionException(argerr("requiredIp[%s] is not in valid IPv4 mediaType", msg.getRequiredIp()));
            }
            if (IPv6NetworkUtils.isIpv6Address(msg.getRequiredIp()) && !l3NetworkVO.getIpVersions().contains(IPv6Constants.IPv6)) {
                throw new ApiMessageInterceptionException(argerr("requiredIp[%s] is not in valid IPv4 mediaType", msg.getRequiredIp()));
            }

            SimpleQuery<VipVO> q = dbf.createQuery(VipVO.class);
            q.add(VipVO_.ip, Op.EQ, msg.getRequiredIp());
            q.add(VipVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
            if (q.isExists()) {
                throw new ApiMessageInterceptionException(operr("there is already a vip[%s] on l3Network[uuid:%s]", msg.getRequiredIp(), msg.getL3NetworkUuid()));
            }

            UsedIpVO usedIpVO = Q.New(UsedIpVO.class).eq(UsedIpVO_.ip, msg.getRequiredIp())
                    .eq(UsedIpVO_.l3NetworkUuid, msg.getL3NetworkUuid()).find();
            if (usedIpVO != null && !msg.isSystem()) {
                throw new ApiMessageInterceptionException(operr("required ip address [%s] is already used", msg.getRequiredIp()));
            }

            if (NetworkUtils.isIpv4Address(msg.getRequiredIp())) {
                msg.setIpVersion(IPv6Constants.IPv4);
            } else if (IPv6NetworkUtils.isIpv6Address(msg.getRequiredIp())) {
                msg.setIpVersion(IPv6Constants.IPv6);
            }
        }

        if (msg.getIpVersion() == null) {
            if (msg.getIpRangeUuid() != null) {
                IpRangeVO ipr = dbf.findByUuid(msg.getIpRangeUuid(), IpRangeVO.class);
                msg.setIpVersion(ipr.getIpVersion());
            } else {
                if (l3NetworkVO.getIpVersions().contains(IPv6Constants.IPv4)) {
                    msg.setIpVersion(IPv6Constants.IPv4);
                } else {
                    msg.setIpVersion(IPv6Constants.IPv6);
                }
            }
        }

        if (msg.getIpVersion() == null) {
            throw new ApiMessageInterceptionException(operr("could not create vip, because can not determine the vip version"));
        }
    }
}
