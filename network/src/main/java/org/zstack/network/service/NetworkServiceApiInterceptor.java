package org.zstack.network.service;

import java.util.Arrays;
import java.util.List;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.*;
import org.zstack.network.l3.IpRangeHelper;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.Utils;
import org.zstack.utils.network.IPv6Constants;


import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 */
@InterceptorForService("network.service")
public class NetworkServiceApiInterceptor implements GlobalApiMessageInterceptor {
   private final static CLogger logger = Utils.getLogger(NetworkServiceApiInterceptor.class);
   @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAttachNetworkServiceToL3NetworkMsg) {
            APIAttachNetworkServiceToL3NetworkMsg attachMsg = (APIAttachNetworkServiceToL3NetworkMsg)msg;
            attachMsg.setNetworkServices(convertNetworkProviderTypeToUuid(attachMsg.getNetworkServices()));
            validate(attachMsg);
        } else if (msg instanceof APIDetachNetworkServiceFromL3NetworkMsg) {
            APIDetachNetworkServiceFromL3NetworkMsg detachMsg = (APIDetachNetworkServiceFromL3NetworkMsg)msg;
            if (detachMsg.getService() != null) {
                Map<String, List<String>> services = new HashMap<>();
                NetworkServiceL3NetworkRefVO ref = Q.New(NetworkServiceL3NetworkRefVO.class)
                        .eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, detachMsg.getL3NetworkUuid())
                        .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, detachMsg.getService()).find();
                if (ref != null) {
                    services.put(ref.getNetworkServiceProviderUuid(), asList(ref.getNetworkServiceType()));
                }
                detachMsg.setNetworkServices(services);
            } else {
                detachMsg.setNetworkServices(convertNetworkProviderTypeToUuid(detachMsg.getNetworkServices()));
            }
        }

        return msg;
    }

    private void validate(APIAttachNetworkServiceToL3NetworkMsg msg) {
        if (msg.getNetworkServices().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("networkServices cannot be empty"));
        }

        SimpleQuery<NetworkServiceTypeVO> q = dbf.createQuery(NetworkServiceTypeVO.class);
        q.add(NetworkServiceTypeVO_.networkServiceProviderUuid, Op.IN, msg.getNetworkServices().keySet());
        List<NetworkServiceTypeVO> vos = q.list();
        Map<String, Set<String>> actual = new HashMap<String, Set<String>>();
        for (NetworkServiceTypeVO vo : vos) {
            Set<String> types = actual.get(vo.getNetworkServiceProviderUuid());
            if (types == null) {
                types = new HashSet<String>();
                actual.put(vo.getNetworkServiceProviderUuid(), types);
            }

            types.add(vo.getType());
        }

        for (Map.Entry<String, List<String>> e : msg.getNetworkServices().entrySet()) {
            String puuid = e.getKey();
            List<String> types = e.getValue();
            if (types == null || types.isEmpty())  {
                throw new ApiMessageInterceptionException(argerr("network service for provider[uuid:%s] must be specified", puuid));
            }

            final Set<String> actualTypes = actual.get(puuid);
            if (actualTypes == null) {
                throw new ApiMessageInterceptionException(argerr("cannot find network service provider[uuid:%s] or it provides no services", puuid));
            }

            if (!actualTypes.containsAll(types)) {
                List<String> notSupported = transformAndRemoveNull(types, type -> {
                    if (!actualTypes.contains(type)) {
                        return type;
                    }
                    return null;
                });

                throw new ApiMessageInterceptionException(argerr("network service provider[uuid:%s] doesn't provide services%s", puuid, notSupported));
            }
        }

        SimpleQuery<NetworkServiceL3NetworkRefVO> nwsq = dbf.createQuery(NetworkServiceL3NetworkRefVO.class);
        nwsq.select(NetworkServiceL3NetworkRefVO_.networkServiceType);
        nwsq.add(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        List<String> existingNwsTypes = nwsq.listValue();

        for (List<String> types : msg.getNetworkServices().values()) {
            for (String type : types) {
                if (existingNwsTypes.contains(type)) {
                    throw new ApiMessageInterceptionException(operr("there has been a network service[%s] attached to L3 network[uuid:%s]", type, msg.getL3NetworkUuid()));
                }

                if (type.equals(NetworkServiceType.DHCP.toString())) {
                    List<IpRangeVO> ipRangeVOS = Q.New(IpRangeVO.class).
                            eq(IpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                            .list();
                    if (ipRangeVOS.isEmpty()) {
                        continue;
                    }

                    boolean isUseForUserVm = false;
                    List<String> usedVmNicUuids = Q.New(UsedIpVO.class).select(UsedIpVO_.vmNicUuid)
                             .in(UsedIpVO_.ipRangeUuid, ipRangeVOS.stream().map(IpRangeVO::getUuid).collect(Collectors.toList()))
                             .listValues();
                    if (!usedVmNicUuids.isEmpty()) {
                        List<String> usedVmInstanceUuids = Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid)
                                .in(VmNicVO_.uuid, usedVmNicUuids)
                                .listValues();
                        isUseForUserVm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.type, VmInstanceConstant.USER_VM_TYPE)
                                .in(VmInstanceVO_.uuid, usedVmInstanceUuids)
                                .isExists();
                    }

                    List<FreeIpInventory> freeIpInventories = new ArrayList<>();
                    for (IpRangeVO ipRangeVO : ipRangeVOS) {
                        List<FreeIpInventory> tempFreeIpInventories;
                        if (ipRangeVO.getIpVersion() == IPv6Constants.IPv6) {
                            tempFreeIpInventories = IpRangeHelper.getFreeIp(ipRangeVO, 2, "::");
                        } else {
                            tempFreeIpInventories = IpRangeHelper.getFreeIp(ipRangeVO, 2, "0.0.0.0");
                        }
                        freeIpInventories.addAll(tempFreeIpInventories);
                    }

                    if ((isUseForUserVm && freeIpInventories.isEmpty())) {
                        throw new ApiMessageInterceptionException(operr("there are not enough IPs for allocation when attaching the DHCP service to L3 network[uuid:%s].", msg.getL3NetworkUuid()));
                    }
                }
            }
        }
    }

    private Map<String, List<String>> convertNetworkProviderTypeToUuid(Map<String, List<String>> map){
        if (map.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("networkServices cannot be empty"));
        }

        Map<String, List<String>> mapNew = new HashMap<>(map);
        List<NetworkServiceProviderVO> networkServiceProviderVOs = Q.New(NetworkServiceProviderVO.class).list();

        for (NetworkServiceProviderVO vo :networkServiceProviderVOs) {
            if (mapNew.containsKey(vo.getType())){
                mapNew.put(vo.getUuid(), mapNew.remove(vo.getType()));
            }
        }

        return mapNew;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
            APIAttachNetworkServiceToL3NetworkMsg.class,
            APIDetachNetworkServiceFromL3NetworkMsg.class
        );
    }

    @Override
    public GlobalApiMessageInterceptor.InterceptorPosition getPosition() {
        return GlobalApiMessageInterceptor.InterceptorPosition.DEFAULT;
    }

}
