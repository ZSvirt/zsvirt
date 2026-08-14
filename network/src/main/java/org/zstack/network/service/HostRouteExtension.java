package org.zstack.network.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.header.Component;
import org.zstack.header.Service;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;


public class HostRouteExtension extends AbstractNetworkServiceExtension implements Component, Service {
    private static final CLogger logger = Utils.getLogger(HostRouteExtension.class);
    private Map<NetworkServiceProviderType, NetworkServiceHostRouteBackend> hostRouteBackends = new HashMap<NetworkServiceProviderType, NetworkServiceHostRouteBackend>();

    @Autowired
    private CloudBus bus;

    public NetworkServiceType getNetworkServiceType() {
        return NetworkServiceType.HostRoute;
    }

    @Override
    public void applyNetworkService(VmInstanceSpec spec, Map<String, Object> data, Completion complete) {
        /* host route is applied by dhcp extesion */
        complete.success();
    }

    @Override
    public void releaseNetworkService(VmInstanceSpec spec, Map<String, Object> data, NoErrorCompletion completion) {
        completion.done();
    }

    private void populateExtensions() {
        for (NetworkServiceHostRouteBackend extp : pluginRgty.getExtensionList(NetworkServiceHostRouteBackend.class)) {
            NetworkServiceHostRouteBackend old = hostRouteBackends.get(extp.getProviderType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate NetworkServiceHostRouteBackend[%s, %s] for type[%s]",
                        extp.getClass().getName(), old.getClass().getName(), extp.getProviderType()));
            }
            hostRouteBackends.put(extp.getProviderType(), extp);
        }
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof AddHostRouteMsg) {
            handle((AddHostRouteMsg) msg);
        } else if (msg instanceof RemoveHostRouteMsg) {
            handle((RemoveHostRouteMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final RemoveHostRouteMsg msg) {
        final RemoveHostRouteReply reply = new RemoveHostRouteReply();
        L3NetworkInventory l3 = L3NetworkInventory.valueOf(dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class));
        NetworkServiceProviderType ptype = getNetworkServiceProviderType(NetworkServiceType.HostRoute, l3);
        if (ptype == null) {
            reply.setError(operr("L3Network [uuid: %s] provide type null", msg.getL3NetworkUuid()));
            bus.reply(msg, reply);
            return;
        }

        NetworkServiceHostRouteBackend bkd = hostRouteBackends.get(ptype);
        bkd.removeHostRoute(msg.getL3NetworkUuid(), asList(msg), new Completion(msg) {
            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }

            @Override
            public void success() {
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final AddHostRouteMsg msg) {
        final AddHostRouteReply reply = new AddHostRouteReply();
        L3NetworkInventory l3 = L3NetworkInventory.valueOf(dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class));
        NetworkServiceProviderType ptype = getNetworkServiceProviderType(NetworkServiceType.HostRoute, l3);
        if (ptype == null) {
            reply.setError(operr("L3Network [uuid: %s] does not have host route service", msg.getL3NetworkUuid()));
            bus.reply(msg, reply);
            return;
        }

        NetworkServiceHostRouteBackend bkd = hostRouteBackends.get(ptype);
        bkd.addHostRoute(msg.getL3NetworkUuid(), asList(msg), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(NetworkServiceConstants.HOSTROUTE_SERVICE_ID);
    }

    @Override
    public int getSyncLevel() {
        return 0;
    }

    @Override
    public List<String> getAliasIds() {
        return null;
    }

    @Override
    public void enableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    public void disableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, Completion completion) {
        completion.success();
    }
}
