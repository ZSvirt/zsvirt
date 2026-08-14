package org.zstack.storage.device.localRaid;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public class SmartDataStruct {
    private Integer id;

    private String attributeName;

    private String flag;

    private Integer value;

    private Integer worst;

    private Integer thresh;

    private String type;

    private String updated;

    private String whenFailed;

    private Long rawValue;

    private String state;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Integer getWorst() {
        return worst;
    }

    public void setWorst(Integer worst) {
        this.worst = worst;
    }

    public Integer getThresh() {
        return thresh;
    }

    public void setThresh(Integer thresh) {
        this.thresh = thresh;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public String getWhenFailed() {
        return whenFailed;
    }

    public void setWhenFailed(String whenFailed) {
        this.whenFailed = whenFailed;
    }

    public Long getRawValue() {
        return rawValue;
    }

    public void setRawValue(Long rawValue) {
        this.rawValue = rawValue;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
