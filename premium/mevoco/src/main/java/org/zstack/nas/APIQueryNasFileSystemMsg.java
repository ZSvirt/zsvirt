package org.zstack.nas;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 2018/3/5.
 */
@AutoQuery(replyClass = APIQueryNasFileSystemReply.class, inventoryClass = NasFileSystemInventory.class)
@RestRequest(
        path = "/primary-storage/nas",
        optionalPaths = {"/primary-storage/nas/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryNasFileSystemReply.class
)
public class APIQueryNasFileSystemMsg extends APIQueryMessage {

    public  static List<String> __example__() {
        return asList("name:nas1");
    }
}
