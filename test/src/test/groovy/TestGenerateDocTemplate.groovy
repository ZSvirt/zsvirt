import org.junit.Test
import org.zstack.rest.RestServer
import org.zstack.rest.sdk.DocumentGenerator

import java.nio.file.Paths

/**
 * Created by xing5 on 2016/12/21.
 */
class TestGenerateDocTemplate {

    @Test
    void test() {
        if (System.getProperty("recreate") != null) {
            RestServer.generateDocTemplate(Paths.get("../").toAbsolutePath().normalize().toString(), DocumentGenerator.DocMode.RECREATE_ALL)
        } else if (System.getProperty("repair") != null) {
            RestServer.generateDocTemplate(Paths.get("../").toAbsolutePath().normalize().toString(), DocumentGenerator.DocMode.REPAIR)
        } else {
            RestServer.generateDocTemplate(Paths.get("../").toAbsolutePath().normalize().toString(), DocumentGenerator.DocMode.CREATE_MISSING)
        }
    }
}
