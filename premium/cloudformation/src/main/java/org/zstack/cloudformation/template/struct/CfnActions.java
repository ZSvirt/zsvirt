package org.zstack.cloudformation.template.struct;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;
import org.zstack.sdk.NonAPIParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/6/1.
 */
public class CfnActions {
    private List<ActionStruct> actions = new ArrayList<>();
    private List<ResultStruct> results = new ArrayList<>();
    private CloudFormationErrorCode errCode = new CloudFormationErrorCode();
    private boolean success = true;

    public List<ActionStruct> getActions() {
        return actions;
    }

    public void setActions(List<ActionStruct> actions) {
        this.actions = actions;
    }

    public List<ResultStruct> getResults() {
        return results;
    }

    public void setResults(List<ResultStruct> results) {
        this.results = results;
    }

    public CloudFormationErrorCode getErrCode() {
        return errCode;
    }

    public void setErrCode(CloudFormationErrorCode errCode) {
        this.errCode = errCode;
        this.success = false;
    }

    public String print() {
        return new GsonBuilder().setPrettyPrinting().addSerializationExclusionStrategy(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                return fieldAttributes.getAnnotation(NonAPIParam.class) != null;
            }

            @Override
            public boolean shouldSkipClass(Class<?> aClass) {
                return false;
            }
        }).create().toJson(this);
    }

    public boolean isSuccess() {
        return success;
    }
}
