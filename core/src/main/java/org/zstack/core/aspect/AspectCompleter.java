package org.zstack.core.aspect;

import org.zstack.core.db.Q;
import org.zstack.header.aspect.OwnedByAccountAspect;
import org.zstack.header.core.StaticInit;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;

import java.util.function.Function;

public class AspectCompleter {
    @StaticInit
    static void staticinit() {
        OwnedByAccountAspect.setAccountUuidGetter(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return Q.New(AccountResourceRefVO.class)
                        .select(AccountResourceRefVO_.accountUuid)
                        .eq(AccountResourceRefVO_.resourceUuid, s)
                        .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                        .findValue();
            }
        });
    }
}
