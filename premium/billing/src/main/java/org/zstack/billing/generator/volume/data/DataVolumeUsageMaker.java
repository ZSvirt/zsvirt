package org.zstack.billing.generator.volume.data;

import org.zstack.billing.ResourceCreateUsageExtensionPoint;
import org.zstack.billing.Usage;
import org.zstack.billing.generator.ResourceUsageMaker;
import org.zstack.billing.spendingcalculator.volume.data.DataVolumeCreateUsageExtensionPoint;
import org.zstack.billing.spendingcalculator.volume.data.DataVolumeUsageVO;
import org.zstack.core.db.Q;
import org.zstack.header.volume.*;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yaoning.li on 2020/7/9.
 */
public class DataVolumeUsageMaker implements ResourceUsageMaker {

    @Override
    public Class getResourceVOClass() {
        return VolumeVO.class;
    }

    @Override
    public List<Usage> make(List<String> resourceUuids) {
        List<Usage> result = new ArrayList<>();

        List<VolumeVO> volumeVOS = Q.New(VolumeVO.class)
                .eq(VolumeVO_.type, VolumeType.Data)
                .in(VolumeVO_.uuid, resourceUuids)
                .list();

        ResourceCreateUsageExtensionPoint point = new DataVolumeCreateUsageExtensionPoint();

        for (VolumeVO volumeVO : volumeVOS) {
            if (VolumeStatus.Ready != volumeVO.getStatus() && VolumeStatus.Deleted != volumeVO.getStatus()) {
                continue;
            }

            String accountUuid = findOwnerUuidOfResource(volumeVO.getUuid());

            DataVolumeUsageVO usageVO = new DataVolumeUsageVO();
            usageVO.setAccountUuid(accountUuid);
            usageVO.setDateInLong(System.currentTimeMillis());
            usageVO.setVolumeName(volumeVO.getName());
            usageVO.setVolumeUuid(volumeVO.getUuid());
            usageVO.setInventory(JSONObjectUtil.toJsonString(VolumeInventory.valueOf(volumeVO)));
            usageVO.setVolumeSize(volumeVO.getSize());
            usageVO.setVolumeStatus(VolumeStatus.Ready.toString());

            Usage newUsage = point.makeUsage(usageVO);
            if (newUsage != null) {
                result.add(newUsage);
                continue;
            }

            result.add(usageVO);
        }

        return result;
    }
}
