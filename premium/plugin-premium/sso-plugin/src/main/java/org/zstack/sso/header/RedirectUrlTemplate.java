package org.zstack.sso.header;

import org.zstack.header.rest.SDK;

/**
 * @Author: DaoDao
 * @Date: 2022/9/8
 */
@SDK
public class RedirectUrlTemplate {
    private String urlTemplate;
    private String name;
    private String description;

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
