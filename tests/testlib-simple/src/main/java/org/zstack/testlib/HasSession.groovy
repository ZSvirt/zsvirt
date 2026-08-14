package org.zstack.testlib

import org.zstack.testlib.identity.AccountSpec

/**
 * Created by xing5 on 2017/2/15.
 */
trait HasSession {
    Closure session

    String accountName

    @SpecMethod
    void useAccount(String name) {
        accountName = name

        if (this instanceof Spec) {
            preCreate {
                addDependency(name, AccountSpec.class)
            }
        }
    }
}