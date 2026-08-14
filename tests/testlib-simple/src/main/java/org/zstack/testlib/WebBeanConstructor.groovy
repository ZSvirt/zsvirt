package org.zstack.testlib

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.webapp.WebAppContext
import org.jboss.shrinkwrap.api.ShrinkWrap
import org.jboss.shrinkwrap.api.spec.WebArchive
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl
import org.zstack.appliancevm.ApplianceVmGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.componentloader.ComponentLoader
import org.zstack.kvm.KVMGlobalProperty
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalProperty
import org.zstack.sdk.ZSClient
import org.zstack.sdk.ZSConfig
import org.zstack.storage.backup.sftp.SftpBackupStorageGlobalProperty
import org.zstack.utils.Utils

import java.util.concurrent.TimeUnit

/**
 * Created by xing5 on 2017/2/12.
 */
class WebBeanConstructor extends BeanConstructor {
    private Server jetty
    private static final String BASE_DIR = "target/test-classes/tomcat"
    private static final String APP_NAME = "zstack"

    static int port = 8989
    static String WEB_HOOK_PATH = "http://127.0.0.1:$port/sdk/webook"

    static final String WEB_XML_PATH = "webXmlPath"
    static final String WEB_SERVLET_CONTEXT_FILE_PATH = "webServletContextFilePath"

    WebBeanConstructor() {
        // initialize static block in Platform
        Platform.getUuid()
        KVMGlobalProperty.AGENT_PORT = port
        VirtualRouterGlobalProperty.AGENT_PORT = port
        SftpBackupStorageGlobalProperty.AGENT_PORT = port
        ApplianceVmGlobalProperty.AGENT_PORT = port
    }

    private void generateWarFile() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "zstack.war")

        def webXmlPath = System.getProperty(WEB_XML_PATH)
        if (webXmlPath == null) {
            webXmlPath = "src/test/resources/webapp/WEB-INF/web.xml"
        }

        def servletContextFilePath = System.getProperty(WEB_SERVLET_CONTEXT_FILE_PATH)
        if (servletContextFilePath == null) {
            servletContextFilePath = "src/test/resources/webapp/WEB-INF/zstack-servlet-context-groovy.xml"
        }

        war.setWebXML(new File(webXmlPath))
        war.addAsWebInfResource(new File(servletContextFilePath), "classes/zstack-servlet-context.xml")
        new ZipExporterImpl(war).exportTo(new File(Utils.getPathUtil().join(BASE_DIR, war.getName())), true)
    }

    private void prepareJetty() throws IOException {
        File dir = new File(BASE_DIR)
        dir.deleteDir()
        dir.mkdirs()

        generateWarFile()

        jetty = new Server()
        ServerConnector http = new ServerConnector(jetty)
        http.setHost("0.0.0.0")
        http.setPort(port)
        http.setDefaultProtocol("HTTP/1.1")
        jetty.addConnector(http)
        final WebAppContext webapp = new WebAppContext()
        webapp.setContextPath("/")
        webapp.setWar(new File(BASE_DIR, APP_NAME + ".war").getAbsolutePath())
        jetty.setHandler(webapp)
    }

    private void configureSDK() {
        ZSClient.configure(
                new ZSConfig.Builder()
                        .setHostname("127.0.0.1")
                        .setPort(port)
                        .setWebHook(WEB_HOOK_PATH)
                        .setDefaultPollingInterval(100, TimeUnit.MILLISECONDS)
                        .setDefaultPollingTimeout(Test.getMessageTimeoutMillsConfig(), TimeUnit.MILLISECONDS)
                        .setReadTimeout(10, TimeUnit.MINUTES)
                        .setWriteTimeout(10, TimeUnit.MINUTES)
                        .build()
        )
    }

    @Override
    ComponentLoader build() {
        configureSDK()
        generateSpringConfig()
        startWebServer()
        return Platform.getComponentLoader()
    }

    private void startWebServer() {
        prepareJetty()
        jetty.start()
    }
}
