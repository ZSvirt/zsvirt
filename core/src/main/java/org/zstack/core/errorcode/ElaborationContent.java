package org.zstack.core.errorcode;

import org.zstack.header.rest.SDK;
import org.zstack.utils.string.ElaborationSearchMethod;
import org.zstack.utils.string.ErrorCodeElaboration;

/**
 * Created by mingjian.deng on 2019/1/4.
 */
@SDK
public class ElaborationContent implements Comparable<ElaborationContent> {
    private String category;
    private String code;
    private String regex;
    private String message_cn;
    private String message_en;
    private String source = "zstack";
    private String method;
    private Double distance;

    public ElaborationContent() {
    }

    public ElaborationContent(ErrorCodeElaboration error) {
        category = error.getCategory();
        code = error.getCode();
        regex = error.getRegex();
        message_cn = error.getMessage_cn();
        message_en = error.getMessage_en();
        if (error.getMethod() == null) {
            method = ElaborationSearchMethod.regex.toString();
        } else {
            method = error.getMethod().toString();
        }
        distance = error.getDistance();
        source = error.getSource();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getMessage_cn() {
        return message_cn;
    }

    public void setMessage_cn(String message_cn) {
        this.message_cn = message_cn;
    }

    public String getMessage_en() {
        return message_en;
    }

    public void setMessage_en(String message_en) {
        this.message_en = message_en;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    @Override
    public int compareTo(ElaborationContent o) {
        if (category.equalsIgnoreCase(o.category)) {
            return code.compareTo(o.code);
        }
        return category.compareTo(o.category);
    }
}
