package org.zstack.header.query;

import java.util.List;

/**
 */
public interface AddExpandedQueryExtensionPoint {
    List<ExpandedQueryStruct> getExpandedQueryStructs();

    List<ExpandedQueryAliasStruct> getExpandedQueryAliasesStructs();
}
