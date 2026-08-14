package org.zstack.header.message;

import org.zstack.header.rest.APINoSee;

import java.util.List;
import java.util.Map;

public abstract class APIDeleteMessage extends APIMessage {
    /**
     * @desc - Permissive: allows check before deletion.
     * If any extension refuses to delete the resource, an error will
     * be returned with reason.
     * - Enforcing: deletes resource forcibly.
     * In this mode, deletion check is bypassed and all deletion errors
     * are omitted.
     * <p>
     * Default value is Permissive.
     * @choices - Permissive
     * - Enforcing
     * @optional
     */
    private String deleteMode = DeletionMode.Permissive.toString();

    /**
     * resource uuid - resource name map
     *
     * `resourceNameMap` is for auditing.
     *
     * After a resource is deleted, it becomes difficult to obtain its name.
     * Therefore, we hope to temporarily store the name of this resource
     * in APIDeleteMessage before deleting it, to ensure that the name of
     * the resource can be obtained during the audit work.
     */
    @APINoSee
    private Map<String, String> resourceNameMap;

    public APIDeleteMessage() {
    }

    public DeletionMode getDeletionMode() {
        return DeletionMode.valueOf(deleteMode);
    }

    public void setDeletionMode(DeletionMode deletionMode) {
        this.deleteMode = deletionMode.toString();
    }

    public List<String> getDeletedResourceUuidList() {
        return null;
    }

    public Map<String, String> getResourceNameMap() {
        return resourceNameMap;
    }

    public void setResourceNameMap(Map<String, String> resourceNameMap) {
        this.resourceNameMap = resourceNameMap;
    }

    public static enum DeletionMode {
        Enforcing,
        Permissive,
    }
}
