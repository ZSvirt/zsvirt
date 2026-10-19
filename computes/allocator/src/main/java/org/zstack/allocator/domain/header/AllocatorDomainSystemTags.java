package org.zstack.allocator.domain.header;

import org.zstack.header.tag.AdminOnlyTag;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.SystemTag;

@TagDefinition
public class AllocatorDomainSystemTags {
    public static final String DOMAIN_UUID_TOKEN = "domainUuid";
    public static final String SCHEDULING_DOMAIN_FORMAT = "schedulingDomain::{" + DOMAIN_UUID_TOKEN + "}";

    @AdminOnlyTag
    public static SystemTag SCHEDULING_DOMAIN = new SystemTag(SCHEDULING_DOMAIN_FORMAT, VmInstanceVO.class);
}
