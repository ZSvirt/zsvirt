package org.zstack.drs.api;

import org.zstack.header.rest.SDK;

/**
 * Created by lining on 2019/12/13.
 */
@SDK
public class Threshold {
    private String thresholdName;

    private String thresholdValue;

    private String operator;

    public String getThresholdName() {
        return thresholdName;
    }

    public void setThresholdName(String thresholdName) {
        this.thresholdName = thresholdName;
    }

    public String getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(String thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public static Threshold __example__() {
        Threshold threshold = new Threshold();
        threshold.setThresholdName("cpuUsedPercentThreshold");
        threshold.setThresholdValue("90");
        threshold.setOperator(">=");
        return threshold;
    }
}
