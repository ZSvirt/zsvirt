package org.zstack.billing;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2016/5/20.
 */
public class VolumeUnitPrice implements UnitPrice {
    @Override
    public double getUnitPrice(PriceVO co) {
        double d = (SizeUnit.valueOf(co.getResourceUnit()).toMegaByte(1) * TimeUnit.valueOf(co.getTimeUnit()).toSeconds(1));
        if (d == 0) {
            throw new CloudRuntimeException(String.format("invalid price %s causing infinity unit price", JSONObjectUtil.toJsonString(co)));
        }

        return co.getPrice() / d;
    }
}
