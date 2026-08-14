package org.zstack.header.identity.quota;

public class FixedSizeRequiredRequest implements QuotaRequiredRequest {
    public FixedSizeRequiredRequest(String quotaName, Long value) {
        this.quotaName = quotaName;
        this.value = value;
    }

    private String quotaName;
    private Long value;

    @Override
    public String getQuotaName() {
        return quotaName;
    }

    public void setQuotaName(String quotaName) {
        this.quotaName = quotaName;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }
}
