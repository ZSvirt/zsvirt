package org.zstack.billing;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2016/5/20.
 */
public interface UnitPrice {
    double getUnitPrice(PriceVO co);

    static double cpuUnitPrice(PriceVO co) {
        return co.getPrice() / TimeUnit.valueOf(co.getTimeUnit()).toSeconds(1);
    }

    static double memoryUnitPrice(PriceVO co) {
        double d = (SizeUnit.valueOf(co.getResourceUnit()).toByte(1) * TimeUnit.valueOf(co.getTimeUnit()).toSeconds(1));
        if (d == 0) {
            throw new CloudRuntimeException(String.format("invalid price %s causing infinity unit price", JSONObjectUtil.toJsonString(co)));
        }

        return co.getPrice() / d;
    }

    static double volumeUnitPrice(PriceVO co) {
        double d = (SizeUnit.valueOf(co.getResourceUnit()).toByte(1) * TimeUnit.valueOf(co.getTimeUnit()).toSeconds(1));
        if (d == 0) {
            throw new CloudRuntimeException(String.format("invalid price %s causing infinity unit price", JSONObjectUtil.toJsonString(co)));
        }

        return co.getPrice() / d;
    }

    static double getUnitPriceByPriceType(PriceVO co) {
        //TODO: remove hard code if .. else
        if (BillingConstants.SPENDING_CPU.equals(co.getResourceName())) {
            return cpuUnitPrice(co);
        } else if (BillingConstants.SPENDING_MEMORY.equals(co.getResourceName())) {
            return memoryUnitPrice(co);
        } else if (BillingConstants.SPENDING_DATA_VOLUME.equals(co.getResourceName()) || BillingConstants.SPENDING_ROOT_VOLUME.equals(co.getResourceName())) {
            return volumeUnitPrice(co);
        } else {
            throw new CloudRuntimeException("should not be here");
        }
    }
}
