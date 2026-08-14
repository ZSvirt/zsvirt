package org.zstack.header.keyprovider;

import java.util.List;

public interface KeyProviderRekeyAssociationExtensionPoint {
    String getType();

    String getAssociatedResourceType();

    List<String> getAssociatedResourceUuids(List<String> resourceUuids);
}
