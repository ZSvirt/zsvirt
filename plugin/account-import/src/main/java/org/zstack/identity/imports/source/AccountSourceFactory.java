package org.zstack.identity.imports.source;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.identity.imports.header.AbstractAccountSourceSpec;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;

import java.util.Set;

/**
 * Created by Wenhao.Zhang on 2024/05/31
 */
public interface AccountSourceFactory {
    /**
     * Account source types handled by this factory.
     *
     * <p>Each value should match a {@link org.zstack.header.identity.AccountSource} name and be
     * persisted in {@link ThirdPartyAccountSourceVO#getType()}. A factory may register more than
     * one type when a single backend serves multiple sources (e.g. LDAP serves both OpenLdap and
     * WindowsAD).
     */
    Set<String> supportedTypes();
    AbstractAccountSourceBase createBase(ThirdPartyAccountSourceVO vo);
    void createAccountSource(AbstractAccountSourceSpec spec, ReturnValueCompletion<ThirdPartyAccountSourceVO> completion);
}
