package org.zstack.test.checker.db

import org.apache.commons.io.FileUtils
import org.junit.Test

import java.nio.file.Paths

class DBChecker {
    List<String> paths = [
        Paths.get("../../conf/db/upgrade").toAbsolutePath().normalize().toString(),
        Paths.get("../../conf/db/ee-history").toAbsolutePath().normalize().toString(),
    ]

    @Test
    void checkDbFolder() {
        for (final def path in paths) {
            ensureSqlFolderAvailable(path)
            scanNotNullField(path)
        }
    }

    static void ensureSqlFolderAvailable(String schemaDir) {
        def neoSchema = new File(schemaDir + "/V100.100.100__schema.sql")
        FileUtils.deleteQuietly(neoSchema)
        FileUtils.touch(neoSchema)
        FileUtils.deleteQuietly(neoSchema)
    }

    static void scanNotNullField(String schemaDir) {
        File dir = new File(schemaDir)
        dir.eachFileRecurse { schema ->
            if (!schema.name.endsWith("__schema.sql")){
                return
            }

            schema.eachLine("UTF-8") { str, ln ->
                def lineLower = str.toLowerCase()
                if (lineLower.contains("insert into resourcevo")
                    || lineLower.contains("insert into zstack.resourcevo")
                    || lineLower.contains("insert into `zstack`.`resourcevo`")) {
                    assert str.contains("concreteResourceType"), schema.name + ":" + ln
                }

                if (lineLower.contains("insert into accountresourcerefvo")
                        || lineLower.contains("insert into zstack.accountresourcerefvo")
                        || lineLower.contains("insert into `zstack`.`accountresourcerefvo`")) {
                    assert str.contains("concreteResourceType"), schema.name + ":" + ln
                }
            }
        }
    }
}
