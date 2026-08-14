package org.zstack.sdk;

import org.zstack.sdk.ErrorCode;

public class ErrorCode  {

    public java.lang.String code;
    public void setCode(java.lang.String code) {
        this.code = code;
    }
    public java.lang.String getCode() {
        return this.code;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String details;
    public void setDetails(java.lang.String details) {
        this.details = details;
    }
    public java.lang.String getDetails() {
        return this.details;
    }

    public java.lang.String i18nDetails;
    public void setI18nDetails(java.lang.String i18nDetails) {
        this.i18nDetails = i18nDetails;
    }
    public java.lang.String getI18nDetails() {
        return this.i18nDetails;
    }

    @Deprecated
    public java.lang.String elaboration;
    public void setElaboration(java.lang.String elaboration) {
        this.elaboration = elaboration;
    }
    public java.lang.String getElaboration() {
        return this.elaboration;
    }

    @Deprecated
    public ErrorCode cause;
    public void setCause(ErrorCode cause) {
        this.cause = cause;
    }
    public ErrorCode getCause() {
        return this.cause;
    }

    public java.util.List causes;
    public void setCauses(java.util.List causes) {
        this.causes = causes;
    }
    public java.util.List getCauses() {
        return this.causes;
    }

    public java.util.LinkedHashMap opaque;
    public void setOpaque(java.util.LinkedHashMap opaque) {
        this.opaque = opaque;
    }
    public java.util.LinkedHashMap getOpaque() {
        return this.opaque;
    }

}
