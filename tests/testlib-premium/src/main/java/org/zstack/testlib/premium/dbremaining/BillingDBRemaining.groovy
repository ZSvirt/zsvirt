package org.zstack.testlib.premium.dbremaining

import org.zstack.billing.BillingConstants
import org.zstack.billing.table.PriceTableVO
import org.zstack.testlib.dbremaining.AllowedDBRemaining

class BillingDBRemaining extends AllowedDBRemaining {
    @Override
    void remaining() {
        table {
            tableVOClass = PriceTableVO.class
            checker = { List<PriceTableVO> vos ->
                return vos.findAll { it.uuid != BillingConstants.GLOBAL_DEFAULT_PRICE_TABLE_UUID }
            }
        }
    }
}
