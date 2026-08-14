package org.zstack.billing;

import org.zstack.utils.data.SizeUnit;

/**
 * Created by xing5 on 2016/9/18.
 */
public interface UsageNormalizer {
    double normalizeUsage(double usage, String resourceUnit);

    static double normalizeCpuUsage(double usage, String resourceUnit) {
        return usage;
    }

    static double normalizeMemoryUsage(Double usage, String resourceUnit) {
        return SizeUnit.valueOf(resourceUnit).convert(usage, SizeUnit.BYTE);
    }

    static double normalizeVolumeUsage(Double usage, String resourceUnit) {
        return SizeUnit.valueOf(resourceUnit).convert(usage, SizeUnit.BYTE);
    }

    static double normalizeSnapShotUsage(Double usage, String resourceUnit) {
        return SizeUnit.valueOf(resourceUnit).convert(usage, SizeUnit.BYTE);
    }

    static double normalizeBandwidthUsage(Double usage, String resourceUnit) {
        return SizeUnit.valueOf(resourceUnit).convert(usage, SizeUnit.BYTE);
    }

    static double normalizeGpuUsage(double usage, String resourceUnit) {
        return usage;
    }

    static double normalizeBareMetalUsage(double usage, String resourceUnit) {
        return usage;
    }
}
