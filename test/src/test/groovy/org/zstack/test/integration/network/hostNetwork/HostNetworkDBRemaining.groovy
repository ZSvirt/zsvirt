package org.zstack.test.integration.network.hostNetwork

import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpVO
import org.zstack.testlib.AllowedDBRemaining

class HostNetworkDBRemaining extends AllowedDBRemaining {
    @Override
    void remaining() {
        table {
            tableVOClass = HostNetworkInterfaceLldpVO.class
            noLimitRows = true
        }

        table {
            tableVOClass = AccountResourceRefVO.class
            checker = { List<AccountResourceRefVO> vos ->
                return vos.findAll { it.resourceType != HostNetworkInterfaceLldpVO.class.simpleName }
            }
        }
    }
}
