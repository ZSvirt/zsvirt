package org.zstack.zwatch.alarm;

import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = AlertDataAckVO.class)
public class AlertDataAckInventory implements Serializable {
    private String alertDataUuid;
    private String alertType;
    private Long ackPeriod;
    private String resourceUuid;
    private Timestamp ackDate;
    private boolean resumeAlert;
    private String operatorAccountUuid;

    protected AlertDataAckInventory(AlertDataAckVO vo) {
        this.setAlertDataUuid(vo.getAlertDataUuid());
        this.setAlertType(vo.getAlertType());
        this.setAckPeriod(vo.getAckPeriod());
        this.setResourceUuid(vo.getResourceUuid());
        this.setAckDate(vo.getAckDate());
        this.setResumeAlert(vo.isResumeAlert());
        this.setOperatorAccountUuid(vo.getOperatorAccountUuid());
    }

    public static AlertDataAckInventory valueOf(AlertDataAckVO vo) {
        return new AlertDataAckInventory(vo);
    }

    public static List<AlertDataAckInventory> valueOf(Collection<AlertDataAckVO> cos) {
        return cos.stream().map(AlertDataAckInventory::valueOf).collect(Collectors.toList());
    }

    public AlertDataAckInventory() {
    }

    public String getAlertDataUuid() {
        return alertDataUuid;
    }

    public void setAlertDataUuid(String $paramName) {
        alertDataUuid = $paramName;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String $paramName) {
        alertType = $paramName;
    }

    public Long getAckPeriod() {
        return ackPeriod;
    }

    public void setAckPeriod(Long $paramName) {
        ackPeriod = $paramName;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String $paramName) {
        resourceUuid = $paramName;
    }

    public Timestamp getAckDate() {
        return ackDate;
    }

    public void setAckDate(Timestamp $paramName) {
        ackDate = $paramName;
    }

    public boolean isResumeAlert() {
        return resumeAlert;
    }

    public void setResumeAlert(boolean resumeAlert) {
        this.resumeAlert = resumeAlert;
    }

    public String getOperatorAccountUuid() {
        return operatorAccountUuid;
    }

    public void setOperatorAccountUuid(String operatorAccountUuid) {
        this.operatorAccountUuid = operatorAccountUuid;
    }
}
