package org.zstack.header.query;

import org.zstack.header.message.APIMessage;

/**
 */
public class APIGenerateQueryableFieldsMsg extends APIMessage {
    public final static String PYTHON_FORMAT = "python";
    private String format = PYTHON_FORMAT;
    private String outputFolder;

    public static String getPythonFormat() {
        return PYTHON_FORMAT;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }
 
    public static APIGenerateQueryableFieldsMsg __example__() {
        APIGenerateQueryableFieldsMsg msg = new APIGenerateQueryableFieldsMsg();


        return msg;
    }

}
