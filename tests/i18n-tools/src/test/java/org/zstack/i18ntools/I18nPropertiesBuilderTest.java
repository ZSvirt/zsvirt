package org.zstack.i18ntools;

import org.junit.Test;

public class I18nPropertiesBuilderTest {
    /**
     * cd premium/i18n-tools
     * mvn -Dmaven.test.skip=true clean install
     * mvn test -Dtest=I18nPropertiesBuilderTest -Dsrc=PATH_TO_PROJECT/conf/i18n_json/ -Ddst=PATH_TO_PROJECT/conf/i18n/
     */
    @Test
    public void test() {
        I18nPropertiesBuilder builder = new I18nPropertiesBuilder()
                .withSrcPath(System.getProperty("src"))
                .withDstPath(System.getProperty("dst"));
        builder.generateI18nPropertiesFromJson();
    }
}
