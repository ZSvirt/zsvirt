package org.zstack.billing.generator;

import org.zstack.billing.Usage;
import org.zstack.identity.Account;

import java.util.List;

/**
 * Created by yaoning.li on 2020/7/9.
 */
public interface ResourceUsageMaker {
    List<Usage> make(List<String> resourceUuids);

    Class getResourceVOClass();

    default String findOwnerUuidOfResource(String resourceUuid) {
        return Account.getAccountUuidOfResource(resourceUuid);
    }
}
