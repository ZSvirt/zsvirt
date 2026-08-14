package org.zstack.rest.sdk;

/**
 * Created by xing5 on 2016/12/23.
 */
public interface DocumentGenerator {
    enum DocMode {
        RECREATE_ALL,
        CREATE_MISSING,
        REPAIR,
    }

    void generateDocTemplates(String scanPath, DocMode mode);

    void generateMarkDown(String scanPath, String resultDir);

    void generateErrorCodeDoc(String scanPath, String resultFile);

    void generateGlobalConfigMarkDown(String sourcePath, DocMode mode);
}
