package org.zstack.test.integration.db.schema

import org.apache.commons.io.FileUtils
import org.zstack.testlib.SubCase

import java.nio.file.Paths


/**
 * Created by Qi Le on 2022/6/6
 */
class CheckSchemaUpgradeCase extends SubCase {
    File neoSchema

    @Override
    void clean() {
        FileUtils.deleteQuietly(neoSchema)
    }

    @Override
    void setup() {
        String upgradeSchemaDir = Paths.get("../conf/db/upgrade").toAbsolutePath().normalize().toString()
        neoSchema = new File(upgradeSchemaDir + "/V100.100.100__schema.sql")
        FileUtils.deleteQuietly(neoSchema)
        FileUtils.touch(neoSchema)
        FileUtils.deleteQuietly(neoSchema)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
    }
}
