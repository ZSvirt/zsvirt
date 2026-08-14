package org.zstack.network.service.eip;


import org.apache.commons.net.util.SubnetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.AddressPoolVO;
import org.zstack.header.network.l3.AddressPoolVO_;
import org.zstack.header.network.l3.NormalIpRangeVO;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicHelper;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.service.vip.VipNetworkServicesRefVO;
import org.zstack.network.service.vip.VipNetworkServicesRefVO_;
import org.zstack.network.service.vip.VipState;
import org.zstack.network.service.vip.VipVO;
import org.zstack.utils.Utils;
import org.zstack.utils.VipUseForList;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.concurrent.Callable;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 */
@InterceptorForService("eip")
public class EipApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(EipApiInterceptor.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof EipMessage) {
            EipMessage emsg = (EipMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, EipConstant.SERVICE_ID, emsg.getEipUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateEipMsg) {
            validate((APICreateEipMsg) msg);
        } else if (msg instanceof APIDeleteEipMsg) {
            validate((APIDeleteEipMsg) msg);
        } else if (msg instanceof APIDetachEipMsg) {
            validate((APIDetachEipMsg) msg);
        } else if (msg instanceof APIAttachEipMsg) {
            validate((APIAttachEipMsg) msg);
        } else if (msg instanceof APIGetEipAttachableVmNicsMsg) {
            validate((APIGetEipAttachableVmNicsMsg) msg);
        }else if (msg instanceof APIGetVmNicAttachableEipsMsg) {
            validate((APIGetVmNicAttachableEipsMsg) msg);
        }

        setServiceId(msg);

