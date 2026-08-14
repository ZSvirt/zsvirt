import org.junit.Test
import org.zstack.testlib.PythonSdkGenerator

/**
 * Created by xing5 on 2017/4/9.
 */
class TestGeneratePythonSDK {
    @Test
    void test() {
        new PythonSdkGenerator().generate(System.getProperty("user.home"))
    }
}
