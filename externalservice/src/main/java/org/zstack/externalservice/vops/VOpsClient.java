package org.zstack.externalservice.vops;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.RestHttp;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VOpsClient {
    String hostname = "127.0.0.1";
    int port = VOpsConstant.SERVER_PORT;
    String sessionUuid;

    @Autowired
    protected RESTFacade restFacade;

    @Autowired
    protected TimeHelper timeHelper;

    public VOpsClient withHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public VOpsClient withPort(int port) {
        this.port = port;
        return this;
    }

    public VOpsClient withSession(String sessionUuid) {
        this.sessionUuid = sessionUuid;
        return this;
    }

    long getCurrentTimeMillis() {
        return timeHelper.getCurrentTimeMillis();
    }

    protected <T> RestHttp<T> http(Class<T> returnClass) {
        return restFacade.http(returnClass);
    }

    public VOpsClientRestHttp createHttp(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        VOpsClientRestHttp http = new VOpsClientRestHttp(this)
                // "http://{self.hostname}:{self.port}/{path}"
                .withPath(String.format("http://%s:%s/%s", hostname, port, path));
        if (sessionUuid != null) {
            http.withSession(sessionUuid);
        }
        return http;
    }
}
