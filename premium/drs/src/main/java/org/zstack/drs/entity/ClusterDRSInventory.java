package org.zstack.drs.entity;

import org.zstack.drs.api.Threshold;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by lining on 2019/12/12.
 */

@PythonClassInventory
@Inventory(mappingVOClass = ClusterDRSVO.class, collectionValueOfMethod = "valueOf1")
public class ClusterDRSInventory implements Serializable {
    private String clusterUuid;
    private String state;
    private String balancedState;
    private String lastAdviceGroupUuid;
    private String automationLevel;
    private List<Threshold> thresholds;
    private Integer thresholdDuration;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String uuid;
    private String name;

    protected ClusterDRSInventory(ClusterDRSVO vo) {
        this.setClusterUuid(vo.getClusterUuid());
        this.setState(vo.getState().toString());
        this.setAutomationLevel(vo.getAutomationLevel().toString());
        this.setThresholds(JSONObjectUtil.toCollection(vo.getThresholds(), ArrayList.class, Threshold.class));
        this.setThresholdDuration(vo.getThresholdDuration());
        this.setDescription(vo.getDescription());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setBalancedState(vo.getBalancedState().toString());
        this.setLastAdviceGroupUuid(vo.getLastAdviceGroupUuid());
    }

    public static ClusterDRSInventory valueOf(ClusterDRSVO vo) {
        return new ClusterDRSInventory(vo);
    }

    public static List<ClusterDRSInventory> valueOf1(Collection<ClusterDRSVO> vos) {
        return CollectionUtils.transform(vos, ClusterDRSInventory::valueOf);
    }

    public ClusterDRSInventory() {
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String $paramName) {
        clusterUuid = $paramName;
    }

    public List<Threshold> getThresholds() {
        return thresholds;
    }

    public void setThresholds(List<Threshold> $paramName) {
        thresholds = $paramName;
    }

    public Integer getThresholdDuration() {
        return thresholdDuration;
    }

    public void setThresholdDuration(Integer $paramName) {
        thresholdDuration = $paramName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String $paramName) {
        description = $paramName;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp $paramName) {
        createDate = $paramName;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp $paramName) {
        lastOpDate = $paramName;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String $paramName) {
        uuid = $paramName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getBalancedState() {
        return balancedState;
    }

    public void setBalancedState(String balancedState) {
        this.balancedState = balancedState;
    }

    public String getLastAdviceGroupUuid() {
        return lastAdviceGroupUuid;
    }

    public void setLastAdviceGroupUuid(String lastAdviceGroupUuid) {
        this.lastAdviceGroupUuid = lastAdviceGroupUuid;
    }

    public String getAutomationLevel() {
        return automationLevel;
    }

    public void setAutomationLevel(String automationLevel) {
        this.automationLevel = automationLevel;
    }
}
