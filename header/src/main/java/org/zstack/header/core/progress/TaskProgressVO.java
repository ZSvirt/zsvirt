package org.zstack.header.core.progress;

import org.zstack.header.vo.BaseResource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Table
@Entity
@BaseResource
public class TaskProgressVO {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column
    private String apiId;
    @Column
    private String content;
    @Column
    private String opaque;
    @Column
    private long createTime;
    @Column
    private long lastOpTime;
    @Column
    private long currentStep;
    @Column
    private long totalStep;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOpaque() {
        return opaque;
    }

    public void setOpaque(String opaque) {
        this.opaque = opaque;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getLastOpTime() {
        return lastOpTime;
    }

    public void setLastOpTime(long lastOpTime) {
        this.lastOpTime = lastOpTime;
    }

    public long getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(long currentStep) {
        this.currentStep = currentStep;
    }

    public long getTotalStep() {
        return totalStep;
    }

    public void setTotalStep(long totalStep) {
        this.totalStep = totalStep;
    }
}
