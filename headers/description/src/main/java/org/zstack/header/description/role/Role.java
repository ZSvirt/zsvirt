package org.zstack.header.description.role;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Role {
    public String uuid;
    public String name;
    public final Set<String> allowedActions = new HashSet<>();
    public final List<String> excludedActions = new ArrayList<>();
}
