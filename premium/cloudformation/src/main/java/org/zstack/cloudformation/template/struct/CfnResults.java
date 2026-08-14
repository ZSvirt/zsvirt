package org.zstack.cloudformation.template.struct;

import com.google.gson.GsonBuilder;
import org.apache.commons.collections.map.HashedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by mingjian.deng on 2018/5/30.
 */
public class CfnResults {
    private String templateVersion;
    private String description;
    private List<PreParameterStruct> preparams = new ArrayList<>();
    private List<ParameterStruct> params = new ArrayList<>();
    private Map<String, Boolean> conditionParams = new HashedMap();
    private List<String> requiredParams = new ArrayList<>();
    private List<MappingStruct> mappings = new ArrayList<>();
    private List<ResourceStruct> resources = new ArrayList<>();
    private List<OutputStruct> outputs = new ArrayList<>();

    public List<ParameterStruct> getParams() {
        return params;
    }

    public void setParams(List<ParameterStruct> params) {
        this.params = params;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public void setTemplateVersion(String templateVersion) {
        this.templateVersion = templateVersion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<MappingStruct> getMappings() {
        return mappings;
    }

    public void setMappings(List<MappingStruct> mappings) {
        this.mappings = mappings;
    }

    public List<String> getRequiredParams() {
        return requiredParams;
    }

    public void setRequiredParams(List<String> requiredParams) {
        this.requiredParams = requiredParams;
    }

    public List<ResourceStruct> getResources() {
        return resources;
    }

    public void setResources(List<ResourceStruct> resources) {
        this.resources = resources;
    }

    public List<OutputStruct> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<OutputStruct> outputs) {
        this.outputs = outputs;
    }

    public String print() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }

    public Map<String, Boolean> getConditionParams() {
        return conditionParams;
    }

    public void setConditionParams(Map<String, Boolean> conditionParams) {
        this.conditionParams = conditionParams;
    }
    public List<PreParameterStruct> getPreparams() {
        return preparams;
    }

    public void setPreparams(List<PreParameterStruct> preparams) {
        this.preparams = preparams;
    }
}
