package org.zstack.billing.table;

import org.zstack.tag.SystemTag;

import java.util.List;

/**
 * Created by lining on 2019/11/2.
 */
public interface PriceExtension {
    String getPriceResourceName();

    String getCurrentPriceUuid (String priceTableUuid, List<String> systemTags);

    String getLastPriceUuid(String priceUuid);
}