        return msg;
    }

    private void validate(APIGetVmNicAttachableEipsMsg msg){
        String vmInstanceUuid = Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid).eq(VmNicVO_.uuid,msg.getVmNicUuid()).findValue();
        if (vmInstanceUuid == null) {
            throw new ApiMessageInterceptionException(operr("vmNic[uuid:%s] is not attached to vmInstance, cannot get attachable eips", msg.getVmNicUuid()));
        }
    }

    private void validate(APIGetEipAttachableVmNicsMsg msg) {
        if (msg.getVipUuid() == null && msg.getEipUuid() == null) {
            throw new ApiMessageInterceptionException(argerr("either eipUuid or vipUuid must be set"));
        }

        if (msg.getEipUuid() != null) {
            EipState state = Q.New(EipVO.class).select(EipVO_.state).eq(EipVO_.uuid,msg.getEipUuid()).findValue();
            if (state != EipState.Enabled) {
                throw new ApiMessageInterceptionException(operr("eip[uuid:%s] is not in state of Enabled, cannot get attachable vm nic", msg.getEipUuid()));
            }
        }

        boolean isAddressPool = false;
        boolean isIpv6 = false;
        if (msg.getVipUuid() != null) {
            VipVO vip = dbf.findByUuid(msg.getVipUuid(), VipVO.class);
            isAddressPool = Q.New(AddressPoolVO.class).eq(AddressPoolVO_.uuid, vip.getIpRangeUuid()).isExists();
            if (IPv6NetworkUtils.isIpv6Address(vip.getIp())) {
                isIpv6 = true;
            }
        } else if (msg.getEipUuid() != null) {
            EipVO eipVO = dbf.findByUuid(msg.getEipUuid(), EipVO.class);
            VipVO vip = dbf.findByUuid(eipVO.getVipUuid(), VipVO.class);
            isAddressPool = Q.New(AddressPoolVO.class).eq(AddressPoolVO_.uuid, vip.getIpRangeUuid()).isExists();
            if (IPv6NetworkUtils.isIpv6Address(vip.getIp())) {
                isIpv6 = true;
            }
        }
        if (isAddressPool) {
            msg.setNetworkServiceProvider("vrouter");
        } else if (isIpv6) {
            /* TODO, temp hard code */
            msg.setNetworkServiceProvider("Flat");
        }
    }

    private void validateEipGuestIpUuid(String vmNicUuid, String guestIpUuid){
        VmNicVO nic = dbf.findByUuid(vmNicUuid, VmNicVO.class);
        boolean found = false;
        for (UsedIpVO ip : nic.getUsedIps()) {
            if (ip.getUuid().equals(guestIpUuid)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new ApiMessageInterceptionException(argerr("ip [uuid:%s] is attached to vm nic [%s]", guestIpUuid, vmNicUuid));
        }
    }

    private void validate(final APIAttachEipMsg msg) {
        SimpleQuery<EipVO> q = dbf.createQuery(EipVO.class);
        q.select(EipVO_.state, EipVO_.vmNicUuid, EipVO_.vipUuid);
        q.add(EipVO_.uuid, Op.EQ, msg.getEipUuid());
        Tuple t = q.findTuple();
        String vmNicUuid = t.get(1, String.class);
        if (vmNicUuid != null) {
            throw new ApiMessageInterceptionException(operr("eip[uuid:%s] has attached to another vm nic[uuid:%s], can't attach again",
                            msg.getEipUuid(), vmNicUuid));
        }

        EipState state = t.get(0, EipState.class);
        if (state != EipState.Enabled) {
            throw new ApiMessageInterceptionException(operr("eip[uuid: %s] can only be attached when state is %s, current state is %s",
                            msg.getEipUuid(), EipState.Enabled, state));
        }

        if (msg.getUsedIpUuid() == null) {
            VmNicVO nic = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
            msg.setUsedIpUuid(nic.getUsedIpUuid());
        } else {
            validateEipGuestIpUuid(msg.getVmNicUuid(), msg.getUsedIpUuid());
        }

        String vipUuid = t.get(2, String.class);
        isVipInVmNicSubnet(vipUuid, msg.getUsedIpUuid());

        VipVO vip = new Callable<VipVO>() {
            @Override
            @Transactional(readOnly = true)
            public VipVO call() {
                String sql = "select vip" +
                        " from VipVO vip, EipVO eip" +
                        " where vip.uuid = eip.vipUuid" +
                        " and eip.uuid = :eipUuid";
                TypedQuery<VipVO> q = dbf.getEntityManager().createQuery(sql, VipVO.class);
                q.setParameter("eipUuid", msg.getEipUuid());
                return q.getSingleResult();
            }
        }.call();

        VmNicVO nic = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        if (VmNicHelper.getL3Uuids(nic).contains(vip.getL3NetworkUuid())){
            throw new ApiMessageInterceptionException(argerr("guest l3Network of vm nic[uuid:%s] and vip l3Network of EIP[uuid:%s] are the same network",
                            msg.getVmNicUuid(), msg.getEipUuid()));
        }

        // check if the vm already has a network where the vip comes
        checkIfVmAlreadyHasVipNetwork(nic.getVmInstanceUuid(), vip);
        checkVmState(msg.getVmNicUuid());

        checkNicRule(msg.getVmNicUuid());

        if (msg.getUsedIpUuid() != null) {
            boolean found = false;
            for (UsedIpVO ip : nic.getUsedIps()) {
                if (ip.getUuid().equals(msg.getUsedIpUuid())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new ApiMessageInterceptionException(argerr("Ip address [uuid:%s] is not belonged to nic [uuid:%s]", msg.getEipUuid(), msg.getVmNicUuid()));
            }
        } else {
            msg.setUsedIpUuid(nic.getUsedIpUuid());
        }
    }

    private void validate(APIDetachEipMsg msg) {
        SimpleQuery<EipVO> q = dbf.createQuery(EipVO.class);
        q.select(EipVO_.vmNicUuid);
        q.add(EipVO_.uuid, Op.EQ, msg.getUuid());
        String vmNicUuid = q.findValue();
        if (vmNicUuid == null) {
            throw new ApiMessageInterceptionException(operr("eip[uuid:%s] has not attached to any vm nic", msg.getUuid()));
        }

        msg.vmNicUuid = vmNicUuid;
    }

    private void validate(APIDeleteEipMsg msg) {
        if (!dbf.isExist(msg.getUuid(), EipVO.class)) {
            APIDeleteEipEvent evt = new APIDeleteEipEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void isVipInVmNicSubnet(String vipUuid, String guestIpUuid) {
        VipVO vip = dbf.findByUuid(vipUuid, VipVO.class);
        UsedIpVO vipIp = dbf.findByUuid(vip.getUsedIpUuid(), UsedIpVO.class);

        UsedIpVO guestIp = dbf.findByUuid(guestIpUuid, UsedIpVO.class);

        if (!vipIp.getIpVersion().equals(guestIp.getIpVersion())) {
            throw new ApiMessageInterceptionException(operr("vip ipVersion [%d] is different from guestIp ipVersion [%d].",
                    vipIp.getIpVersion(), guestIp.getIpVersion()));
        }

        if (guestIp.getIpRangeUuid() != null) {
            NormalIpRangeVO guestRange = dbf.findByUuid(guestIp.getIpRangeUuid(), NormalIpRangeVO.class);
            if (vipIp.getIpVersion() == IPv6Constants.IPv4) {
                SubnetUtils guestSub = new SubnetUtils(guestRange.getGateway(), guestRange.getNetmask());
                if (guestSub.getInfo().isInRange(vipIp.getIp())) {
                    throw new ApiMessageInterceptionException(operr("Vip[%s] is in the guest ip range [%s, %s]",
                            vipIp.getIp(), guestRange.getStartIp(), guestRange.getEndIp()));
                }
            } else {
                if (IPv6NetworkUtils.isIpv6InCidrRange(vipIp.getIp(), guestRange.getNetworkCidr())){
                    throw new ApiMessageInterceptionException(operr("Vip[%s] is in the guest ip range [%s, %s]",
                            vipIp.getIp(), guestRange.getStartIp(), guestRange.getEndIp()));
                }
            }
        }
    }

    @Transactional(readOnly = true)
    private void checkIfVmAlreadyHasVipNetwork(String vmUuid, VipVO vip) {
        String sql = "select count(*) from VmNicVO nic, VmInstanceVO vm where nic.vmInstanceUuid = vm.uuid" +
                " and vm.uuid = :vmUuid and nic.l3NetworkUuid = :vipL3Uuid";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("vmUuid", vmUuid);
        q.setParameter("vipL3Uuid", vip.getL3NetworkUuid());
        Long c = q.getSingleResult();
        if (c > 0) {
            throw new ApiMessageInterceptionException(argerr("the vm[uuid:%s] that the EIP is about to attach is already on the public network[uuid:%s] from which" +
                            " the vip[uuid:%s, name:%s, ip:%s] comes", vmUuid, vip.getL3NetworkUuid(), vip.getUuid(), vip.getName(), vip.getIp()));
        }
    }

    private void validate(APICreateEipMsg msg) {
        VipVO vip = dbf.findByUuid(msg.getVipUuid(), VipVO.class);
        List<String> useFor = Q.New(VipNetworkServicesRefVO.class).select(VipNetworkServicesRefVO_.serviceType).eq(VipNetworkServicesRefVO_.vipUuid, msg.getVipUuid()).listValues();
        if(useFor != null && !useFor.isEmpty()){
            VipUseForList useForList = new VipUseForList(useFor);
            if(!useForList.validateNewAdded(EipConstant.EIP_NETWORK_SERVICE_TYPE)) {
                throw new ApiMessageInterceptionException(operr("vip[uuid:%s] has been occupied other network service entity[%s]", msg.getVipUuid(), useForList.toString()));
            }
        }

        if (vip.isSystem()) {
            throw new ApiMessageInterceptionException(operr("eip can not be created on system vip"));
        }

        if (vip.getState() != VipState.Enabled) {
            throw new ApiMessageInterceptionException(operr("vip[uuid:%s] is not in state[%s], current state is %s", msg.getVipUuid(), VipState.Enabled, vip.getState()));
        }

        if (msg.getVmNicUuid() != null) {
            SimpleQuery<VmNicVO> nicq = dbf.createQuery(VmNicVO.class);
            nicq.add(VmNicVO_.uuid, Op.EQ, msg.getVmNicUuid());
            VmNicVO nic = nicq.find();
            if (VmNicHelper.getL3Uuids(nic).contains(vip.getL3NetworkUuid())) {
                throw new ApiMessageInterceptionException(argerr("guest l3Network of vm nic[uuid:%s] and vip l3Network of vip[uuid: %s] are the same network", msg.getVmNicUuid(), msg.getVipUuid()));
            }

            if (msg.getUsedIpUuid() == null) {
                msg.setUsedIpUuid(nic.getUsedIpUuid());
            } else {
                validateEipGuestIpUuid(msg.getVmNicUuid(), msg.getUsedIpUuid());
            }

            // check if the vm already has a network where the vip comes
            checkIfVmAlreadyHasVipNetwork(nic.getVmInstanceUuid(), vip);
        }

        if (msg.getUsedIpUuid() != null) {
            isVipInVmNicSubnet(msg.getVipUuid(), msg.getUsedIpUuid());
        }

        checkNicRule(msg.getVmNicUuid());
    }

    @Transactional(readOnly = true)
    private void checkVmState(String vmNicUuid){
        VmInstanceState state = SQL.New(
                "select vm.state from VmInstanceVO vm, VmNicVO nic" +
                        " where nic.uuid =:vmNicUuid" +
                        " and vm.uuid = nic.vmInstanceUuid",
                VmInstanceState.class)
                .param("vmNicUuid", vmNicUuid).find();
        if (state != null && !EipConstant.attachableVmStates.contains(state)){
            throw new ApiMessageInterceptionException(operr(
                    "vm state[%s] is not allowed to operate eip, maybe you should wait the vm process complete",
                    state.toString()));
        }
    }

    @Transactional(readOnly = true)
    private void checkNicRule(String vmNicUuid){
        List<String> cidrs = SQL.New(
                "select pf.allowedCidr from PortForwardingRuleVO pf" +
                        " where pf.vmNicUuid =:vmNicUuid and pf.allowedCidr is not NULL"
                , String.class).param("vmNicUuid", vmNicUuid).list();

        if (!cidrs.isEmpty()) {
            throw new ApiMessageInterceptionException(operr(
                    "vmNic uuid[%s] is not allowed add eip, because vmNic exist portForwarding with allowedCidr rule",
                    vmNicUuid));
        }
    }
}
