package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/5/25.
 */
@AutoQuery(replyClass = APIQueryImageCacheReply.class, inventoryClass = ImageCacheInventory.class)
@RestRequest(
        path = "/primary-storage/imagecache",
        method = HttpMethod.GET,
        responseClass = APIQueryImageCacheReply.class
)
public class APIQueryImageCacheMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }
}
