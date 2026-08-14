package org.zstack.network.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.Component;
import org.zstack.header.Service;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.header.network.l3.L3NetworkDnsVO;
import org.zstack.header.network.l3.L3NetworkDnsVO_;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicHelper;
import org.zstack.header.vm.VmNicSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 7:53 PM
 * To change this template use File | Settings | File Templates.
 */

//TODO: implement remove dns when dns is removed from database
public class DnsExtension extends AbstractNetworkServiceExtension implements Component, Service {
    private static final CLogger logger = Utils.getLogger(DnsExtension.class);
    private Map<NetworkServiceProviderType, NetworkServiceDnsBackend> dnsBackends = new HashMap<NetworkServiceProviderType, NetworkServiceDnsBackend>();

    private final String RESULT = String.format("result.%s", DnsExtension.class.getName());

    @Autowired
    private CloudBus bus;

    public NetworkServiceType getNetworkServiceType() {
        return NetworkServiceType.DNS;
    }

    private void doDns(final Iterator<Map.Entry<NetworkServiceDnsBackend, List<DnsStruct>>> it, final VmInstanceSpec spec, final Completion complete) {
        if (!it.hasNext()) {
            complete.success();
            return;
        }

        Map.Entry<NetworkServiceDnsBackend, List<DnsStruct>> e = it.next();
        NetworkServiceDnsBackend bkd = e.getKey();
        List<DnsStruct> structs = e.getValue();
        logger.debug(String.format("%s is applying DNS service", bkd.getClass().getName()));
        bkd.applyDnsService(structs, spec, new Completion(complete) {
            @Override
            public void success() {
                doDns(it, spec, complete);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                complete.fail(errorCode);
            }
        });
    }

    @Override
    public void applyNetworkService(VmInstanceSpec spec, Map<String, Object> data, Completion complete) {
        Map<NetworkServiceDnsBackend, List<DnsStruct>> entries = workoutDns(spec);
        data.put(RESULT, entries);
        doDns(entries.entrySet().iterator(), spec, complete);
    }

    @Override
    public void releaseNetworkService(VmInstanceSpec spec, Map<String, Object> data, NoErrorCompletion completion) {
        completion.done();
    }

    private void populateExtensions() {
        for (NetworkServiceDnsBackend extp : pluginRgty.getExtensionList(NetworkServiceDnsBackend.class)) {
            NetworkServiceDnsBackend old = dnsBackends.get(extp.getProviderType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate NetworkServiceDnsBackend[%s, %s] for type[%s]",
                        extp.getClass().getName(), old.getClass().getName(), extp.getProviderType()));
            }
            dnsBackends.put(extp.getProviderType(), extp);
        }
    }

    private List<String> getDnsOnL3(String l3NetworkUuid) {
        SimpleQuery<L3NetworkDnsVO> q = dbf.createQuery(L3NetworkDnsVO.class);
        q.select(L3NetworkDnsVO_.dns);
        q.add(L3NetworkDnsVO_.l3NetworkUuid, SimpleQuery.Op.EQ, l3NetworkUuid);
        return q.listValue();
    }

    private DnsStruct makeDnsStruct(VmInstanceSpec spec, L3NetworkInventory l3) {
        List<String> dns = getDnsOnL3(l3.getUuid());
        DnsStruct struct = new DnsStruct();
        struct.setDns(dns);
        struct.setL3Network(l3);
        return struct;
    }

    private Map<NetworkServiceDnsBackend, List<DnsStruct>> workoutDns(VmInstanceSpec spec) {
        Map<NetworkServiceDnsBackend, List<DnsStruct>> map = new HashMap<NetworkServiceDnsBackend, List<DnsStruct>>();
        Map<NetworkServiceProviderType, List<L3NetworkInventory>> providerMap = getNetworkServiceProviderMap(NetworkServiceType.DNS,
                VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks()));

        for (Map.Entry<NetworkServiceProviderType, List<L3NetworkInventory>> e : providerMap.entrySet()) {
            NetworkServiceProviderType ptype = e.getKey();
            List<DnsStruct> lst = new ArrayList<DnsStruct>();

            for (L3NetworkInventory l3 : e.getValue()) {
                lst.add(makeDnsStruct(spec, l3));
            }

            NetworkServiceDnsBackend bkd = dnsBackends.get(ptype);
            if (bkd == null) {
                throw new CloudRuntimeException(String.format("unable to find NetworkServiceDnsBackend[provider type: %s]", ptype));
            }
            map.put(bkd, lst);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("DNS Backend[%s] is about to apply entries: \n%s", bkd.getClass().getName(), lst));
            }
        }

        return map;
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
        if (msg instanceof AddDnsMsg) {
            handle((AddDnsMsg) msg);
        } else if (msg instanceof RemoveDnsMsg) {
            handle((RemoveDnsMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final RemoveDnsMsg msg) {
        final RemoveDnsReply reply = new RemoveDnsReply();
        L3NetworkInventory l3 = L3NetworkInventory.valueOf(dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class));
        NetworkServiceProviderType ptype = getNetworkServiceProviderType(NetworkServiceType.DNS, l3);
        if (ptype == null) {
            bus.reply(msg, reply);
            return;
        }

        NetworkServiceDnsBackend bkd = dnsBackends.get(ptype);
        bkd.removeDns(l3, list(msg.getDns()), new Completion(msg) {
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

    private void handle(final AddDnsMsg msg) {
        final AddDnsReply reply = new AddDnsReply();
        L3NetworkInventory l3 = L3NetworkInventory.valueOf(dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class));
        NetworkServiceProviderType ptype = getNetworkServiceProviderType(NetworkServiceType.DNS, l3);
        if (ptype == null) {
            bus.reply(msg, reply);
            return;
        }

        NetworkServiceDnsBackend bkd = dnsBackends.get(ptype);
        bkd.addDns(l3, list(msg.getDns()), new Completion(msg) {
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
        return bus.makeLocalServiceId(NetworkServiceConstants.DNS_SERVICE_ID);
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
