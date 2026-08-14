package org.zstack.billing.table;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.billing.PriceVO;
import org.zstack.billing.PriceVO_;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;

import java.util.List;

/**
 * Created by lining on 2019/11/2.
 */
public abstract class BasePriceExtension implements PriceExtension {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public String getCurrentPriceUuid(String priceTableUuid, List<String> systemTags) {
        PriceVO lastPriceVO = Q.New(PriceVO.class)
                .eq(PriceVO_.tableUuid, priceTableUuid)
                .eq(PriceVO_.resourceName, this.getPriceResourceName())
                .isNull(PriceVO_.endDateInLong)
                .orderBy(PriceVO_.dateInLong, SimpleQuery.Od.DESC)
                .limit(1)
                .find();
        return lastPriceVO != null ? lastPriceVO.getUuid() : null;
    }

    @Override
    public String getLastPriceUuid(String targetPriceUuid) {
        PriceVO targetPriceVO = dbf.findByUuid(targetPriceUuid, PriceVO.class);

        PriceVO lastPriceVO = Q.New(PriceVO.class)
                .eq(PriceVO_.tableUuid, targetPriceVO.getTableUuid())
                .notEq(PriceVO_.uuid, targetPriceVO.getUuid())
                .eq(PriceVO_.resourceName, targetPriceVO.getResourceName())
                .lte(PriceVO_.dateInLong, targetPriceVO.getDateInLong())
                .orderBy(PriceVO_.dateInLong, SimpleQuery.Od.DESC)
                .limit(1)
                .find();
        return lastPriceVO != null ? lastPriceVO.getUuid() : null;
    }
}
