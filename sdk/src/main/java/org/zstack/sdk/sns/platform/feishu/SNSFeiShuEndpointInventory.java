package org.zstack.sdk.sns.platform.feishu;



public class SNSFeiShuEndpointInventory extends org.zstack.sdk.sns.SNSApplicationEndpointInventory {

    public java.lang.String url;
    public void setUrl(java.lang.String url) {
        this.url = url;
    }
    public java.lang.String getUrl() {
        return this.url;
    }

    public boolean atAll;
    public void setAtAll(boolean atAll) {
        this.atAll = atAll;
    }
    public boolean getAtAll() {
        return this.atAll;
    }

    public java.util.List atPersonUserIds;
    public void setAtPersonUserIds(java.util.List atPersonUserIds) {
        this.atPersonUserIds = atPersonUserIds;
    }
    public java.util.List getAtPersonUserIds() {
        return this.atPersonUserIds;
    }

    public java.util.List atPersonList;
    public void setAtPersonList(java.util.List atPersonList) {
        this.atPersonList = atPersonList;
    }
    public java.util.List getAtPersonList() {
        return this.atPersonList;
    }

    public java.lang.String secret;
    public void setSecret(java.lang.String secret) {
        this.secret = secret;
    }
    public java.lang.String getSecret() {
        return this.secret;
    }

}
