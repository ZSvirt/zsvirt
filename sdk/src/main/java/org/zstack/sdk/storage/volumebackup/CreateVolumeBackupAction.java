package org.zstack.sdk.storage.volumebackup;

import java.util.HashMap;
import java.util.Map;
import org.zstack.sdk.*;

public class CreateVolumeBackupAction extends AbstractAction {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    private static final HashMap<String, Parameter> nonAPIParameterMap = new HashMap<>();

    public static class Result {
        public ErrorCode error;
        public org.zstack.sdk.storage.volumebackup.CreateVolumeBackupResult value;

        public Result throwExceptionIfError() {
            if (error != null) {
                throw new ApiException(
                    String.format("error[code: %s, description: %s, details: %s]", error.code, error.description, error.details)
                );
            }
            
            return this;
        }
    }

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String volumeUuid;

    @Param(required = true, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String backupStorageUuid;

    @Param(required = true, maxLength = 255, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String name;

    @Param(required = false, maxLength = 2048, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String description;

    @Param(required = false, validValues = {"full"}, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.lang.String mode;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1024L,9223372036854775807L}, numberRangeUnit = {"Bps", "Bps"}, noTrim = false)
    public java.lang.Long volumeReadBandwidth;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1024L,9223372036854775807L}, numberRangeUnit = {"Bps", "Bps"}, noTrim = false)
    public java.lang.Long volumeWriteBandwidth;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1024L,9223372036854775807L}, numberRangeUnit = {"Bps", "Bps"}, noTrim = false)
    public java.lang.Long networkReadBandwidth;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, numberRange = {1024L,9223372036854775807L}, numberRangeUnit = {"Bps", "Bps"}, noTrim = false)
    public java.lang.Long networkWriteBandwidth;

    @Param(required = false)
    public java.lang.String resourceUuid;

    @Param(required = false, nonempty = false, nullElements = false, emptyString = true, noTrim = false)
    public java.util.List tagUuids;

    @Param(required = false)
    public java.util.List systemTags;

    @Param(required = false)
    public java.util.List userTags;

    @Param(required = false)
    public String sessionId;

    @Param(required = false)
    public String accessKeyId;

    @Param(required = false)
    public String accessKeySecret;

    @Param(required = false)
    public String requestIp;

    @NonAPIParam
    public long timeout = -1;

    @NonAPIParam
    public long pollingInterval = -1;


    private Result makeResult(ApiResult res) {
        Result ret = new Result();
        if (res.error != null) {
            ret.error = res.error;
            return ret;
        }
        
        org.zstack.sdk.storage.volumebackup.CreateVolumeBackupResult value = res.getResult(org.zstack.sdk.storage.volumebackup.CreateVolumeBackupResult.class);
        ret.value = value == null ? new org.zstack.sdk.storage.volumebackup.CreateVolumeBackupResult() : value; 

        return ret;
    }

    public Result call() {
        ApiResult res = ZSClient.call(this);
        return makeResult(res);
    }

    public void call(final Completion<Result> completion) {
        ZSClient.call(this, new InternalCompletion() {
            @Override
            public void complete(ApiResult res) {
                completion.complete(makeResult(res));
            }
        });
    }

    protected Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    protected Map<String, Parameter> getNonAPIParameterMap() {
        return nonAPIParameterMap;
    }

    protected RestInfo getRestInfo() {
        RestInfo info = new RestInfo();
        info.httpMethod = "POST";
        info.path = "/volumes/{volumeUuid}/volume-backups";
        info.needSession = true;
        info.needPoll = true;
        info.parameterName = "params";
        return info;
    }

}
