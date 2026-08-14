package org.zstack.header.network.l3;

import org.zstack.header.configuration.PythonClass;

import java.util.ArrayList;
import java.util.List;

@PythonClass
public interface L3NetworkConstant {
    public static final String SERVICE_ID = "network.l3";

    public static final String ACTION_CATEGORY = "l3Network";
    @PythonClass
    public static final String L3_BASIC_NETWORK_TYPE = "L3BasicNetwork";
    @PythonClass
    public static final String ASC_DELAY_RECYCLE_IP_ALLOCATOR_STRATEGY = "AscDelayRecycleIpAllocatorStrategy";
    @PythonClass
    public static final String FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY = "FirstAvailableIpAllocatorStrategy";
    @PythonClass
    public static final String RANDOM_IP_ALLOCATOR_STRATEGY = "RandomIpAllocatorStrategy";
    @PythonClass
    public static final String FIRST_AVAILABLE_IPV6_ALLOCATOR_STRATEGY = "FirstAvailableIpv6AllocatorStrategy";
    @PythonClass
    public static final String RANDOM_IPV6_ALLOCATOR_STRATEGY = "RandomIpv6AllocatorStrategy";
    @PythonClass
    public static final String STATIC_IP_ALLOCATOR_STRATEGY = "StaticIpAllocatorStrategy";

    public static final String VROUTER_CREATE_EVENT_PATH = "/vrouter/create/event/path";

    public class VRouterData {
        public String vrouterUuid;
    }

    enum Param {
        L3_HOSTROUTE_VO,
        L3_HOSTROUTE_SUCCESS,
    }
}
