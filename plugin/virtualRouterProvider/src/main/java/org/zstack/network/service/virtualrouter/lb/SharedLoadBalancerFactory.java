package org.zstack.network.service.virtualrouter.lb;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.service.lb.*;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;

import javax.persistence.TypedQuery;
import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SharedLoadBalancerFactory implements LoadBalancerFactory {
    @Autowired
    DatabaseFacade dbf;
    @Autowired
    LoadBalancerManager lbMgr;

    @Override
    public String getType() {
        return LoadBalancerType.Shared.toString();
    }

    @Override
    public LoadBalancerVO persistLoadBalancer(APICreateLoadBalancerMsg msg) {
        LoadBalancerVO vo = new LoadBalancerVO();
        vo.setName(msg.getName());
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setDescription(msg.getDescription());
        vo.setVipUuid(msg.getVipUuid());
        vo.setState(LoadBalancerState.Enabled);
        vo.setType(LoadBalancerType.Shared);
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo = dbf.persistAndRefresh(vo);
        return vo;
    }

    @Override
    public void deleteLoadBalancer(LoadBalancerVO vo) {
        dbf.removeByPrimaryKey(vo.getUuid(), LoadBalancerVO.class);
    }

    @Override
    public String getNetworkServiceType() {
        return LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING;
    }

    @Override
    public LoadBalancerBackend getLoadBalancerBackend(LoadBalancerVO vo) {
        if (vo.getProviderType() == null) {
            return null;
        }

        return lbMgr.getBackend(vo.getProviderType());
    }

    @Override
    public String getProviderTypeByVmNicUuid(String nicUuid) {
        if (nicUuid == null) {
            return null;
        }

        String sql = "select l3 from L3NetworkVO l3, VmNicVO nic where nic.l3NetworkUuid = l3.uuid and nic.uuid = :uuid";
        TypedQuery<L3NetworkVO> q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
        q.setParameter("uuid", nicUuid);
        L3NetworkVO l3 = q.getSingleResult();
        for (NetworkServiceL3NetworkRefVO ref : l3.getNetworkServices()) {
            if (LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING.equals(ref.getNetworkServiceType())) {
                sql = "select p.type from NetworkServiceProviderVO p where p.uuid = :uuid";
                TypedQuery<String> nq = dbf.getEntityManager().createQuery(sql, String.class);
                nq.setParameter("uuid", ref.getNetworkServiceProviderUuid());
                return nq.getSingleResult();
            }
        }

        return null;
    }

    @Override
    public List<VmNicVO> getAttachableVmNicsForServerGroup(LoadBalancerVO lbVO, LoadBalancerServerGroupVO groupVO) {
        String providerType = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE;
        if (lbVO.getProviderType() != null) {
            providerType = lbVO.getProviderType();
        }

        LoadBalancerBackend backend = lbMgr.getBackend(providerType);
        return backend.getAttachableVmNicsForServerGroup(lbVO, groupVO);
    }

    @Override
    public String getApplianceVmType() {
        return VyosConstants.VYOS_VM_TYPE;
    }
}
