package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.configuration.*;

public class VirtualRouterOfferingFactory implements InstanceOfferingFactory {
	static final InstanceOfferingType type = new InstanceOfferingType(VirtualRouterConstant.VIRTUAL_ROUTER_OFFERING_TYPE);

	@Autowired
	private DatabaseFacade dbf;
	
	public InstanceOfferingType getInstanceOfferingType() {
		return type;
	}

	public InstanceOfferingInventory createInstanceOffering(InstanceOfferingVO vo, APICreateInstanceOfferingMsg msg) {

		return new SQLBatchWithReturn<VirtualRouterOfferingInventory>(){

			@Override
			protected VirtualRouterOfferingInventory scripts() {
				VirtualRouterOfferingVO rvo = new VirtualRouterOfferingVO(vo);
				APICreateVirtualRouterOfferingMsg amsg = (APICreateVirtualRouterOfferingMsg) msg;
				rvo.setManagementNetworkUuid(amsg.getManagementNetworkUuid());
				rvo.setPublicNetworkUuid(amsg.getPublicNetworkUuid());
				rvo.setZoneUuid(amsg.getZoneUuid());
				rvo.setImageUuid(amsg.getImageUuid());
				rvo.setAccountUuid(msg.getSession().getAccountUuid());
				rvo.setDefault(amsg.isDefault() != null ? amsg.isDefault() : false);
				persist(rvo);
				reload(rvo);

				DefaultVirtualRouterOfferingSelector selector = new DefaultVirtualRouterOfferingSelector();
				selector.setOfferingUuid(rvo.getUuid());
				selector.setZoneUuid(rvo.getZoneUuid());
				selector.setPreferToBeDefault(amsg.isDefault());
				selector.setCreated(true);
				selector.selectDefaultOffering();

				return VirtualRouterOfferingInventory.valueOf(reload(rvo));
			}
		}.execute();
	}

    @Override
    public InstanceOffering getInstanceOffering(InstanceOfferingVO vo) {
        VirtualRouterOfferingVO vro = dbf.findByUuid(vo.getUuid(), VirtualRouterOfferingVO.class);
        return new VirtualRouterOffering(vro);
    }
}
