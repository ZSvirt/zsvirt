package org.zstack.identity.imports.source;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.identity.imports.header.AbstractAccountSourceSpec;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;

/**
 * Created by Wenhao.Zhang on 2024/05/31
 */
public interface AccountSourceFactory {
    String type();
    AbstractAccountSourceBase createBase(ThirdPartyAccountSourceVO vo);
    void createAccountSource(AbstractAccountSourceSpec spec, ReturnValueCompletion<ThirdPartyAccountSourceVO> completion);
}
