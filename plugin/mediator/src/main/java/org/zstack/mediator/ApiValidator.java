package org.zstack.mediator;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.header.network.l3.IpRangeVO_;
import org.zstack.header.vm.*;
import org.zstack.network.service.eip.APIAttachEipMsg;
import org.zstack.network.service.eip.APICreateEipMsg;
import org.zstack.network.service.eip.EipConstant;
import org.zstack.network.service.eip.EipVO;
import org.zstack.network.service.lb.APICreateLoadBalancerListenerMsg;
import org.zstack.network.service.lb.LoadBalancerConstants;
import org.zstack.network.service.lb.LoadBalancerVO;
import org.zstack.network.service.lb.LoadBalancerVO_;
import org.zstack.network.service.portforwarding.APIAttachPortForwardingRuleMsg;
import org.zstack.network.service.portforwarding.APICreatePortForwardingRuleMsg;
import org.zstack.network.service.portforwarding.PortForwardingRuleVO;
import org.zstack.network.service.vip.*;
import org.zstack.utils.RangeSet;
import org.zstack.utils.Utils;
import org.zstack.utils.VipUseForList;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 */
public class ApiValidator implements GlobalApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    private final static CLogger logger = Utils.getLogger(ApiValidator.class);
    @Autowired
    private PluginRegistry pluginRgty;

    private List<VipGetUsedPortRangeExtensionPoint> vipGetUsedPortRangeExtensionPoints;

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APICreateEipMsg.class);
        ret.add(APIAttachEipMsg.class);
        ret.add(APICreatePortForwardingRuleMsg.class);
        ret.add(APICreateLoadBalancerListenerMsg.class);
        ret.add(APIAttachPortForwardingRuleMsg.class);
        ret.add(APIAttachL3NetworkToVmMsg.class);
        return ret;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateEipMsg) {
            validate((APICreateEipMsg) msg);
        } else if (msg instanceof APIAttachEipMsg) {
            validate((APIAttachEipMsg) msg);
        } else if (msg instanceof APICreatePortForwardingRuleMsg) {
            validate((APICreatePortForwardingRuleMsg) msg);
        } else if (msg instanceof APIAttachPortForwardingRuleMsg) {
            validate((APIAttachPortForwardingRuleMsg) msg);
        } else if (msg instanceof APIAttachL3NetworkToVmMsg) {
            validate((APIAttachL3NetworkToVmMsg) msg);
        } else if (msg instanceof APICreateLoadBalancerListenerMsg) {
            validate((APICreateLoadBalancerListenerMsg) msg);
        }

        return msg;
    }

    private void validate(APIAttachL3NetworkToVmMsg msg) {
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        if (vmInstanceVO.getType().equals(ApplianceVmConstant.APPLIANCE_VM_TYPE)){
            validateIpRangeOverlapWithVm(msg.getL3NetworkUuid(), msg.getVmInstanceUuid());
        }
    }

    private void validateIpRangeOverlapWithVm(String l3NetworkUuid, String vmInstanceUuid) {
        List<VmNicVO> vmNicVOS = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vmInstanceUuid).list();
        List<IpRangeVO> newIpRangeVOS = Q.New(IpRangeVO.class).eq(IpRangeVO_.l3NetworkUuid, l3NetworkUuid).list();
        if (newIpRangeVOS == null || newIpRangeVOS.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("no ip ranges attached with l3 network[uuid:%s]", l3NetworkUuid));
        }

        List<IpRangeVO> newIp4RangeVOS = newIpRangeVOS.stream().filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv4).collect(Collectors.toList());
        List<IpRangeVO> newIp6RangeVOS = newIpRangeVOS.stream().filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv6).collect(Collectors.toList());
        for (VmNicVO vmNicVO: vmNicVOS) {
            List<IpRangeVO> ip4RangeVOS = Q.New(IpRangeVO.class).eq(IpRangeVO_.l3NetworkUuid, vmNicVO.getL3NetworkUuid())
                    .eq(IpRangeVO_.ipVersion, IPv6Constants.IPv4).limit(1).list();
            List<IpRangeVO> ip6RangeVOS = Q.New(IpRangeVO.class).eq(IpRangeVO_.l3NetworkUuid, vmNicVO.getL3NetworkUuid())
                    .eq(IpRangeVO_.ipVersion, IPv6Constants.IPv6).limit(1).list();
            if (!newIp4RangeVOS.isEmpty() && !ip4RangeVOS.isEmpty()) {
                if (NetworkUtils.isCidrOverlap(newIp4RangeVOS.get(0).getNetworkCidr(), ip4RangeVOS.get(0).getNetworkCidr())) {
                    throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The cidr of l3[%s] to attach overlapped with l3[%s] already attached to vm", l3NetworkUuid, vmNicVO.getL3NetworkUuid()));
                }
            }
            if (!newIp6RangeVOS.isEmpty() && !ip6RangeVOS.isEmpty()) {
                if (IPv6NetworkUtils.isIpv6RangeOverlap(ip6RangeVOS.get(0).getStartIp(), ip6RangeVOS.get(0).getEndIp(),
                        newIp6RangeVOS.get(0).getStartIp(),  newIp6RangeVOS.get(0).getEndIp())) {
                    throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The cidr of l3[%s] to attach overlapped with l3[%s] already attached to vm", l3NetworkUuid, vmNicVO.getL3NetworkUuid()));
                }
            }
        }
    }

    private void validate(APIAttachPortForwardingRuleMsg msg) {
//        Note(WeiW): Disable this since eip and portforwarding can be used at same time
//        isVmNicUsedByEip(msg.getVmNicUuid());
    }

    private void validate(APICreatePortForwardingRuleMsg msg) {
        RangeSet.Range cur = new RangeSet.Range(msg.getVipPortStart(), msg.getVipPortEnd());
        checkVipPortConfliction(msg.getVipUuid(), msg.getProtocolType(), cur);
    }

    @Transactional(readOnly = true)
    private void isVmNicUsedByPortForwarding(String vmNicUuid) {
        String sql = "select pf from PortForwardingRuleVO pf, VmInstanceVO vm, VmNicVO nic where pf.vmNicUuid = nic.uuid" +
                " and nic.vmInstanceUuid = vm.uuid and vm.uuid = (select n.vmInstanceUuid from VmNicVO n where n.uuid = :nicUuid)";
        TypedQuery<PortForwardingRuleVO> q = dbf.getEntityManager().createQuery(sql, PortForwardingRuleVO.class);
        q.setParameter("nicUuid", vmNicUuid);
        List<PortForwardingRuleVO> pfs = q.getResultList();

        if (!pfs.isEmpty()) {
            sql = "select vm from VmInstanceVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.uuid = :nicUuid";
            TypedQuery<VmInstanceVO> vq = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
            vq.setParameter("nicUuid", vmNicUuid);
            VmInstanceVO vm = vq.getSingleResult();

            List<String> pfStr = pfs.stream().map(pf -> String.format("(name:%s, ip:%s)", pf.getName(), pf.getVipIp())).collect(Collectors.toList());

            throw new ApiMessageInterceptionException(operr("the vm[name:%s, uuid:%s] already has some port forwarding rules%s attached", vm.getName(), vm.getUuid(),
                            StringUtils.join(pfStr, ",")));
        }
    }

    @Transactional(readOnly = true)
    private void isVmNicUsedByEip(String vmNicUuid) {
        String sql = "select eip from EipVO eip, VmInstanceVO vm, VmNicVO nic where eip.vmNicUuid = nic.uuid" +
                " and nic.vmInstanceUuid = vm.uuid and vm.uuid = (select n.vmInstanceUuid from VmNicVO n where n.uuid = :nicUuid)";
        TypedQuery<EipVO> q = dbf.getEntityManager().createQuery(sql, EipVO.class);
        q.setParameter("nicUuid", vmNicUuid);
        List<EipVO> eips = q.getResultList();

        if (!eips.isEmpty()) {
            sql = "select vm from VmInstanceVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.uuid = :nicUuid";
            TypedQuery<VmInstanceVO> vq = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
            vq.setParameter("nicUuid", vmNicUuid);
            VmInstanceVO vm = vq.getSingleResult();

            List<String> eipStr = eips.stream().map(eip -> String.format("(name:%s, ip:%s)", eip.getName(), eip.getVipIp())).collect(Collectors.toList());

            throw new ApiMessageInterceptionException(operr("the vm[name:%s, uuid:%s] already has some EIPs%s attached", vm.getName(), vm.getUuid(),
                            StringUtils.join(eipStr, ",")));
        }
    }

    private void validate(APIAttachEipMsg msg) {
//        Note(WeiW): Disable this since eip and portforwarding can be used at same time
//        isVmNicUsedByPortForwarding(msg.getVmNicUuid());
    }

    private void validate(APICreateEipMsg msg) {
        List<String> useFor = Q.New(VipNetworkServicesRefVO.class).select(VipNetworkServicesRefVO_.serviceType).eq(VipNetworkServicesRefVO_.vipUuid, msg.getVipUuid()).listValues();
        if(useFor != null && !useFor.isEmpty()) {
            VipUseForList useForList = new VipUseForList(useFor);
            if (!useForList.validateNewAdded(EipConstant.EIP_NETWORK_SERVICE_TYPE)) {
                throw new ApiMessageInterceptionException(operr("the vip[uuid:%s] already has bound to other service[%s]", msg.getVipUuid(), useForList.toString()));
            }
        }
    }

    private void validate(APICreateLoadBalancerListenerMsg msg){
        String vipUuid = Q.New(LoadBalancerVO.class).eq(LoadBalancerVO_.uuid, msg.getLoadBalancerUuid()).select(LoadBalancerVO_.vipUuid).findValue();
        RangeSet.Range cur = new RangeSet.Range(msg.getLoadBalancerPort(), msg.getLoadBalancerPort());
        checkVipPortConfliction(vipUuid, msg.getProtocol(), cur);
    }

    private RangeSet getVipPortRangeList(String vipUuid, String protocol){
        List<String> useFor = Q.New(VipNetworkServicesRefVO.class).select(VipNetworkServicesRefVO_.serviceType).eq(VipNetworkServicesRefVO_.vipUuid, vipUuid).listValues();
        VipUseForList useForList = new VipUseForList(useFor);

        List<RangeSet.Range> portRangeList = new ArrayList<RangeSet.Range>();
        for (VipGetUsedPortRangeExtensionPoint ext : pluginRgty.getExtensionList(VipGetUsedPortRangeExtensionPoint.class)){
            RangeSet range = ext.getVipUsePortRange(vipUuid, protocol, useForList);
            portRangeList.addAll(range.getRanges());
        }

        RangeSet portRange = new RangeSet();
        portRange.setRanges(portRangeList);
        portRange.sort();
        return portRange;
    }

    private void checkVipPortConfliction(String vipUuid, String protocol, RangeSet.Range range){

        RangeSet portRangeList = getVipPortRangeList(vipUuid, protocol);
        portRangeList.sort();

        Iterator<RangeSet.Range> it = portRangeList.getRanges().iterator();
        while (it.hasNext()){
            RangeSet.Range cur = it.next();
            if (cur.isOverlap(range) || range.isOverlap(cur)){
                if (cur.getSystem()) {
                    throw new ApiMessageInterceptionException(operr("Current port range[%s, %s] is conflicted with system service port range [%s, %s] with vip[uuid: %s] protocol: %s ",
                            Long.toString(range.getStart()), Long.toString(range.getEnd()), Long.toString(cur.getStart()), Long.toString(cur.getEnd()), vipUuid, protocol));
                } else {
                    throw new ApiMessageInterceptionException(operr("Current port range[%s, %s] is conflicted with used port range [%s, %s] with vip[uuid: %s] protocol: %s ",
                            Long.toString(range.getStart()), Long.toString(range.getEnd()), Long.toString(cur.getStart()), Long.toString(cur.getEnd()), vipUuid, protocol));
                }
            }
        }
    }
}
