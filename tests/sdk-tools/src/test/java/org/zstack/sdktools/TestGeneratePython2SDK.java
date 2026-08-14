package org.zstack.sdktools;

import org.junit.Test;
import org.zstack.testlib.PythonSdkGenerator;

public class TestGeneratePython2SDK {
    @Test
    public void test() {
        System.setProperty("exitJVMOnBootFailure", "false");
        String path = System.getProperty("user.home");
        System.out.println("generate python2 sdk by RestServer: path=" + path);
        new PythonSdkGenerator().generate(path);
    }
}
