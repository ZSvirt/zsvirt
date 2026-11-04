package org.zstack.sdktools;

import org.junit.Test;
import org.zstack.sdktools.py.Python2SdkGenerator;

public class TestGeneratePython2SDK {
    @Test
    public void test() {
        System.setProperty("exitJVMOnBootFailure", "false");
        String path = System.getProperty("user.home");
        System.out.println("generate python2 sdk by RestServer: path=" + path);
        new Python2SdkGenerator().generate(path);
    }
}
