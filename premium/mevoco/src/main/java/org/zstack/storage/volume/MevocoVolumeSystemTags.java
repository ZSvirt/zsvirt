package org.zstack.storage.volume;

import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.volume.VolumeVO;
import org.zstack.tag.PatternedSystemTag;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mingjian.deng on 2017/9/20.
 */
@TagDefinition
public class MevocoVolumeSystemTags {
    public static final String VOLUME_TOTAL_BANDWIDTH_TOKEN = "volumeTotalBandwidth";
    public static PatternedSystemTag VOLUME_TOTAL_BANDWIDTH = new PatternedSystemTag(
            String.format("volumeTotalBandwidth::{%s}", VOLUME_TOTAL_BANDWIDTH_TOKEN),
            DiskOfferingVO.class
    );

    public static final String VOLUME_READ_BANDWIDTH_TOKEN = "volumeReadBandwidth";
    public static PatternedSystemTag VOLUME_READ_BANDWIDTH = new PatternedSystemTag(
            String.format("volumeReadBandwidth::{%s}", VOLUME_READ_BANDWIDTH_TOKEN), DiskOfferingVO.class);

    public static final String VOLUME_WRITE_BANDWIDTH_TOKEN = "volumeWriteBandwidth";
    public static PatternedSystemTag VOLUME_WRITE_BANDWIDTH = new PatternedSystemTag(
            String.format("volumeWriteBandwidth::{%s}", VOLUME_WRITE_BANDWIDTH_TOKEN), DiskOfferingVO.class);

    public static final String VOLUME_TOTAL_IOPS_TOKEN = "volumeTotalIops";
    public static PatternedSystemTag VOLUME_TOTAL_IOPS = new PatternedSystemTag(
            String.format("volumeTotalIops::{%s}", VOLUME_TOTAL_IOPS_TOKEN), DiskOfferingVO.class);

    public static final String VOLUME_READ_IOPS_TOKEN = "volumeReadIops";
    public static PatternedSystemTag VOLUME_READ_IOPS = new PatternedSystemTag(
            String.format("volumeReadIops::{%s}", VOLUME_READ_IOPS_TOKEN), DiskOfferingVO.class);

    public static final String VOLUME_WRITE_IOPS_TOKEN = "volumeWriteIops";
    public static PatternedSystemTag VOLUME_WRITE_IOPS = new PatternedSystemTag(
            String.format("volumeWriteIops::{%s}", VOLUME_WRITE_IOPS_TOKEN), DiskOfferingVO.class);

    public static PatternedSystemTag NORMAL_ACCOUNT_VOLUME_BANDWIDTH_UP_THRESHOLD = new PatternedSystemTag(
            String.format("volumeTotalBandwidth::{%s}::volumeWriteBandwidth::{%s}::volumeReadBandwidth::{%s}",
                    VOLUME_TOTAL_BANDWIDTH_TOKEN, VOLUME_WRITE_BANDWIDTH_TOKEN, VOLUME_READ_BANDWIDTH_TOKEN
            ), VolumeVO.class);

    public static PatternedSystemTag NORMAL_ACCOUNT_VOLUME_IOPS_UP_THRESHOLD = new PatternedSystemTag(
            String.format("volumeTotalIOPS::{%s}::volumeWriteIOPS::{%s}::volumeReadIOPS::{%s}", VOLUME_TOTAL_IOPS_TOKEN,
                    VOLUME_WRITE_IOPS_TOKEN, VOLUME_READ_IOPS_TOKEN
            ), VolumeVO.class);

    public static final Map<String, PatternedSystemTag> iopsSystemTagMap = new HashMap<>();
    public static final Map<String, PatternedSystemTag> volumeSystemTagMap = new HashMap<>();
    static {
        iopsSystemTagMap.put(VOLUME_TOTAL_IOPS_TOKEN, VOLUME_TOTAL_IOPS);
        iopsSystemTagMap.put(VOLUME_READ_IOPS_TOKEN, VOLUME_READ_IOPS);
        iopsSystemTagMap.put(VOLUME_WRITE_IOPS_TOKEN, VOLUME_WRITE_IOPS);

        volumeSystemTagMap.put(VOLUME_TOTAL_BANDWIDTH_TOKEN, VOLUME_TOTAL_BANDWIDTH);
        volumeSystemTagMap.put(VOLUME_READ_BANDWIDTH_TOKEN, VOLUME_READ_BANDWIDTH);
        volumeSystemTagMap.put(VOLUME_WRITE_BANDWIDTH_TOKEN, VOLUME_WRITE_BANDWIDTH);
    }

    public static final String IO_THREAD_PIN_TOKEN = "ioThreadPin";
    public static PatternedSystemTag IO_THREAD_PIN = new PatternedSystemTag(
            String.format("ioThreadPin::{%s}", IO_THREAD_PIN_TOKEN),
            VolumeVO.class
    );
}
