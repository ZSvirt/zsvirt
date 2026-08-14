package org.zstack.premium.externalservice.fluentbit.output;

/**
 * @author shenjin
 * @date 2024/12/2
 */
public class ElasticsearchConfigurationStruct extends ConfigurationStruct {
    String httpUser;
    String httpPassword;
    String tls;
    String index;

    public String getHttpUser() {
        return httpUser;
    }

    public void setHttpUser(String httpUser) {
        this.httpUser = httpUser;
    }

    public String getHttpPassword() {
        return httpPassword;
    }

    public void setHttpPassword(String httpPassword) {
        this.httpPassword = httpPassword;
    }

    public String getTls() {
        return tls;
    }

    public void setTls(String tls) {
        this.tls = tls;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }
}
