package org.zstack.header.storage.primary;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/5/25.
 */
@RestResponse(allTo = "inventories")
public class APIQueryImageCacheReply extends APIQueryReply {
    private List<ImageCacheInventory> inventories;

    public List<ImageCacheInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ImageCacheInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryImageCacheReply __example__() {
        APIQueryImageCacheReply reply = new APIQueryImageCacheReply();

        String imageUuid = uuid();
        ImageCacheInventory cache = new ImageCacheInventory();
        cache.setPrimaryStorageUuid(uuid());
        cache.setImageUuid(imageUuid);
        cache.setMediaType("RootVolumeTemplate");
        cache.setSize(2308505600L);
        cache.setState(ImageCacheState.ready.toString());
        cache.setMd5sum("not calculated");
        cache.setInstallUrl(String.format("file:///zstack_ps/imagecache/template/%s/%s.qcow2;hostUuid://%s", imageUuid, imageUuid, uuid()));

        reply.setInventories(Collections.singletonList(cache));
        return reply;
    }
}
