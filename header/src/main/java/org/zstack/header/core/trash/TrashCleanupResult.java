package org.zstack.header.core.trash;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.SDK;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@SDK
public class TrashCleanupResult {
    private String resourceUuid;
    private boolean success = true;
    private ErrorCode error;
    private long trashId;
    private Long size = 0L;

    public TrashCleanupResult() {

    }

    public TrashCleanupResult(String resourceUuid, long trashId, long size) {
        this.resourceUuid = resourceUuid;
        this.trashId = trashId;
        this.size = size;
    }

    public TrashCleanupResult(String resourceUuid, long trashId, ErrorCode error) {
        this.resourceUuid = resourceUuid;
        this.trashId = trashId;
        this.success = false;
        this.error = error;
    }

    public TrashCleanupResult(long trashId) {
        this.trashId = trashId;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }

    public long getTrashId() {
        return trashId;
    }

    public void setTrashId(long trashId) {
        this.trashId = trashId;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public static CleanTrashResult buildCleanTrashResult(List<TrashCleanupResult> results) {
        CleanTrashResult cleanTrashResult = new CleanTrashResult();
        cleanTrashResult.setResourceUuids(results.stream()
                .filter(it -> it.getError() == null).map(TrashCleanupResult::getResourceUuid)
                .filter(Objects::nonNull).collect(Collectors.toList()));
        cleanTrashResult.setDetails(results.stream().map(TrashCleanupResult::getError)
                .filter(Objects::nonNull).map(ErrorCode::getDetails).collect(Collectors.toList()));
        cleanTrashResult.setSize(results.stream().mapToLong(TrashCleanupResult::getSize).sum());
        return cleanTrashResult;
    }
}
