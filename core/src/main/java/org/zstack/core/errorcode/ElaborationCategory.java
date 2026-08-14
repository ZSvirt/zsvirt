package org.zstack.core.errorcode;

import org.zstack.header.rest.SDK;

/**
 * Created by mingjian.deng on 2018/12/1.
 */
@SDK
public class ElaborationCategory {
    private String category;
    private Integer num;

    public ElaborationCategory() {
    }

    public ElaborationCategory(String category, Integer num) {
        this.category = category;
        this.num = num;
    }

    public static ElaborationCategory __example__() {
        return new ElaborationCategory("BS", 10);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }
}
