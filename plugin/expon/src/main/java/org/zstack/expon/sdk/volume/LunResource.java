package org.zstack.expon.sdk.volume;

import org.zstack.expon.sdk.Param;

public class LunResource {
    @Param
    private String lunId;

    @Param(validValues = {"volume", "snapshot"})
    private String lunType;

    @Param(required = false)
    private boolean isReadonly;

    public String getLunId() {
        return lunId;
    }

    public void setLunId(String lunId) {
        this.lunId = lunId;
    }

    public String getLunType() {
        return lunType;
    }

    public void setLunType(String lunType) {
        this.lunType = lunType;
    }

    public void setIsReadonly(boolean isReadonly) {
        this.isReadonly = isReadonly;
    }

    public boolean getIsReadonly() {
        return isReadonly;
    }

    public LunResource() {
    }

    public LunResource(String lunId, String lunType) {
        this.lunId = lunId;
        this.lunType = lunType;
    }

    public LunResource(String lunId, String lunType, boolean isReadonly) {
        this.lunId = lunId;
        this.lunType = lunType;
        this.isReadonly = isReadonly;
    }
}
