package org.zstack.compute.vm.metadata.resolver;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.vm.metadata.VmUuidFromApiResolver;

/**
 * The field value is already a vmInstanceUuid — return it directly.
 *
 * <p>Used for APIs whose {@code @MetadataImpact(field = "vmInstanceUuid")} or
 * similar field (e.g. {@code "templatedVmInstanceUuid"}) directly contains the
 * target VM UUID.</p>
 */
public class VmUuidDirectResolver implements VmUuidFromApiResolver {

    @Override
    public String resolveVmUuid(String fieldValue) {
        if (StringUtils.isEmpty(fieldValue)) {
            return null;
        }
        return fieldValue;
    }
}
