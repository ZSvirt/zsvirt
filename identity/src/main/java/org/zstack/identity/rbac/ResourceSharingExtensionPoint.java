package org.zstack.identity.rbac;

import org.zstack.identity.header.ShareResourceContext;

public interface ResourceSharingExtensionPoint {
    void beforeSharingResource(ShareResourceContext context);
}
