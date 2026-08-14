package org.zstack.storage.backup.sftp;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuerySftpBackupStorageReply.class, inventoryClass = SftpBackupStorageInventory.class)
@RestRequest(
        path = "/backup-storage/sftp",
        optionalPaths = {"/backup-storage/sftp/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySftpBackupStorageReply.class
)
public class APIQuerySftpBackupStorageMsg extends APIQueryMessage {


    public static List<String> __example__() {
        return asList("sshPort=8000");
    }

}
