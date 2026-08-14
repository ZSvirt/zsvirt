package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.storage.backup.*;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.distinctByKey;

/**
 * Created by MaJin on 2019/3/21.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStoragePriorityGetterImpl implements PrimaryStoragePriorityGetter {
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    @Transactional(readOnly = true)
    public PrimaryStoragePriority getPrimaryStoragePriority(String imageUuid, String requiredBackupStorageUuid) {
        PrimaryStoragePriority result = new PrimaryStoragePriority();

        ImageInventory image = ImageInventory.valueOf((ImageVO)
                Q.New(ImageVO.class).eq(ImageVO_.uuid, imageUuid).find());

        DebugUtils.Assert(image.getBackupStorageRefs() != null && image.getBackupStorageRefs().size() > 0,
                String.format("imageUuid [%s] not in any BackupStorage", imageUuid));

        List<String> bsUuids = image.getBackupStorageRefs().stream()
                .map(ImageBackupStorageRefInventory::getBackupStorageUuid)
                .collect(Collectors.toList());

        //TODO: we suppose imageUuid is only in 1 bs, if it could be in 2 or more bss, then we should improve the bellow code
        String bsUuid = bsUuids.contains(requiredBackupStorageUuid) ? requiredBackupStorageUuid : bsUuids.get(0);
        BackupStorageInventory bs = BackupStorageInventory.valueOf((BackupStorageVO)
                Q.New(BackupStorageVO.class).eq(BackupStorageVO_.uuid, bsUuid).find());

        List<PriorityMap> priMap = new ArrayList<>();
        for (BackupStoragePrimaryStorageExtensionPoint ext : pluginRgty.getExtensionList(BackupStoragePrimaryStorageExtensionPoint.class)) {
            priMap.addAll(formatPriority(ext.getPrimaryStoragePriorityMap(bs, image)));
        }

        priMap = priMap.stream()
                .sorted(Comparator.comparingInt(it -> it.priority))
                .filter(distinctByKey(it -> it.PS))
                .collect(Collectors.toList());

        result.psPriority = priMap;
        return result;
    }


    @SuppressWarnings("unchecked")
    // priorityStr format is: [{"PS":"Ceph", "priority":"5"},{"PS":"LocalStorage", "priority":"10"}]
    private List<PriorityMap> formatPriority(final String priorityStr) {
        if (priorityStr != null) {
            return JSONObjectUtil.toCollection(priorityStr, ArrayList.class, PriorityMap.class);
        }
        return new ArrayList<>();
    }
}
