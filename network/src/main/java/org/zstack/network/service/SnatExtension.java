package org.zstack.network.service;

import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.network.service.NetworkServiceSnatBackend;
import org.zstack.header.network.service.SnatStruct;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class SnatExtension extends AbstractNetworkServiceExtension implements Component {
    private final static CLogger logger = Utils.getLogger(SnatExtension.class);

    private Map<NetworkServiceProviderType, NetworkServiceSnatBackend> snatBackends = new HashMap<NetworkServiceProviderType, NetworkServiceSnatBackend>();

    private final String RESULT = String.format("result.%s", SnatExtension.class.getName());

    @Override
    public NetworkServiceType getNetworkServiceType() {
        return NetworkServiceType.SNAT;
    }

    private void doSnat(final Iterator<Map.Entry<NetworkServiceSnatBackend, List<SnatStruct>>> it, final VmInstanceSpec spec, final Completion complete) {
        if (!it.hasNext()) {
            complete.success();
            return;
        }

        Map.Entry<NetworkServiceSnatBackend, List<SnatStruct>> e = it.next();
        NetworkServiceSnatBackend bkd = e.getKey();
        List<SnatStruct> structs = e.getValue();
        bkd.applySnatService(structs, spec, new Completion(complete) {
            @Override
            public void success() {
                doSnat(it, spec, complete);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                complete.fail(errorCode);
            }
        });
    }

    @Override
    public void applyNetworkService(VmInstanceSpec spec, Map<String, Object> data, Completion complete) {
        Map<NetworkServiceSnatBackend, List<SnatStruct>> entries = workoutSnat(spec);
        data.put(RESULT, entries);
        doSnat(entries.entrySet().iterator(), spec, complete);
    }

    @Override
    public void releaseNetworkService(VmInstanceSpec spec, Map<String, Object> data, NoErrorCompletion completion) {
        // never release source nat. once SNAT service is enabled on l3Network, it stays alive all the time
        // during virtual router life cycle. vm stop/destroy won't effect as SNAT is shared by all vms on this
        // network
        completion.done();
    }

    private void populateExtensions() {
        for (NetworkServiceSnatBackend extp : pluginRgty.getExtensionList(NetworkServiceSnatBackend.class)) {
            NetworkServiceSnatBackend old = snatBackends.get(extp.getProviderType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate NetworkServiceSnatBackend[%s, %s] for type[%s]",
                        extp.getClass().getName(), old.getClass().getName(), extp.getProviderType()));
            }
            snatBackends.put(extp.getProviderType(), extp);
        }
    }

    private SnatStruct makeSnatStruct(VmInstanceSpec spec, L3NetworkInventory l3) {
        VmNicInventory nic = null;
        for (VmNicInventory inv : spec.getDestNics()) {
            if (VmNicHelper.getL3Uuids(inv).contains(l3.getUuid())){
                nic = inv;
                break;
            }
        }

        SnatStruct struct = new SnatStruct();
        struct.setL3Network(l3);
        if (nic != null) {
            struct.setGuestGateway(nic.getGateway());
            struct.setGuestIp(nic.getIp());
            struct.setGuestMac(nic.getMac());
            struct.setGuestNetmask(nic.getNetmask());
        }
        return struct;
    }

    private Map<NetworkServiceSnatBackend, List<SnatStruct>> workoutSnat(VmInstanceSpec spec) {
        Map<NetworkServiceSnatBackend, List<SnatStruct>> map = new HashMap<NetworkServiceSnatBackend, List<SnatStruct>>();
        Map<NetworkServiceProviderType, List<L3NetworkInventory>> providerMap = getNetworkServiceProviderMap(NetworkServiceType.SNAT,
                VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks()));

        for (Map.Entry<NetworkServiceProviderType, List<L3NetworkInventory>> e : providerMap.entrySet()) {
            NetworkServiceProviderType ptype = e.getKey();
            List<SnatStruct> lst = new ArrayList<SnatStruct>();

            for (L3NetworkInventory l3 : e.getValue()) {
                lst.add(makeSnatStruct(spec, l3));
            }

            NetworkServiceSnatBackend bkd = snatBackends.get(ptype);
            if (bkd == null) {
                throw new CloudRuntimeException(String.format("unable to find NetworkServiceSnatBackend[provider type: %s]", ptype));
            }

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("SNAT Backend[%s] is about to apply entries: \n%s", bkd.getClass().getName(), lst));
            }

            map.put(bkd, lst);
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
    public void enableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    public void disableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, Completion completion) {
        completion.success();
    }
}
