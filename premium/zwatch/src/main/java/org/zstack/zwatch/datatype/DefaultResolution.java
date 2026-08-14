package org.zstack.zwatch.datatype;

import org.zstack.zwatch.ZWatchGlobalConfig;

public class DefaultResolution implements Resolution {
    @Override
    public int resolution(long start, long end) {
        long offset = end - start;
        int scrapeInterval =  ZWatchGlobalConfig.SCRAPE_INTERVAL.value(Integer.class);
        int maxSampleNumber = ZWatchGlobalConfig.MAXIMUM_RESOLUTION_SAMPLES.value(Integer.class);

        long sampleNumber = offset / scrapeInterval;
        if (sampleNumber <= maxSampleNumber) {
            return scrapeInterval;
        }

        return (int)(offset / maxSampleNumber);
    }
}
