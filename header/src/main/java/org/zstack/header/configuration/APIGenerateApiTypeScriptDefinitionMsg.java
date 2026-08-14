package org.zstack.header.configuration;

import org.zstack.header.message.APIMessage;

/**
 */
public class APIGenerateApiTypeScriptDefinitionMsg extends APIMessage {
    private String outputPath;

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }
 
    public static APIGenerateApiTypeScriptDefinitionMsg __example__() {
        APIGenerateApiTypeScriptDefinitionMsg msg = new APIGenerateApiTypeScriptDefinitionMsg();


        return msg;
    }

}
