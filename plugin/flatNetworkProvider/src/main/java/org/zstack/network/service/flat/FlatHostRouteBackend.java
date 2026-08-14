package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.core.Completion;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class FlatHostRouteBackend implements NetworkServiceHostRouteBackend, DhcpServerExtensionPoint, PrepareDbInitialValueExtensionPoint {
    private static final CLogger logger = Utils.getLogger(FlatHostRouteBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public NetworkServiceProviderType getProviderType() {
        return FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE;
    }

    @Override
    public void addHostRoute(String l3Uuid, List<AddHostRouteMsg> routes, Completion completion) {
        /** hostroute will not install to vm immediately, but change the dnsmasq
         * once vm reboot or use restart dhcp process, host route will be installed to vm */
        L3NetworkUpdateDhcpMsg msg = new L3NetworkUpdateDhcpMsg();
        msg.setL3NetworkUuid(l3Uuid);
        bus.makeTargetServiceIdByResourceUuid(msg, FlatNetworkServiceConstant.SERVICE_ID, l3Uuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void removeHostRoute(String l3Uuid, List<RemoveHostRouteMsg> routes, Completion completion) {
        /** hostroute will not install to vm immediately, but change the dnsmasq
         * once vm reboot or use restart dhcp process, host route will be installed to vm */
        L3NetworkUpdateDhcpMsg msg = new L3NetworkUpdateDhcpMsg();
        msg.setL3NetworkUuid(l3Uuid);
        bus.makeTargetServiceIdByResourceUuid(msg, FlatNetworkServiceConstant.SERVICE_ID, l3Uuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void afterAllocateDhcpServerIP(String L3NetworkUuid, String dhcpSererIp) {
        /* only ipv4 network need a static route for userdata service */
        if (!NetworkUtils.isIpv4Address(dhcpSererIp)) {
            return;
        }

        /* skip adding host route for network without host route service */
        boolean refExists = Q.New(NetworkServiceL3NetworkRefVO.class).eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, L3NetworkUuid)
                .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, NetworkServiceType.HostRoute.toString())
                .isExists();
        if (!refExists) {
            logger.debug(String.format("L3 Network doesn't has %s service", NetworkServiceType.HostRoute.toString()));
            return;
        }

        if (!Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, L3NetworkUuid).isExists()) {
            return;
        }

        updateMetadataRoute(L3NetworkUuid, dhcpSererIp);
    }

    private void updateMetadataRoute(String L3NetworkUuid, String dhcpSererIp) {
        L3NetworkHostRouteVO vo = Q.New(L3NetworkHostRouteVO.class).eq(L3NetworkHostRouteVO_.l3NetworkUuid, L3NetworkUuid)
                .eq(L3NetworkHostRouteVO_.prefix, NetworkServiceConstants.METADATA_HOST_PREFIX).find();
        if (vo != null) {
            if (vo.getNexthop().equals(dhcpSererIp)) {
                return;
            }
            vo.setNexthop(dhcpSererIp);
            dbf.update(vo);
            return;
        }

        vo = new L3NetworkHostRouteVO();
        vo.setL3NetworkUuid(L3NetworkUuid);
        vo.setPrefix(NetworkServiceConstants.METADATA_HOST_PREFIX);
        vo.setNexthop(dhcpSererIp);
        dbf.persist(vo);
    }

    @Override
    public void afterRemoveDhcpServerIP(String L3NetworkUuid, String dhcpSererIp) {
        SQL.New(L3NetworkHostRouteVO.class).eq(L3NetworkHostRouteVO_.l3NetworkUuid, L3NetworkUuid)
                .eq(L3NetworkHostRouteVO_.prefix, NetworkServiceConstants.METADATA_HOST_PREFIX).delete();
    }

    @Override
    public void prepareDbInitialValue() {
        SimpleQuery<NetworkServiceProviderVO> query = dbf.createQuery(NetworkServiceProviderVO.class);
        query.add(NetworkServiceProviderVO_.type, SimpleQuery.Op.EQ, FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        NetworkServiceProviderVO rpvo = query.find();
        if (rpvo != null) {
            // check if any network service type missing, if any, complement them
            SimpleQuery<NetworkServiceTypeVO> q = dbf.createQuery(NetworkServiceTypeVO.class);
            q.add(NetworkServiceTypeVO_.networkServiceProviderUuid, SimpleQuery.Op.EQ, rpvo.getUuid());
            List<NetworkServiceTypeVO> refs = q.list();
            Set<String> types = new HashSet<String>();
            for (NetworkServiceTypeVO ref : refs) {
                types.add(ref.getType());
            }

            if (!types.contains(NetworkServiceType.HostRoute.toString())) {
                NetworkServiceTypeVO ref = new NetworkServiceTypeVO();
                ref.setNetworkServiceProviderUuid(rpvo.getUuid());
                ref.setType(NetworkServiceType.HostRoute.toString());
                dbf.persist(ref);
            }

            return;
        }

        rpvo = new NetworkServiceProviderVO();
        rpvo.setUuid(Platform.getUuid());
        rpvo.setName("Flat Network Service Provider");
        rpvo.setDescription("Flat Network Service Provider");
        rpvo.getNetworkServiceTypes().add(NetworkServiceType.HostRoute.toString());
        rpvo.setType(FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        dbf.persistAndRefresh(rpvo);
    }
}
