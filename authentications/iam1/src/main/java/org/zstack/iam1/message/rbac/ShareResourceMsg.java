package org.zstack.iam1.message.rbac;

import org.zstack.header.identity.APIShareResourceMsg;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.iam1.api.accounts.APIShareResourceToGroupMsg;

import java.util.List;

public class ShareResourceMsg extends NeedReplyMessage {
    private List<String> resourceUuids;
    private List<String> accountUuids;
    private boolean toPublic;
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

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public static ShareResourceMsg valueOf(APIShareResourceMsg message) {
        ShareResourceMsg innerMsg = new ShareResourceMsg();
        innerMsg.setResourceUuids(message.getResourceUuids());

        if (message.isToPublic()) {
            innerMsg.setToPublic(true);
        } else {
            innerMsg.setAccountUuids(message.getAccountUuids());
        }

        return innerMsg;
    }

    public static ShareResourceMsg valueOf(APIShareResourceToGroupMsg message) {
        ShareResourceMsg innerMsg = new ShareResourceMsg();
        innerMsg.setResourceUuids(message.getResourceUuids());
        innerMsg.setGroupUuid(message.getGroupUuid());
        return innerMsg;
    }
}
