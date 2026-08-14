package org.zstack.network.service;

import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.ForwardDnsStruct;
import org.zstack.header.network.service.NetworkServiceCentralizedDnsBackend;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicHelper;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

/**
 * Created by AlanJager on 2017/7/8.
 */
public class CentralizedDnsExtension extends AbstractNetworkServiceExtension implements Component {
    private static final CLogger logger = Utils.getLogger(CentralizedDnsExtension.class);
    private final Map<NetworkServiceProviderType, NetworkServiceCentralizedDnsBackend> cDnsBackends = new HashMap<NetworkServiceProviderType, NetworkServiceCentralizedDnsBackend>();

    private final String RESULT = String.format("result.%s", CentralizedDnsExtension.class.getName());

    @Override
    public NetworkServiceType getNetworkServiceType() {
        return NetworkServiceType.Centralized_DNS;
    }

    @Override
    public void applyNetworkService(VmInstanceSpec spec, Map<String, Object> data, Completion completion) {
        completion.success();
    }


    private void doForwardDns(final Iterator<Map.Entry<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>>> it, final VmInstanceSpec spec, final Completion complete) {
        if (!it.hasNext() || new CentralizedDnsValidator().validate(spec.getVmInventory().getUuid())) {
            complete.success();
            return;
        }

        Map.Entry<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>> e = it.next();
        NetworkServiceCentralizedDnsBackend bkd = e.getKey();
        List<ForwardDnsStruct> structs = e.getValue();
        logger.debug(String.format("%s is applying centralized dns service", bkd.getClass().getName()));
        bkd.applyForwardDnsService(structs, spec, new Completion(complete) {
            @Override
            public void success() {
                doForwardDns(it, spec, complete);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                complete.fail(errorCode);
            }
        });
    }

    private Map<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>> workoutForwardDns(VmInstanceSpec spec) {
        Map<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>> map = new HashMap<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>>();
        Map<NetworkServiceProviderType, List<L3NetworkInventory>> providerMap = getNetworkServiceProviderMap(NetworkServiceType.Centralized_DNS,
                VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks()));

        for (Map.Entry<NetworkServiceProviderType, List<L3NetworkInventory>> e : providerMap.entrySet()) {
            NetworkServiceProviderType ptype = e.getKey();
            List<ForwardDnsStruct> lst = new ArrayList<ForwardDnsStruct>();

            for (L3NetworkInventory l3 : e.getValue()) {
                lst.add(makeForwardDnsStruct(spec, l3));
            }

            NetworkServiceCentralizedDnsBackend bkd = cDnsBackends.get(ptype);
            if (bkd == null) {
                throw new CloudRuntimeException(String.format("unable to find NetworkServiceDhcpBackend[provider type: %s]", ptype));
            }
            map.put(bkd, lst);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("DHCP Backend[%s] is about to apply entries: \n%s", bkd.getClass().getName(), lst));
            }
        }

        return map;
    }

    private ForwardDnsStruct makeForwardDnsStruct(VmInstanceSpec spec, final L3NetworkInventory l3) {
        VmNicInventory nic = null;
        for (VmNicInventory inv : spec.getDestNics()) {
            if (VmNicHelper.getL3Uuids(inv).contains(l3.getUuid())) {
                nic = inv;
                break;
            }
        }

        ForwardDnsStruct struct = new ForwardDnsStruct();
        struct.setL3Network(l3);
        if (nic != null) {
            struct.setMac(nic.getMac());
        }

        return struct;
    }

    @Override
    public void releaseNetworkService(VmInstanceSpec spec, Map<String, Object> data, NoErrorCompletion completion) {
        completion.done();
    }

    private void releaseForwardDns(final Iterator<Map.Entry<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>>> it, final VmInstanceSpec spec, final NoErrorCompletion complete) {
        if (!it.hasNext() || new CentralizedDnsValidator().validate(spec.getVmInventory().getUuid())) {
            complete.done();
            return;
        }
        if (!Optional.ofNullable(spec.getDestHost()).isPresent()){
            complete.done();
            return;
        }
        Map.Entry<NetworkServiceCentralizedDnsBackend, List<ForwardDnsStruct>> e = it.next();
        NetworkServiceCentralizedDnsBackend bkd = e.getKey();
        List<ForwardDnsStruct> structs = e.getValue();
        logger.debug(String.format("%s is applying centralized dns service", bkd.getClass().getName()));
        bkd.releaseForwardDnsService(structs, spec, new NoErrorCompletion(complete) {
            @Override
            public void done() {
                complete.done();
            }
        });
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

    private void populateExtensions() {
        for (NetworkServiceCentralizedDnsBackend extp : pluginRgty.getExtensionList(NetworkServiceCentralizedDnsBackend.class)) {
            NetworkServiceCentralizedDnsBackend old = cDnsBackends.get(extp.getProviderType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate NetworkServiceDnsBackend[%s, %s] for type[%s]",
                        extp.getClass().getName(), old.getClass().getName(), extp.getProviderType()));
            }
            cDnsBackends.put(extp.getProviderType(), extp);
        }
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
