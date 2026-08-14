package org.zstack.compute.vm.metadata.resolver;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.metadata.VmUuidFromApiResolver;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Resolves a SystemTagVO UUID to a VM UUID for {@code @MetadataImpact} processing.
 *
 * <p>Used exclusively by {@code APIUpdateSystemTagMsg} and {@code APIDeleteTagMsg},
 * where {@code field="uuid"} refers to the SystemTagVO's own UUID (not a resource UUID).
 *
 * <p>Resolution: tag UUID → {@code SystemTagVO.resourceUuid} → delegate to
 * {@link ResourceUuidToVmUuidResolver} (VM / Volume / Nic lookup).</p>
 */
public class SystemTagUuidToVmUuidResolver implements VmUuidFromApiResolver {
    private static final CLogger logger = Utils.getLogger(SystemTagUuidToVmUuidResolver.class);

    private ResourceUuidToVmUuidResolver resourceResolver;

    private ResourceUuidToVmUuidResolver getResourceResolver() {
        if (resourceResolver == null) {
            resourceResolver = Platform.getComponentLoader().getComponent(ResourceUuidToVmUuidResolver.class);
        }
        return resourceResolver;
    }

    @Override
    public String resolveVmUuid(String fieldValue) {
        if (fieldValue == null) {
            return null;
        }

        String resourceUuid = Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.uuid, fieldValue)
                .select(SystemTagVO_.resourceUuid)
                .findValue();
        if (resourceUuid == null) {
            logger.warn(String.format("[SystemTagUuidToVmUuidResolver] SystemTagVO[uuid:%s] not found", fieldValue));
            return null;
        }

        return getResourceResolver().resolveVmUuid(resourceUuid);
    }
}
