package org.zstack.zwatch.metricpusher;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class MetricTemplateVO {

    @Column
    @Id
    private String uuid;

    @Column
    private String receiverUuid;

    @Column
    private String template;

    @Column
    private String namespace;

    @Column
    private String metricName;

    @Column
    private String labelsJsonStr;

    @Column
    private String description;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getReceiverUuid() {
        return receiverUuid;
    }

    public void setReceiverUuid(String receiverUuid) {
        this.receiverUuid = receiverUuid;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getLabelsJsonStr() {
        return labelsJsonStr;
    }

    public void setLabelsJsonStr(String labelsJsonStr) {
        this.labelsJsonStr = labelsJsonStr;
    }
}
