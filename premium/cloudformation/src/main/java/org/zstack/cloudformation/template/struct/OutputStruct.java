package org.zstack.cloudformation.template.struct;

/**
 * Created by mingjian.deng on 2018/5/31.
 */
public class OutputStruct implements ZStackResourceTemplateStruct {
    private String outputName; // key for output
    private String description;
    private Object content;

    public String getOutputName() {
        return outputName;
    }

    public void setOutputName(String outputName) {
        this.outputName = outputName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }
}
