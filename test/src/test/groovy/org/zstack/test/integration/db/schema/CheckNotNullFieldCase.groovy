package org.zstack.test.integration.db.schema

import org.zstack.core.db.Q
import org.zstack.header.vo.ResourceVO
import org.zstack.header.vo.ResourceVO_
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.VersionComparator

import java.nio.file.Paths


class CheckNotNullFieldCase extends SubCase{
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
        env = makeEnv {}
    }

    @Override
    void test() {
        checkNotNullField(Paths.get("../conf/db/upgrade").toAbsolutePath().normalize().toString())
    }

    static void checkNotNullField(String upgradeSchemaDir) {
        File dir = new File(upgradeSchemaDir)
        dir.eachFileRecurse { schema ->
            if (!schema.name.contains("__")){
                return
            }

            String version = schema.name.split("__")[0].drop(1)
            VersionComparator schemaVersion = new VersionComparator(version)
            if (schemaVersion.compare("2.6.0") > 0) {
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

        long count1 = Q.New(ResourceVO.class).eq(ResourceVO_.concreteResourceType, null).count()
        assert count1 == 0, "Found null on field[ResourceVO.concreteResourceType], but it has not null property"
    }
}
