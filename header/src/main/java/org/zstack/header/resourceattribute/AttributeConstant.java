package org.zstack.header.resourceattribute;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AttributeConstant {
    public static final String SERVICE_ID = "resourceAttribute";

    public static final String CONSTRAINTS_OPTION = "option";
    public static final String CONSTRAINTS_ENUM = "enum";
    public static final Set<String> VALID_CONSTRAINTS_OPTIONS =
            new HashSet<>(Arrays.asList(CONSTRAINTS_OPTION, CONSTRAINTS_ENUM));
}
