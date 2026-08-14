package org.zstack.pluginpremium.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.allocator.AbstractHostAllocatorStrategyFactory;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostAllocatorStrategyType;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.configuration.APICreateInstanceOfferingMsg;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.host.HostAllocateExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.tag.SystemTagUtils;
import org.zstack.tag.TagManager;
import java.util.List;
import static org.zstack.core.Platform.argerr;

/**
 * Created by lining on 2018/3/6.
 */
public class MaxInstancePerHostHostAllocatorStrategyFactory extends AbstractHostAllocatorStrategyFactory implements ManagementNodeReadyExtensionPoint, HostAllocateExtensionPoint {
	private static final HostAllocatorStrategyType type = new HostAllocatorStrategyType(HostAllocatorConstant.MAX_INSTANCE_PER_HOST_HOST_ALLOCATOR_STRATEGY_TYPE);

	@Autowired
	private TagManager tagMgr;

	@Override
	public HostAllocatorStrategyType getHostAllocatorStrategyType() {
	    return type;
    }

	@Override
	public void managementNodeReady() {

		class MaxInstancePerHostValidator implements SystemTagCreateMessageValidator, SystemTagValidator {
			@Override
			public void validateSystemTagInCreateMessage(APICreateMessage cmsg) {
				final APICreateInstanceOfferingMsg msg = (APICreateInstanceOfferingMsg) cmsg;

				List<String> systemTags = cmsg.getSystemTags();
				String allocatorStrategy = msg.getAllocatorStrategy();
				if (!HostAllocatorConstant.MAX_INSTANCE_PER_HOST_HOST_ALLOCATOR_STRATEGY_TYPE.equals(allocatorStrategy)) {
					return;
				}

				if (systemTags == null || systemTags.isEmpty()) {
					throw new ApiMessageInterceptionException(argerr("Select %s strategy, you must set %s", HostAllocatorConstant.MAX_INSTANCE_PER_HOST_HOST_ALLOCATOR_STRATEGY_TYPE, HostAllocatorSystemTags.MAX_INSTANCE_PER_HOST_TOKEN));
				}

				String maxInstancePerHost = SystemTagUtils.findTagValue(systemTags, HostAllocatorSystemTags.MAX_INSTANCE_PER_HOST, HostAllocatorSystemTags.MAX_INSTANCE_PER_HOST_TOKEN);
				if (maxInstancePerHost == null) {
					throw new ApiMessageInterceptionException(argerr("Select %s strategy, you must set %s", HostAllocatorConstant.MAX_INSTANCE_PER_HOST_HOST_ALLOCATOR_STRATEGY_TYPE, HostAllocatorSystemTags.MAX_INSTANCE_PER_HOST_TOKEN));
				}

				try {
					Integer.parseInt(maxInstancePerHost);
				} catch (Exception e) {
					throw new ApiMessageInterceptionException(argerr("%s must be a number", HostAllocatorSystemTags.MAX_INSTANCE_PER_HOST_TOKEN));
				}
			}

			@Override
			public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
				if (HostAllocatorSystemTags.MAX_INSTANCE_PER_HOST.isMatch(systemTag)) {
					String maxInstancePerHost = HostAllocatorSystemTags.MAX_INSTANCE_PER_HOST.getTokenByTag(systemTag, HostAllocatorSystemTags.MAX_INSTANCE_PER_HOST_TOKEN);

					try {
						Integer.parseInt(maxInstancePerHost);
					} catch (Exception e) {
						throw new ApiMessageInterceptionException(argerr("%s must be a number", HostAllocatorSystemTags.MAX_INSTANCE_PER_HOST_TOKEN));
					}
				}
			}
		}

		MaxInstancePerHostValidator maxInstancePerHostValidator = new MaxInstancePerHostValidator();
		tagMgr.installCreateMessageValidator(InstanceOfferingVO.class.getSimpleName(), maxInstancePerHostValidator);
		HostAllocatorSystemTags.MAX_INSTANCE_PER_HOST.installValidator(maxInstancePerHostValidator);
	}

	@Override
	public void beforeAllocateHostSuccessReply(HostAllocatorSpec spec, String replyHostUuid) {
		if (!HostAllocatorConstant.MAX_INSTANCE_PER_HOST_HOST_ALLOCATOR_STRATEGY_TYPE.equals(spec.getAllocatorStrategy())) {
			return;
		}

		String vmInstanceUuid = spec.getVmInstance().getUuid();
		new SQLBatch() {
			@Override
			protected void scripts() {
				sql(VmInstanceVO.class)
						.eq(VmInstanceVO_.uuid, vmInstanceUuid)
						.set(VmInstanceVO_.hostUuid, replyHostUuid)
						.update();
			}
		}.execute();
	}
}
