package org.zstack.zwatch.datatype;

public class AccountResourceQuotaData {
    private String accountUuid;
    private String resourceUuid;
    private String resporceName;
    private Long useNumber;
    private Long quota;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResporceName() {
        return resporceName;
    }

    public void setResporceName(String resporceName) {
        this.resporceName = resporceName;
    }

    public Long getUseNumber() {
        return useNumber;
    }

    public void setUseNumber(Long useNumber) {
        this.useNumber = useNumber;
    }

    public Long getQuota() {
        return quota;
    }

    public void setQuota(Long quota) {
        this.quota = quota;
    }
}
