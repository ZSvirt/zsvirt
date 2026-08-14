package org.zstack.pluginpremium.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.allocator.AbstractHostAllocatorStrategyFactory;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostAllocatorStrategyType;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.tag.SystemTagUtils;
import org.zstack.tag.TagManager;
import java.util.Arrays;
import java.util.List;
import static org.zstack.core.Platform.argerr;

/**
 * Created by lining on 2018/3/6.
 */
public class MinimumMemoryUsageHostAllocatorStrategyFactory extends AbstractHostAllocatorStrategyFactory implements ManagementNodeReadyExtensionPoint {
	private static final HostAllocatorStrategyType type = new HostAllocatorStrategyType(HostAllocatorConstant.MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_STRATEGY_TYPE);

	@Autowired
	private TagManager tagMgr;

	@Override
	public HostAllocatorStrategyType getHostAllocatorStrategyType() {
	    return type;
    }

	@Override
	public void managementNodeReady() {
		List<String> modes = Arrays.asList(HostAllocatorConstant.HOST_ALLOCATOR_STRATEGY_MODE_HARD, HostAllocatorConstant.HOST_ALLOCATOR_STRATEGY_MODE_SOFT);

		class MinimumMemoryUsageHostAllocatorStrategyModeValidator implements SystemTagCreateMessageValidator, SystemTagValidator {
			@Override
			public void validateSystemTagInCreateMessage(APICreateMessage cmsg) {
				List<String> systemTags = cmsg.getSystemTags();

				if (systemTags == null || systemTags.isEmpty()) {
					return;
				}

				String mode = SystemTagUtils.findTagValue(systemTags, HostAllocatorSystemTags.MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_STRATEGY_MODE, HostAllocatorSystemTags.MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_STRATEGY_MODE_TOKEN);
				if (mode != null && !modes.contains(mode)) {
					throw new ApiMessageInterceptionException(argerr("Incorrect %s settings, valid value is %s", HostAllocatorSystemTags.MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_STRATEGY_MODE_TOKEN, modes));
				}
			}

			@Override
			public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
				if (HostAllocatorSystemTags.MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_STRATEGY_MODE.isMatch(systemTag)) {
					String mode = HostAllocatorSystemTags.MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_STRATEGY_MODE.getTokenByTag(systemTag, HostAllocatorSystemTags.MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_STRATEGY_MODE_TOKEN);

					if (!modes.contains(mode)) {
						throw new ApiMessageInterceptionException(argerr("Incorrect %s settings, valid value is %s", HostAllocatorSystemTags.MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_STRATEGY_MODE_TOKEN, modes));
					}
				}
			}
		}

		MinimumMemoryUsageHostAllocatorStrategyModeValidator validator = new MinimumMemoryUsageHostAllocatorStrategyModeValidator();
		tagMgr.installCreateMessageValidator(InstanceOfferingVO.class.getSimpleName(), validator);
		HostAllocatorSystemTags.MINIMUM_MEMORY_USAGE_HOST_ALLOCATOR_STRATEGY_MODE.installValidator(validator);
	}
}
