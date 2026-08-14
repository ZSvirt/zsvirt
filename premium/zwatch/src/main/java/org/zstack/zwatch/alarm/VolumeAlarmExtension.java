package org.zstack.zwatch.alarm;

import org.zstack.core.db.SQL;
import org.zstack.header.volume.RefreshVolumeSizeExtensionPoint;
import org.zstack.zwatch.namespace.VolumeNamespace;

import java.util.List;

public class VolumeAlarmExtension implements RefreshVolumeSizeExtensionPoint {
    @Override
    public List<String> getNeedRefreshVolumeSizeVolume() {
        return SQL.New("select al.value from AlarmVO a, AlarmLabelVO al" +
                " where a.metricName = :metric" +
                " and al.alarmUuid = a.uuid" +
                " and al.key = :key", String.class)
                .param("metric", VolumeNamespace.VolumeActualSizeInPercent.getName())
                .param("key", VolumeNamespace.LabelNames.VolumeUuid.toString())
                .list();
    }
}
