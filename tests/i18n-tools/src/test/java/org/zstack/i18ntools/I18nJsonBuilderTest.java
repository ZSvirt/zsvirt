package org.zstack.i18ntools;

import org.junit.Test;

public class I18nJsonBuilderTest {
    /**
     * cd premium/i18n-tools
     * mvn -Dmaven.test.skip=true clean install
     * mvn test -Dtest=I18nJsonBuilderTest -Dsrc=PATH_TO_PROJECT/ -Ddst=PATH_TO_PROJECT/conf/i18n_json/
     */
    @Test
    public void test() {
        System.out.println(System.getProperty("src"));

        I18nPropertiesBuilder builder = new I18nPropertiesBuilder()
                .withSrcPath(System.getProperty("src"))
                .withDstPath(System.getProperty("dst"));
        builder.generateJson();
    }
}
