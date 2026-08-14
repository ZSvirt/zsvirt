package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkDnsVO;
import org.zstack.header.network.l3.L3NetworkDnsVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.network.service.virtualrouter.VirtualRouterKvmBackendCommands.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.core.Platform.operr;

public class VirtualRouterKvmBackend implements VirtualRouterHypervisorBackend {
	private static final CLogger logger = Utils.getLogger(VirtualRouterKvmBackend.class);

	@Autowired
	private DatabaseFacade dbf;
	@Autowired
	private CloudBus bus;

	@Override
	public HypervisorType getVirtualRouterSupportedHypervisorType() {
		return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
	}

	private List<String> getDns(String l3NetworkUuid) {
		SimpleQuery<L3NetworkDnsVO> q = dbf.createQuery(L3NetworkDnsVO.class);
		q.select(L3NetworkDnsVO_.dns);
		q.add(L3NetworkDnsVO_.l3NetworkUuid, Op.EQ, l3NetworkUuid);
		return q.listValue();
	}
	
	@Override
	public void createVirtualRouterBootstrapIso(final VirtualRouterBootstrapIsoInventory iso, final VmInstanceSpec vrSpec, final Completion complete) {
		CreateVritualRouterBootstrapIsoCmd cmd = new CreateVritualRouterBootstrapIsoCmd();

		VmNicInventory mgmtNic = null;
		VmNicInventory publicNic = null;
		for (VmNicInventory nic : vrSpec.getDestNics()) {
			if (VirtualRouterNicMetaData.isManagementNic(nic)) {
				mgmtNic = nic;
			}
			if (VirtualRouterNicMetaData.isPublicNic(nic)) {
				publicNic = nic;
			}
		}

		List<String> dns = getDns(mgmtNic.getL3NetworkUuid());
		
		BootstrapIsoInfo info = new BootstrapIsoInfo();
		info.setManagementNicGateway(mgmtNic.getGateway());
		info.setManagementNicIp(mgmtNic.getIp());
		info.setManagementNicMac(mgmtNic.getMac());
		info.setManagementNicNetmask(mgmtNic.getNetmask());
		if (publicNic != null) {
			info.setPublicNicIp(publicNic.getIp());
			info.setPublicNicNetmask(publicNic.getNetmask());
			info.setPublicNicMac(publicNic.getMac());
			info.setPublicNicGateway(publicNic.getGateway());
			dns.addAll(getDns(publicNic.getL3NetworkUuid()));
		}
		
		if (dns.isEmpty()) {
			dns.add("8.8.8.8");
		}
		info.setDns(dns);
		
		cmd.setIsoInfo(info);
		cmd.setIsoPath(iso.getIsoPath());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(VirtualRouterConstant.VR_KVM_CREATE_BOOTSTRAP_ISO_PATH);
        msg.setHostUuid(vrSpec.getDestHost().getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, vrSpec.getDestHost().getUuid());
        bus.send(msg, new CloudBusCallBack(complete) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    complete.fail(reply.getError());
                    return;
                }

                CreateVritualRouterBootstrapIsoRsp rsp = ((KVMHostAsyncHttpCallReply)reply).toResponse(CreateVritualRouterBootstrapIsoRsp.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to create VirtualRouterBootstrapIso[%s] on kvm host[uuid:%s, ip:%s] for virtual router[uuid:%s], because %s",
                            iso.getIsoPath(), vrSpec.getDestHost().getUuid(), vrSpec.getDestHost().getManagementIp(), iso.getVirtualRouterUuid(),
                            rsp.getError());
                    complete.fail(err);
                    return;
                }

                complete.success();
            }
        });
	}

	@Override
	public void deleteVirtualRouterBootstrapIso(final VirtualRouterBootstrapIsoInventory iso, final VmInstanceInventory vrSpec, final Completion complete) {
		DeleteVirtualRouterBootstrapIsoCmd cmd = new DeleteVirtualRouterBootstrapIsoCmd();
		cmd.setIsoPath(iso.getIsoPath());

		final String hostUuid = vrSpec.getHostUuid() == null ? vrSpec.getLastHostUuid() : vrSpec.getHostUuid();
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(VirtualRouterConstant.VR_KVM_DELETE_BOOTSTRAP_ISO_PATH);
        msg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(complete) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    complete.fail(reply.getError());
                    return;
                }

                DeleteVirtualRouterBootstrapIsoRsp rsp = ((KVMHostAsyncHttpCallReply)reply).toResponse(DeleteVirtualRouterBootstrapIsoRsp.class);
                if (!rsp.isSuccess()) {
					ErrorCode err = operr("failed to delete VirtualRouterBootstrapIso[%s] on kvm host[uuid:%s] for virtual router[uuid:%s], because %s",
							iso.getIsoPath(), hostUuid, iso.getVirtualRouterUuid(),
							rsp.getError());
					complete.fail(err);
                    return;
                }

                complete.success();
            }
        });
	}
}
