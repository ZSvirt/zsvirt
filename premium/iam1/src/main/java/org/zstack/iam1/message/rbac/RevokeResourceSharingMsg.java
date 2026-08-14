package org.zstack.iam1.message.rbac;

import org.zstack.header.identity.APIRevokeResourceSharingMsg;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.iam1.api.accounts.APIRevokeResourceSharingToGroupMsg;

import java.util.List;

public class RevokeResourceSharingMsg extends NeedReplyMessage {
    private List<String> resourceUuids;
    private List<String> accountUuids;
    private boolean toPublic;
    private boolean all;
    private String groupUuid;

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }

    public List<String> getAccountUuids() {
        return accountUuids;
    }

    public void setAccountUuids(List<String> accountUuids) {
        this.accountUuids = accountUuids;
    }

    public boolean isToPublic() {
        return toPublic;
    }

    public void setToPublic(boolean toPublic) {
        this.toPublic = toPublic;
    }

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public static RevokeResourceSharingMsg valueOf(APIRevokeResourceSharingMsg message) {
        RevokeResourceSharingMsg innerMsg = new RevokeResourceSharingMsg();
        innerMsg.setResourceUuids(message.getResourceUuids());

        if (message.isAll()) {
            innerMsg.setAll(true);
        } else if (message.isToPublic()) {
            innerMsg.setToPublic(true);
        } else {
            innerMsg.setAccountUuids(message.getAccountUuids());
        }

        return innerMsg;
    }

    public static RevokeResourceSharingMsg valueOf(APIRevokeResourceSharingToGroupMsg message) {
        RevokeResourceSharingMsg innerMsg = new RevokeResourceSharingMsg();
        innerMsg.setResourceUuids(message.getResourceUuids());
        innerMsg.setGroupUuid(message.getGroupUuid());
        return innerMsg;
    }
}
