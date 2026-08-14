package org.zstack.header.volume;

/**
 * Created by Qi Le on 2021/12/20
 */
public interface MevocoVolumeConstants {
    String VOLUME_QOS_MODE_READ = "read";
    String VOLUME_QOS_MODE_WRITE = "write";
    String VOLUME_QOS_MODE_TOTAL = "total";
    String VOLUME_QOS_MODE_ALL = "all";
    String VOLUME_QOS_MODE_OVERWRITE = "overwrite";

    String VOLUME_QOS_TYPE_BANDWIDTH = "bandwidth";
    String VOLUME_QOS_TYPE_IOPS = "iops";

    String DEFAULT_NULL_IOTHREADID = "";
    String DEFAULT_NULL_IOTHREADPIN = "";
    String IOTHREADPIN_SEPARATOR = ":";

    Integer IOTHREADPIN_QUANTITY_LIMIT_PER_VM = 5;

    String IOTHREAD_QEMU_VERSION = "2.4";
    String IOTHREAD_LIBVIRT_VERSION = "1.3.5";

    String VIRTIO_SCSI_VOLUME_DEFAULT_CONTROLLER_INDEX = "0";
}
