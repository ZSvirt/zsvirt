package org.zstack.allocator.domain.header;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AllocatorDomainConstant {
    private AllocatorDomainConstant() {
    }

    public static final String SERVICE_ID = "allocator.domain";

    public static final String ACTION_CATEGORY = "allocator.domain";

    public static final List<AllocatorDomainScene> DEFAULT_SCENES = Collections.unmodifiableList(Arrays.asList(
            AllocatorDomainScene.NewCreate,
            AllocatorDomainScene.Start,
            AllocatorDomainScene.Migrate,
            AllocatorDomainScene.Ha,
            AllocatorDomainScene.HostMaintenance,
            AllocatorDomainScene.Drs
    ));
}
