package org.zstack.cbd;

import java.util.List;

/**
 * @author Xingwei Yu
 * @date 2024/4/2 11:13
 */
public class Config {
    private List<String> mdsUrls;
    private String logicalPoolName;

    public List<String> getMdsUrls() {
        return mdsUrls;
    }

    public void setMdsUrls(List<String> mdsUrls) {
        this.mdsUrls = mdsUrls;
    }

    public String getLogicalPoolName() {
        return logicalPoolName;
    }

    public void setLogicalPoolName(String logicalPoolName) {
        this.logicalPoolName = logicalPoolName;
    }
}
