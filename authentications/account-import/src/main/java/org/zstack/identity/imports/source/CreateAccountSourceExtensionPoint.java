package org.zstack.identity.imports.source;

import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;

public interface CreateAccountSourceExtensionPoint {
    void afterCreatingAccountSource(ThirdPartyAccountSourceVO accountSource);
}
