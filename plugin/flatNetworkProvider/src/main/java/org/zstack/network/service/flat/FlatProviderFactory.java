package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.network.NetworkException;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkCreateExtensionPoint;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.service.*;
import org.zstack.network.service.eip.EipConstant;
import org.zstack.network.service.userdata.UserdataConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by frank on 9/15/2015.
 */
public class FlatProviderFactory implements NetworkServiceProviderFactory, PrepareDbInitialValueExtensionPoint,
        L2NetworkCreateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(FlatProviderFactory.class);

    @Autowired
    private DatabaseFacade dbf;

    private NetworkServiceProviderInventory flatProvider;

    @Override
    public NetworkServiceProviderType getType() {
        return FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE;
    }

    @Override
    public void createNetworkServiceProvider(APIAddNetworkServiceProviderMsg msg, NetworkServiceProviderVO vo) {

    }

    @Override
    public NetworkServiceProvider getNetworkServiceProvider(NetworkServiceProviderVO vo) {
        return new FlatProvider(vo);
    }

    @Override
    public void prepareDbInitialValue() {
        SimpleQuery<NetworkServiceProviderVO> query = dbf.createQuery(NetworkServiceProviderVO.class);
        query.add(NetworkServiceProviderVO_.type, Op.EQ, FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        NetworkServiceProviderVO rpvo = query.find();
        if (rpvo != null) {
            flatProvider = NetworkServiceProviderInventory.valueOf(rpvo);

            // check if any network service type missing, if any, complement them
            SimpleQuery<NetworkServiceTypeVO> q = dbf.createQuery(NetworkServiceTypeVO.class);
            q.add(NetworkServiceTypeVO_.networkServiceProviderUuid, Op.EQ, flatProvider.getUuid());
            List<NetworkServiceTypeVO> refs = q.list();
            Set<String> types = new HashSet<String>();
            for (NetworkServiceTypeVO ref : refs) {
                types.add(ref.getType());
            }

            if (!types.contains(NetworkServiceType.DHCP.toString())) {
                NetworkServiceTypeVO ref = new NetworkServiceTypeVO();
                ref.setNetworkServiceProviderUuid(flatProvider.getUuid());
                ref.setType(NetworkServiceType.DHCP.toString());
                dbf.persist(ref);
            }
            if (!types.contains(UserdataConstant.USERDATA_TYPE_STRING)) {
                NetworkServiceTypeVO ref = new NetworkServiceTypeVO();
                ref.setNetworkServiceProviderUuid(flatProvider.getUuid());
                ref.setType(UserdataConstant.USERDATA_TYPE_STRING);
                dbf.persist(ref);
            }
            if (!types.contains(EipConstant.EIP_NETWORK_SERVICE_TYPE)) {
                NetworkServiceTypeVO ref = new NetworkServiceTypeVO();
                ref.setNetworkServiceProviderUuid(flatProvider.getUuid());
                ref.setType(EipConstant.EIP_NETWORK_SERVICE_TYPE);
                dbf.persist(ref);
            }
            if (!types.contains(NetworkServiceType.DNS.toString())) {
                NetworkServiceTypeVO ref = new NetworkServiceTypeVO();
                ref.setNetworkServiceProviderUuid(flatProvider.getUuid());
                ref.setType(NetworkServiceType.DNS.toString());
                dbf.persist(ref);
            }

            return;
        }

        rpvo = new NetworkServiceProviderVO();
        rpvo.setUuid(Platform.getUuid());
        rpvo.setName("Flat Network Service Provider");
        rpvo.setDescription("Flat Network Service Provider");
        rpvo.getNetworkServiceTypes().add(NetworkServiceType.DHCP.toString());
        rpvo.getNetworkServiceTypes().add(UserdataConstant.USERDATA_TYPE_STRING);
        rpvo.getNetworkServiceTypes().add(EipConstant.EIP_NETWORK_SERVICE_TYPE);
        rpvo.getNetworkServiceTypes().add(NetworkServiceType.DNS.toString());
        rpvo.setType(FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        rpvo = dbf.persistAndRefresh(rpvo);
        flatProvider = NetworkServiceProviderInventory.valueOf(rpvo);
    }

    @Override
    public void beforeCreateL2Network(APICreateL2NetworkMsg msg) throws NetworkException {

    }

    @Override
    public void afterCreateL2Network(L2NetworkInventory l2Network) {
        NetworkServiceProviderL2NetworkRefVO ref = new NetworkServiceProviderL2NetworkRefVO();
        ref.setL2NetworkUuid(l2Network.getUuid());
        ref.setNetworkServiceProviderUuid(flatProvider.getUuid());
        dbf.persist(ref);
        logger.debug(String.format("successfully attach flat network service provider[uuid:%s] to the L2 network[uuid:%s, name:%s]",
                flatProvider.getUuid(), l2Network.getUuid(), l2Network.getName()));
    }
}
