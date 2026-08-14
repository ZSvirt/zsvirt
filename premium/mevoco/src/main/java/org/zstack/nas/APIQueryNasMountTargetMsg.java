package org.zstack.nas;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 2018/3/9.
 */
@AutoQuery(replyClass = APIQueryNasMountTargetReply.class, inventoryClass = NasMountTargetInventory.class)
@RestRequest(
        path = "/primary-storage/nas/mount",
        optionalPaths = {"/primary-storage/nas/mount/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryNasMountTargetReply.class
)
public class APIQueryNasMountTargetMsg extends APIQueryMessage {

    public  static List<String> __example__() {
        return asList("name:mount1");
    }
}
