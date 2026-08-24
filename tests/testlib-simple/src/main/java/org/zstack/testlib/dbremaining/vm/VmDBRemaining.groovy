package org.zstack.testlib.dbremaining.vm

import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.vm.VmPriorityConfigVO
import org.zstack.testlib.dbremaining.AllowedDBRemaining

class VmDBRemaining extends AllowedDBRemaining {
    @Override
    void remaining() {
        table {
            tableVOClass = VmPriorityConfigVO.class
            noLimitRows = true
        }

        table {
            tableVOClass = AccountResourceRefVO.class
            checker = { List<AccountResourceRefVO> vos ->
                return vos.findAll { it.resourceType != VmPriorityConfigVO.class.simpleName }
            }
        }
    }
}
