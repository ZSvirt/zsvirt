package org.zstack.zwatch.utils;

import org.zstack.utils.data.SizeUnit;
import org.zstack.zwatch.datatype.UnitCovertRes;

/**
 * Created by Qi Le on 2022/1/14
 */
public class MetricUnitCoversionUtils {
    public static UnitCovertRes convertSizeUnit(double origin, SizeUnit toUnit) {
        UnitCovertRes res = new UnitCovertRes();
        res.setUnit(toUnit.getName());
        if (toUnit == SizeUnit.BYTE) {
            res.setValue(origin);
            return res;
        }
        res.setValue(toUnit.convert(origin, SizeUnit.BYTE));
        return res;
    }

    public static UnitCovertRes convertSizeUnit(double origin) {
        if (origin >= SizeUnit.PETABYTE.getUnitValue()) {
            return convertSizeUnit(origin, SizeUnit.PETABYTE);
        } else if (origin >= SizeUnit.TERABYTE.getUnitValue()) {
            return convertSizeUnit(origin, SizeUnit.TERABYTE);
        } else if (origin >= SizeUnit.GIGABYTE.getUnitValue()) {
            return convertSizeUnit(origin, SizeUnit.GIGABYTE);
        } else if (origin >= SizeUnit.MEGABYTE.getUnitValue()) {
            return convertSizeUnit(origin, SizeUnit.MEGABYTE);
        } else if (origin >= SizeUnit.KILOBYTE.getUnitValue()) {
            return convertSizeUnit(origin, SizeUnit.KILOBYTE);
        } else {
            return convertSizeUnit(origin, SizeUnit.BYTE);
        }
    }
}
