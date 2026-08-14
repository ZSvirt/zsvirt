package org.zstack.header.search;

import java.util.List;


public interface SearchIndexRecreateExtensionPoint {
    List<String> returnUuidToReindex(String inventoryName);
}
