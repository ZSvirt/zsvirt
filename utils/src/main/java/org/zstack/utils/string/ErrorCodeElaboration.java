package org.zstack.utils.string;

import java.util.Locale;

/**
 * Created by mingjian.deng on 2018/11/28.
 */
public class ErrorCodeElaboration {
    private String category;
    private String code;
    private String regex;
    private String message_cn;
    private String message_en;
    private String source = "zstack";
    private double distance = 0;
    private ElaborationSearchMethod method;
    private String formatSrcError;

    public ErrorCodeElaboration() {
    }

    public static ErrorCodeElaboration clone(ErrorCodeElaboration template) {
        ErrorCodeElaboration elaboration = new ErrorCodeElaboration();
        elaboration.category = template.category;
        elaboration.code = template.code;
        elaboration.regex = template.regex;
        elaboration.message_en = template.message_en;
        elaboration.message_cn = template.message_cn;
        elaboration.source = template.source;
        elaboration.distance = template.distance;
        elaboration.formatSrcError = template.formatSrcError;
        elaboration.method = template.method;
        return elaboration;
    }

    public static ErrorCodeElaboration cloneSimple(ErrorCodeElaboration template) {
        ErrorCodeElaboration elaboration = new ErrorCodeElaboration();
        elaboration.message_en = template.message_en;
        elaboration.message_cn = template.message_cn;
        elaboration.code = template.code;
        elaboration.distance = template.distance;
        elaboration.method = template.method;
        return elaboration;
    }

    public static ErrorCodeElaboration cloneSimple(ErrorCodeElaboration template, Object...args) {
        ErrorCodeElaboration elaboration = new ErrorCodeElaboration();
        elaboration.message_en = args == null ? template.message_en : String.format(template.message_en, args);
        if (template.message_cn != null) {
            elaboration.message_cn = args == null ? template.message_cn : String.format(template.message_cn, args);
        }
        elaboration.code = template.code;
        elaboration.distance = template.distance;
        elaboration.method = template.method;
        return elaboration;
    }

    public ErrorCodeElaboration removeCnMessageIfLocaleIsNotMatch(Locale locale) {
        if (locale != null && !Locale.SIMPLIFIED_CHINESE.equals(locale)) {
            message_cn = null;
        }
        return this;
    }

    public ErrorCodeElaboration addElaborationMessage(ErrorCodeElaboration other) {
        if (message_en != null) {
            message_en = message_en + "," + other.message_en;
        } else {
            message_en = other.message_en;
        }

        if (message_cn != null) {
            message_cn = message_cn + "," + other.message_cn;
        } else {
            message_cn = other.message_cn;
        }
        return this;
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

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getFormatSrcError() {
        return formatSrcError;
    }

    public void setFormatSrcError(String formatSrcError) {
        this.formatSrcError = formatSrcError;
    }

    public ElaborationSearchMethod getMethod() {
        return method;
    }

    public void setMethod(ElaborationSearchMethod method) {
        this.method = method;
    }
}
