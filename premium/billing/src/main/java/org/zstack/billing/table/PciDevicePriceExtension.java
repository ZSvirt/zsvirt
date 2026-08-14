package org.zstack.billing.table;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.billing.BillingConstants;
import org.zstack.billing.BillingSystemTags;
import org.zstack.billing.PriceVO;
import org.zstack.billing.PriceVO_;
import org.zstack.billing.spendingcalculator.pcidevice.PricePciDeviceOfferingRefVO;
import org.zstack.billing.spendingcalculator.pcidevice.PricePciDeviceOfferingRefVO_;
import org.zstack.billing.userconfig.BillingUserConfigUtils;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.tag.SystemTagUtils;

import java.util.List;

import static org.zstack.billing.BillingSystemTags.PRICE_GPU_OFFERING_UUID;
import static org.zstack.billing.BillingSystemTags.PRICE_GPU_OFFERING_UUID_TOKEN;

/**
 * Created by lining on 2019/11/2.
 */
public class PciDevicePriceExtension extends BasePriceExtension {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public String getPriceResourceName() {
        return BillingConstants.SPENDING_TYPE_PCI_DEVICE;
    }

    @Override
    public String getCurrentPriceUuid(String priceTableUuid, List<String> systemTags) {
        List<PriceVO> priceVOS = Q.New(PriceVO.class)
                .eq(PriceVO_.tableUuid, priceTableUuid)
                .eq(PriceVO_.resourceName, this.getPriceResourceName())
                .isNull(PriceVO_.endDateInLong)
                .orderBy(PriceVO_.dateInLong, SimpleQuery.Od.DESC)
                .list();

        String pciDeviceOfferingUuid = getPciDeviceOfferingUuid(systemTags);

        for (PriceVO priceVO : priceVOS) {
            String offeringUuid = this.getPciDeviceOfferingUuid(priceVO.getUuid());

            if (pciDeviceOfferingUuid == null && offeringUuid == null) {
                return priceVO.getUuid();
            }

            if (pciDeviceOfferingUuid != null && offeringUuid != null && pciDeviceOfferingUuid.equals(offeringUuid)) {
                return priceVO.getUuid();
            }
        }

        return null;
    }

    private String getPciDeviceOfferingUuid(String priceUuid) {
        String pciDeviceOfferingUuid = Q.New(PricePciDeviceOfferingRefVO.class)
                .select(PricePciDeviceOfferingRefVO_.pciDeviceOfferingUuid)
                .eq(PricePciDeviceOfferingRefVO_.priceUuid, priceUuid)
                .findValue();
        return pciDeviceOfferingUuid;
    }

    private String getPciDeviceOfferingUuid(List<String> systemTags) {
        if (systemTags == null || systemTags.isEmpty()) {
            return null;
        }

        String pciDeviceOfferingUuid = SystemTagUtils.findTagValue(systemTags, PRICE_GPU_OFFERING_UUID, PRICE_GPU_OFFERING_UUID_TOKEN);
        return pciDeviceOfferingUuid;
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

        String targetOfferingUuid = this.getPciDeviceOfferingUuid(targetPriceVO.getUuid());

        for (PriceVO priceVO : priceVOS) {
            String offeringUuid = this.getPciDeviceOfferingUuid(priceVO.getUuid());

            if (targetOfferingUuid == null && offeringUuid == null) {
                return priceVO.getUuid();
            }

            if (targetOfferingUuid != null && offeringUuid != null && targetOfferingUuid.equals(offeringUuid)) {
                return priceVO.getUuid();
            }
        }

        return null;
    }
}
