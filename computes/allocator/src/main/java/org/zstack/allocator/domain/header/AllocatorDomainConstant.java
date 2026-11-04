package org.zstack.allocator.domain.header;

import org.zstack.header.allocator.AllocatorDomainScene;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AllocatorDomainConstant {
    private AllocatorDomainConstant() {
    }

    public static final String SERVICE_ID = "allocator.domain";

    public static final String ACTION_CATEGORY = "allocator.domain";

    public static final String PORT_GROUP_RESOURCE_TYPE = "PortGroupVO";

    public static final String UNBOUND_DOMAIN_UUID = "None";

    public static final List<AllocatorDomainScene> DEFAULT_SCENES = Collections.unmodifiableList(Arrays.asList(
            AllocatorDomainScene.NewCreate,
            AllocatorDomainScene.Start,
            AllocatorDomainScene.Migrate,
            AllocatorDomainScene.Ha,
            AllocatorDomainScene.HostMaintenance,
            AllocatorDomainScene.Drs
    ));

    public static final class Opaque {
        private Opaque() {
        }

        public static final String HOST_UUID = "host.uuid";
        public static final String CLUSTER_UUID = "cluster.uuid";
        public static final String ZONE_UUID = "zone.uuid";
        public static final String PRIMARY_STORAGE_UUID = "primary.storage.uuid";
        public static final String L3_UUID = "l3.uuid";
        public static final String RESOURCE_UUID = "resource.uuid";
        public static final String RESOURCE_TYPE = "resource.type";
        public static final String RESOURCE_QUALIFIER = "resource.qualifier";
        public static final String EFFECT = "effect";
        public static final String DOMAIN_UUID = "domain.uuid";
    }
}
