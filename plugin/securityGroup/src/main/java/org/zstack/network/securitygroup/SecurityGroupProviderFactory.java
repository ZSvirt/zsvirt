package org.zstack.network.securitygroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.core.Completion;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.network.*;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkCreateExtensionPoint;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

public class SecurityGroupProviderFactory implements NetworkServiceProviderFactory, L2NetworkCreateExtensionPoint, PrepareDbInitialValueExtensionPoint, ApplyNetworkServiceExtensionPoint {
    private static CLogger logger = Utils.getLogger(SecurityGroupProviderFactory.class);
    
    static NetworkServiceProviderType type = new NetworkServiceProviderType(SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE);
    static NetworkServiceType networkServiceType = new NetworkServiceType(SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE);
    
    private List<String> supportedL2NetworkTypes;
    private NetworkServiceProviderVO provider;

    @Autowired
    private DatabaseFacade dbf;
    
    @Override
    public NetworkServiceProviderType getType() {
        return type;
    }

    @Override
    public void createNetworkServiceProvider(APIAddNetworkServiceProviderMsg msg, NetworkServiceProviderVO vo) {
    }

    @Override
    public NetworkServiceProvider getNetworkServiceProvider(NetworkServiceProviderVO vo) {
        return new SecurityGroupProvider();
    }

    @Override
    public void beforeCreateL2Network(APICreateL2NetworkMsg msg) throws NetworkException {
        
    }

    @Override
    public void afterCreateL2Network(L2NetworkInventory l2Network) {
        if (!supportedL2NetworkTypes.contains(l2Network.getType())) {
            return;
        }
        
        SimpleQuery<NetworkServiceProviderVO> q = dbf.createQuery(NetworkServiceProviderVO.class);
        q.add(NetworkServiceProviderVO_.type, Op.EQ, SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE);
        NetworkServiceProviderVO vo = q.find();
        
        NetworkServiceProviderL2NetworkRefVO ref = new NetworkServiceProviderL2NetworkRefVO();
        ref.setNetworkServiceProviderUuid(vo.getUuid());
        ref.setL2NetworkUuid(l2Network.getUuid());
        dbf.persist(ref);
        String info = String.format("successfully attach network service provider[uuid:%s, name:%s, type:%s] to l2network[uuid:%s, name:%s, type:%s]",
                vo.getUuid(), vo.getName(), vo.getType(), l2Network.getUuid(), l2Network.getName(), l2Network.getType());
        logger.debug(info);
    }

    public List<String> getSupportedL2NetworkTypes() {
        return supportedL2NetworkTypes;
    }

    public void setSupportedL2NetworkTypes(List<String> supportedL2NetworkTypes) {
        this.supportedL2NetworkTypes = supportedL2NetworkTypes;
    }

    @Override
    public void prepareDbInitialValue() {
        SimpleQuery<NetworkServiceProviderVO> query = dbf.createQuery(NetworkServiceProviderVO.class);
        query.add(NetworkServiceProviderVO_.type, Op.EQ, SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE);
        provider = query.find();
        if (provider != null) {
            return;
        }
        
        NetworkServiceProviderVO vo = new NetworkServiceProviderVO();
        vo.setUuid(Platform.getUuid());
        vo.setName(SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE);
        vo.setDescription("zstack security group network service provider");
        vo.getNetworkServiceTypes().add(networkServiceType.toString());
        vo.setType(SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE);
        provider = dbf.persistAndRefresh(vo);
    }
    
    public boolean isL3NetworkNeedSecurityGroupService(L3NetworkInventory l3) {
        return !l3.getNetworkServiceTypesFromProvider(provider.getUuid()).isEmpty();
    }
    
    public boolean isL3NetworkNeedSecurityGroupService(String l3NetworkUuid) {
        L3NetworkVO l3 = dbf.findByUuid(l3NetworkUuid, L3NetworkVO.class);
        L3NetworkInventory l3inv = L3NetworkInventory.valueOf(l3);
        return isL3NetworkNeedSecurityGroupService(l3inv);
    }

    @Override
    public NetworkServiceProviderType getProviderType() {
        return type;
    }

    @Override
    public void applyNetworkService(VmInstanceSpec servedVm, Completion complete) {
        complete.success();
    }

    @Override
    public void releaseNetworkService(VmInstanceSpec servedVm, Completion complete) {
        complete.success();
    }
    
}
