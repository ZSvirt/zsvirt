package org.zstack.sdk;

import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2016/12/9.
 */
public class ZSConfig {
    String hostname = "localhost";
    int port = 8080;
    long defaultPollingTimeout = TimeUnit.HOURS.toMillis(3);
    long defaultPollingInterval = TimeUnit.SECONDS.toMillis(1);
    String webHook;
    Long readTimeout;
    Long writeTimeout;
    String contextPath;
    String accessKeyDateStr;

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public long getDefaultPollingTimeout() {
        return defaultPollingTimeout;
    }

    public long getDefaultPollingInterval() {
        return defaultPollingInterval;
    }

    public static class Builder {
        ZSConfig config = new ZSConfig();

        public Builder setHostname(String hostname) {
            config.hostname = hostname;
            return this;
        }

        public Builder setWebHook(String webHook) {
            config.webHook = webHook;
            return this;
        }

        public Builder setPort(int port) {
            config.port = port;
            return this;
        }

        public Builder setDefaultPollingTimeout(long value, TimeUnit unit) {
            config.defaultPollingTimeout = unit.toMillis(value);
            return this;
        }

        public Builder setDefaultPollingInterval(long value, TimeUnit unit) {
            config.defaultPollingInterval = unit.toMillis(value);
            return this;
        }

        public Builder setReadTimeout(long value, TimeUnit unit) {
            config.readTimeout = unit.toMillis(value);
            return this;
        }

        public Builder setWriteTimeout(long value, TimeUnit unit) {
            config.writeTimeout = unit.toMillis(value);
            return this;
        }

        public Builder setContextPath(String name) {
            config.contextPath = name;
            return this;
        }

        public Builder setAccessKeyDateStr(String name) {
            config.accessKeyDateStr = name;
            return this;
        }

        public ZSConfig build() {
            return config;
        }
    }
}
