package org.zstack.ipsec;

import org.apache.commons.net.util.SubnetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmHaStatus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO_;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.network.service.vip.VipNetworkServicesRefVO;
import org.zstack.network.service.vip.VipNetworkServicesRefVO_;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.utils.VipUseForList;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import java.util.*;

import static org.zstack.core.Platform.argerr;

/**
 * Created by xing5 on 2016/11/16.
 */
@InterceptorForService("ipsec")
public class IPsecApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VirtualRouterHaBackend haBackend;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateIPsecConnectionMsg) {
            validate((APICreateIPsecConnectionMsg) msg);
        } else if (msg instanceof APIAttachL3NetworksToIPsecConnectionMsg) {
            validate((APIAttachL3NetworksToIPsecConnectionMsg) msg);
        } else if (msg instanceof APIAddRemoteCidrsToIPsecConnectionMsg) {
            validate((APIAddRemoteCidrsToIPsecConnectionMsg) msg);
        } else if (msg instanceof APIDetachL3NetworksFromIPsecConnectionMsg) {
            validate((APIDetachL3NetworksFromIPsecConnectionMsg) msg);
        } else if (msg instanceof APIRemoveRemoteCidrsFromIPsecConnectionMsg) {
            validate((APIRemoveRemoteCidrsFromIPsecConnectionMsg) msg);
        } if (msg instanceof APIChangeIPSecConnectionStateMsg) {
            validate((APIChangeIPSecConnectionStateMsg) msg);
        } if (msg instanceof APIReconnectIPsecConnectionMsg) {
            validate((APIReconnectIPsecConnectionMsg) msg);
        }

        return msg;
    }

    private void validateNetworkService(String l3NetworkUuid){
        if (Q.New(NetworkServiceL3NetworkRefVO.class).eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, l3NetworkUuid)
                .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString()).find() == null) {
            throw new ApiMessageInterceptionException(argerr("Network [uuid: %s] does't not have IPsec service", l3NetworkUuid));
        }
    }

    private void validateIPsecRemoteCidrs(List<String> l3NetworkUuids, List<String> remoteCidrs) {
        /* verify remote cidrs */
        Set<String> cidrs = new HashSet<>();
        for (String rcidr : remoteCidrs) {
            if (cidrs.contains(rcidr)) {
                throw new ApiMessageInterceptionException(argerr("the remote CIDR[%s] is same to existed cidrs", rcidr));
            }
            cidrs.add(rcidr);
        }

        for (String rcidr : remoteCidrs) {
            for (String tempCidr : remoteCidrs) {
                if (tempCidr.equals(rcidr)) {
                    continue;
                }

                if (NetworkUtils.isCidrOverlap(rcidr, tempCidr)) {
                    throw new ApiMessageInterceptionException(argerr("the remote CIDR[%s] and remote CIDR[%s] are overlaped",
                            rcidr, tempCidr));
                }
            }
        }

        /* get all ip range from local network */
        List<NormalIpRangeVO> iprVos = new ArrayList<>();
        for (String l3NetworkUuid : l3NetworkUuids) {
            NormalIpRangeVO vo = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, l3NetworkUuid).eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4).limit(1).find();
            if (vo != null) {
                iprVos.add(vo);
            }
        }
        if (iprVos.isEmpty()) {
            return;
        }

        /* map ipRange to CIDR  */
        for (NormalIpRangeVO range : iprVos) {
            String lcidr = range.getNetworkCidr();
            for (String tempCidr : cidrs) {
                if (NetworkUtils.isCidrOverlap(lcidr, tempCidr)) {
                    throw new ApiMessageInterceptionException(argerr("the CIDR[%s] of local router and remote CIDR[%s] are overlaped",
                            lcidr, tempCidr));
                }
            }
            cidrs.add(lcidr);
        }
    }

    private void validateIPsecL3Network(List<String> l3NetworkUuids, String peerAddress, List<String> peerCidrs, String ipsecUuid) {
        /* 1. all network belong to same type: L3BasicNetwork or L3VpcNetwork */
        List<String> types = Q.New(L3NetworkVO.class).in(L3NetworkVO_.uuid, l3NetworkUuids).select(L3NetworkVO_.type)
                .groupBy(L3NetworkVO_.type).listValues();
        if (types != null && types.size() > 1) {
            throw new ApiMessageInterceptionException(argerr("all networks in same IPsecConnection should be same type"));
        }

        /* 2. for l3 basic network, only 1 l3 networks is acceptable */
        String type = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, l3NetworkUuids.get(0)).select(L3NetworkVO_.type).findValue();
        if (type.equals(L3NetworkConstant.L3_BASIC_NETWORK_TYPE) && l3NetworkUuids.size() > 1) {
            throw new ApiMessageInterceptionException(argerr("IPsecConnection can ONLY have 1 network for %s", L3NetworkConstant.L3_BASIC_NETWORK_TYPE));
        }

        /* 3. get all l3 networks belong to same vpc router */
        List<String> l3NetworkAll = new ArrayList<>();
        if (type.equals(VpcConstants.VPC_L3_NETWORK_TYPE)){
            Set<String> vrUuids = new HashSet<>();
            String masterUuid = null;
            for (String l3Uuid: l3NetworkUuids) {
                List<String> uuids = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, l3Uuid).in(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST)
                        .select(VmNicVO_.vmInstanceUuid).listValues();
                if (uuids == null || uuids.isEmpty()) {
                    throw new ApiMessageInterceptionException(argerr("L3Network [uuid: %s] has not been attached to vpc router", l3Uuid));
                }

                for (String uuid : uuids) {
                    String haUuid = haBackend.getVirtualRouterHaUuid(uuid);
                    if (haUuid != null) {
                        vrUuids.add(haUuid);
                        VirtualRouterVmVO vrVo = dbf.findByUuid(uuid, VirtualRouterVmVO.class);
                        if (vrVo.getHaStatus() == ApplianceVmHaStatus.Master) {
                            masterUuid = uuid;
                        }
                    } else {
                        vrUuids.add(uuid);
                        masterUuid = uuid;
                    }
                }
            }

            if (vrUuids.size() > 1) {
                throw new ApiMessageInterceptionException(argerr("all networks in same IPsecConnection must be attached to same VPC router"));
            } else {
                if (masterUuid == null) {
                    throw new ApiMessageInterceptionException(argerr("there is no master vpc for ha group %s", vrUuids.toArray()[0]));
                }
                List<String> l3Networks = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, masterUuid)
                        .in(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST).select(VmNicVO_.l3NetworkUuid).listValues();
                l3NetworkAll.addAll(l3Networks);
            }
            if(Q.New(VirtualRouterSoftwareVersionVO.class)
                    .eq(VirtualRouterSoftwareVersionVO_.uuid, masterUuid)
                    .eq(VirtualRouterSoftwareVersionVO_.currentVersion, VirtualRouterConstant.IPSEC_NETWORK_SERVICE_OLD_VERSION)
                    .isExists()) {
                throw new ApiMessageInterceptionException(argerr("there is a vpc[%s] using old ipsec plugin, upgrade it to create ipsec", masterUuid));
            }
        } else {
            l3NetworkAll.add(l3NetworkUuids.get(0));
        }

        /* 4. same router and same peerAddr can create only 1 ipsec */
        String sql = "select ipsec.uuid, ipsec.name from IPsecConnectionVO ipsec, IPsecL3NetworkRefVO ref where ipsec.uuid=ref.connectionUuid and " +
                "ipsec.peerAddress=:peerAddr and ref.l3NetworkUuid in (:l3NetworkUuids)";
        List<Tuple> tuples = SQL.New(sql, Tuple.class).param("peerAddr", peerAddress).param("l3NetworkUuids", l3NetworkAll).list();
        if (tuples != null && !tuples.isEmpty()) {
            /* for new create, there should NOT have same ipsec */
            if (ipsecUuid == null) {
                throw new ApiMessageInterceptionException(argerr("there already have ipsec connection[uuid:%s, name:%s] with the same vrouter and" +
                        " peerAddress", tuples.get(0).get(0, String.class), tuples.get(0).get(1, String.class)));
            } else {
                /* for attach, can not create 2 ipsec with peer on same router */
                String uuid = tuples.get(0).get(0, String.class);
                if (!ipsecUuid.equals(uuid)) {
                    throw new ApiMessageInterceptionException(argerr("there already have ipsec connection[uuid:%s, name:%s] with the same vrouter and" +
                            " peerAddress", tuples.get(0).get(0, String.class), tuples.get(0).get(1, String.class)));
                }
            }
        }

        /* 5. cidrs of local network and remote cidrs should not be overlapped */
        if (peerCidrs != null) {
            validateIPsecRemoteCidrs(l3NetworkAll, peerCidrs);
        }
    }

    private void validate(APICreateIPsecConnectionMsg msg) {
        VipVO vip = dbf.findByUuid(msg.getVipUuid(), VipVO.class);
        List<String> useFor = Q.New(VipNetworkServicesRefVO.class).select(VipNetworkServicesRefVO_.serviceType).eq(VipNetworkServicesRefVO_.vipUuid, msg.getVipUuid()).listValues();
        if(useFor != null && !useFor.isEmpty()){
            VipUseForList useForList = new VipUseForList(useFor);
            if (!useForList.validateNewAdded(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString())){
                throw new ApiMessageInterceptionException(argerr("the vip[uuid:%s] has been used for %s", msg.getVipUuid(), useForList.toString()));
            }
        }

        if (vip.getIp().equals(msg.getPeerAddress())) {
            throw new ApiMessageInterceptionException(argerr("the peerAddress[%s] cannot be the same to the VIP address", msg.getPeerAddress()));
        }

        if (!NetworkUtils.isIpv4Address(msg.getPeerAddress())) {
            throw new ApiMessageInterceptionException(argerr("the peerAddress[%s] is not an IPv4 address", msg.getPeerAddress()));
        }

        /* l3network is optional parameters */
        if (msg.getL3NetworkUuid() != null) {
            validateNetworkService(msg.getL3NetworkUuid());
            validateIPsecL3Network(Collections.singletonList(msg.getL3NetworkUuid()), msg.getPeerAddress(), msg.getPeerCidrs(), null);
        }

        if (msg.getAuthMode().equals("psk")) {
            String key = msg.getAuthKey();
            if (key.contains("\"") || key.contains(" ") || key.contains("`") || key.contains("\\") || key.contains("'")) {
                throw new ApiMessageInterceptionException(argerr(
                        "the authKey cannot contain white space and special characters of '\"`\\"
                ));
            }
        }

        /* the vip can not the first of the last ip of the cidr */
        VipVO vipVO = dbf.findByUuid(msg.getVipUuid(), VipVO.class);
        if (NetworkUtils.isIpv4Address(vipVO.getIp())) {
            AddressPoolVO addressPoolVO = dbf.findByUuid(vipVO.getIpRangeUuid(), AddressPoolVO.class);
            if (addressPoolVO == null) {
                return;
            }

            SubnetUtils utils = new SubnetUtils(addressPoolVO.getNetworkCidr());
            SubnetUtils.SubnetInfo subnet = utils.getInfo();
            String firstIp = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(subnet.getLowAddress()) - 1);
            String lastIp = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(subnet.getHighAddress()) + 1);
            if (vipVO.getIp().equals(firstIp) || vipVO.getIp().equals(lastIp)) {
                throw new ApiMessageInterceptionException(argerr("Ipsec VIP [%s] cannot be the first or the last IP of the CIDR with the public address pool type", vipVO.getIp()));
            }
        }
    }

    private void validate(APIAttachL3NetworksToIPsecConnectionMsg msg) {
        if (msg.getL3NetworkUuids() == null || msg.getL3NetworkUuids().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("must include l3 networks in APIAttachL3NetworksToIPsecConnectionMsg"));
        }

        IPsecConnectionInventory inv = IPsecConnectionInventory.valueOf(dbf.findByUuid(msg.getIPsecConnectionUuid(), IPsecConnectionVO.class));
        for (String l3NetworkUuid: msg.getL3NetworkUuids()) {
            L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(l3NetworkUuid, L3NetworkVO.class));
            validateNetworkService(l3NetworkUuid);
            if (l3Inv.getType().equals(L3NetworkConstant.L3_BASIC_NETWORK_TYPE)) {
                throw new ApiMessageInterceptionException(argerr("L3 network [%s] is not vpc network, can not be attached or detached to ipsec", l3NetworkUuid));
            }

            /* l3 network can not be attached again */
            if (inv.getLocalL3Networks().contains(l3NetworkUuid)) {
                throw new ApiMessageInterceptionException(argerr("L3 network [%s] can not be attached to ipsec [uuid :%s]twice", l3NetworkUuid, msg.getIPsecConnectionUuid()));
            }
        }

        List<String> l3NetworkUuids = new ArrayList<>(msg.getL3NetworkUuids());
        l3NetworkUuids.addAll(inv.getLocalL3Networks());

        validateIPsecL3Network(l3NetworkUuids, inv.getPeerAddress(), inv.getPeerCidrSignatures(), msg.getUuid());
    }

    private void validate(APIDetachL3NetworksFromIPsecConnectionMsg msg) {
        if (msg.getL3NetworkUuids() == null || msg.getL3NetworkUuids().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("must include l3 networks in APIAttachL3NetworksToIPsecConnectionMsg"));
        }

        IPsecConnectionInventory inv = IPsecConnectionInventory.valueOf(dbf.findByUuid(msg.getIPsecConnectionUuid(), IPsecConnectionVO.class));
        for (String l3NetworkUuid: msg.getL3NetworkUuids()) {
            L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(l3NetworkUuid, L3NetworkVO.class));
            if (l3Inv.getType().equals(L3NetworkConstant.L3_BASIC_NETWORK_TYPE)) {
                throw new ApiMessageInterceptionException(argerr("L3 network [%s] is not vpc network, can not be attached or detached to ipsec", l3NetworkUuid));
            }

            if (!inv.getLocalL3Networks().contains(l3NetworkUuid)) {
                throw new ApiMessageInterceptionException(argerr("L3 network [%s] is not be attached to ipsec [uuid :%s]", l3NetworkUuid, msg.getIPsecConnectionUuid()));
            }
        }
    }

    private void validate(APIAddRemoteCidrsToIPsecConnectionMsg msg) {
        IPsecConnectionInventory inv = IPsecConnectionInventory.valueOf(dbf.findByUuid(msg.getUuid(), IPsecConnectionVO.class));

        for (String cidr: msg.getPeerCidrs()) {
            if (inv.getPeerCidrSignatures().contains(cidr)) {
                throw new ApiMessageInterceptionException(argerr("Cidr [%s] is already in the Cidrs of ipsec [uuid :%s]", cidr, msg.getIPsecConnectionUuid()));
            }
        }

        List<String> remoteCidrs = new ArrayList<>(msg.getPeerCidrs());
        remoteCidrs.addAll(inv.getPeerCidrSignatures());
        validateIPsecRemoteCidrs(inv.getLocalL3Networks(), remoteCidrs);
    }

    private void validate(APIRemoveRemoteCidrsFromIPsecConnectionMsg msg) {
        IPsecConnectionInventory inv = IPsecConnectionInventory.valueOf(dbf.findByUuid(msg.getUuid(), IPsecConnectionVO.class));

        for (String cidr: msg.getPeerCidrs()) {
            if (!inv.getPeerCidrSignatures().contains(cidr)) {
                throw new ApiMessageInterceptionException(argerr("Cidr [%s] is not in Cidrs of ipsec [uuid :%s]", cidr, msg.getIPsecConnectionUuid()));
            }
        }
    }

    private void validate(APIChangeIPSecConnectionStateMsg msg) {
        IPsecConnectionInventory inv = IPsecConnectionInventory.valueOf(dbf.findByUuid(msg.getUuid(), IPsecConnectionVO.class));
        if (!inv.getStatus().equals(IPSecStatus.Ready.toString())) {
            throw new ApiMessageInterceptionException(argerr("can not change state because ipsec [uuid:%s] status is not ready", msg.getUuid()));
        }
    }

    private void validate(APIReconnectIPsecConnectionMsg msg) {
        if (IpSecconnectionSystemTags.IPSEC_LOW_VERSION.getTag(msg.getUuid(), IPsecConnectionVO.class) != null) {
            throw new ApiMessageInterceptionException(argerr("could not reconnect this ipsec [uuid:%s], please upgrade ipsec version", msg.getUuid()));
        }
    }
}
