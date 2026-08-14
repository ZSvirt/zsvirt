import org.junit.Test
import org.zstack.rest.RestServer
import org.zstack.utils.path.PathUtil

class TestGenerateGoSDK {

    @Test
    void Test() {
        RestServer.generateGoSDK(PathUtil.join(System.getProperty("user.home"), "zstack-go-sdk"))
    }
}