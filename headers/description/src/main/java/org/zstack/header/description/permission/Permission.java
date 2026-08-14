package org.zstack.header.description.permission;

import org.zstack.header.message.APIMessageDefinition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Permission {
    public final Set<Class<? extends APIMessageDefinition>> adminOnlyAPIs = new HashSet<>();
    public final Set<Class<? extends APIMessageDefinition>> normalAPIs = new HashSet<>();
    public final Set<String> adminOnlyPolicies = new HashSet<>();
    public final Set<String> normalPolicies = new HashSet<>();
    public final List<Class<?>> targetResources = new ArrayList<>();
    public final List<String> requirementList = new ArrayList<>();
    public final List<String> productList = new ArrayList<>();
    public String name;
    public String basePackage;
}
