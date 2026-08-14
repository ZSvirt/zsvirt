package org.zstack.header.storage.backup;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @apiResult api reply for message :ref:`APIGetBackupStorageTypesMsg`
 * @example {
 * "org.zstack.header.storage.backup.APIGetBackupStorageTypesReply": {
 * "backupStorageTypes": [
 * "SimulatorBackupStorage"
 * ],
 * "success": true
 * }
 * }
 * @since 0.1.0
 */

@RestResponse(fieldsTo = {"types=backupStorageTypes"})
public class APIGetBackupStorageTypesReply extends APIReply {
    /**
     * @desc a list of backup storage types supported by zstack
     */
    private List<String> backupStorageTypes;

    public List<String> getBackupStorageTypes() {
        return backupStorageTypes;
    }

    public void setBackupStorageTypes(List<String> backupStorageTypes) {
        this.backupStorageTypes = backupStorageTypes;
    }
 
    public static APIGetBackupStorageTypesReply __example__() {
        APIGetBackupStorageTypesReply reply = new APIGetBackupStorageTypesReply();

        reply.setBackupStorageTypes(asList("Ceph", "ImageStore"));

        return reply;
    }

}
