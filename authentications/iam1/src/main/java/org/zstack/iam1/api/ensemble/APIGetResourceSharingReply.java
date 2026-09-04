package org.zstack.iam1.api.ensemble;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.iam1.entity.ensemble.AccountGroupSharingView;
import org.zstack.iam1.entity.ensemble.AccountSharingView;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by Wenhao.Zhang on 2024/08/06
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetResourceSharingReply extends APIReply {
    private String uuid;
    private String masterUuid;
    private String masterResourceType;
    private boolean toPublic;
    private List<AccountSharingView> accounts;
    private List<AccountGroupSharingView> accountGroups;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMasterUuid() {
        return masterUuid;
    }

    public void setMasterUuid(String masterUuid) {
        this.masterUuid = masterUuid;
    }

    public String getMasterResourceType() {
        return masterResourceType;
    }

    public void setMasterResourceType(String masterResourceType) {
        this.masterResourceType = masterResourceType;
    }

    public boolean isToPublic() {
        return toPublic;
    }

    public void setToPublic(boolean toPublic) {
        this.toPublic = toPublic;
    }

    public List<AccountSharingView> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountSharingView> accounts) {
        this.accounts = accounts;
    }

    public List<AccountGroupSharingView> getAccountGroups() {
        return accountGroups;
    }

    public void setAccountGroups(List<AccountGroupSharingView> accountGroups) {
        this.accountGroups = accountGroups;
    }

    public static APIGetResourceSharingReply __example__() {
        APIGetResourceSharingReply reply = new APIGetResourceSharingReply();
        reply.setUuid(uuid());
        reply.setToPublic(false);
        reply.setAccounts(list(AccountSharingView.__example__()));
        reply.setAccountGroups(list(AccountGroupSharingView.__example__()));
        return reply;
    }
}
