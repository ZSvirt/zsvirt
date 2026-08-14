package org.zstack.billing.table;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.billing.BillingConstants;
import org.zstack.billing.BillingSystemTags;
import org.zstack.billing.PriceVO;
import org.zstack.billing.PriceVO_;
import org.zstack.billing.userconfig.BillingUserConfigUtils;
import org.zstack.billing.userconfig.price.ResourcePriceUserConfig;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.tag.SystemTagUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.List;

/**
 * Created by lining on 2019/11/2.
 */
public abstract class VolumePriceExtension extends BasePriceExtension {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public String getCurrentPriceUuid(String priceTableUuid, List<String> systemTags) {
        List<PriceVO> priceVOS = Q.New(PriceVO.class)
                .eq(PriceVO_.tableUuid, priceTableUuid)
                .eq(PriceVO_.resourceName, this.getPriceResourceName())
                .isNull(PriceVO_.endDateInLong)
                .orderBy(PriceVO_.dateInLong, SimpleQuery.Od.DESC)
                .list();

        String priceKeyName = getPriceUserConfigKey(systemTags);

        for (PriceVO priceVO : priceVOS) {
            String key = this.getPriceUserConfigKey(priceVO.getUuid());

            if (priceKeyName == null && key == null) {
                return priceVO.getUuid();
            }

            if (priceKeyName != null && key != null && priceKeyName.equals(key)) {
                return priceVO.getUuid();
            }
        }

        return null;
    }

    private String getPriceUserConfigKey(String priceUuid) {
        String key = null;
        if (BillingSystemTags.PRICE_USER_CONFIG.hasTag(priceUuid)) {
            key = BillingUserConfigUtils.getResourcePriceConfig(priceUuid).getPriceUserConfig().getPriceKeyName();
        }

        return key;
    }

    private String getPriceUserConfigKey(List<String> systemTags) {
        if (systemTags == null || systemTags.isEmpty()) {
            return null;
        }

        String configStr = SystemTagUtils.findTagValue(systemTags, BillingSystemTags.PRICE_USER_CONFIG,  BillingSystemTags.PRICE_USER_CONFIG_TOKEN);
        ResourcePriceUserConfig config = JSONObjectUtil.toObject(configStr, ResourcePriceUserConfig.class);
        return config.getPriceUserConfig().getPriceKeyName();
    }

    @Override
    public String getLastPriceUuid(String targetPriceUuid) {
        PriceVO targetPriceVO = dbf.findByUuid(targetPriceUuid, PriceVO.class);

        List<PriceVO> priceVOS = Q.New(PriceVO.class)
                .eq(PriceVO_.tableUuid, targetPriceVO.getTableUuid())
                .notEq(PriceVO_.uuid, targetPriceVO.getUuid())
                .eq(PriceVO_.resourceName, targetPriceVO.getResourceName())
                .lte(PriceVO_.dateInLong, targetPriceVO.getDateInLong())
                .orderBy(PriceVO_.dateInLong, SimpleQuery.Od.DESC)
                .list();

        String targetPriceKey = this.getPriceUserConfigKey(targetPriceUuid);
        for (PriceVO priceVO : priceVOS) {
            String key = this.getPriceUserConfigKey(priceVO.getUuid());

            if (targetPriceKey == null && key == null) {
                return priceVO.getUuid();
            }

            if (targetPriceKey != null && key != null && targetPriceKey.equals(key)) {
                return priceVO.getUuid();
            }
        }

        return null;
    }
}
