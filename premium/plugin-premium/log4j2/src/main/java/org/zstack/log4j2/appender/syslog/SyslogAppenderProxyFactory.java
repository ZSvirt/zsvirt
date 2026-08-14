package org.zstack.log4j2.appender.syslog;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.SyslogAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Priority;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.util.Chars;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.log4j2.Log4j2AppenderProxyFactory;
import org.zstack.log4j2.Log4j2AppenderType;
import org.zstack.log4j2.Log4j2ConfigurationStruct;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.network.NetworkUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.zstack.core.Platform.argerr;

public class SyslogAppenderProxyFactory implements Log4j2AppenderProxyFactory {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS", Locale.ENGLISH);

    @Override
    public Log4j2AppenderType getLog4j2AppenderType() {
        return Log4j2AppenderType.Syslog;
    }

    @Override
    public void validate(Log4j2ConfigurationStruct struct) {
        Log4jSystemAppenderConfig configuration = Log4jSystemAppenderConfig.fromJson(struct.getConfiguration());

        if (configuration.facility == null) {
            throw new ApiMessageInterceptionException(argerr("facility can not be null"));
        }

        if (Facility.toFacility(configuration.facility) == null) {
            throw new ApiMessageInterceptionException(argerr("invalid facility %s", configuration.facility));
        }

        if (configuration.hostname == null) {
            throw new ApiMessageInterceptionException(argerr("hostname can not be null"));
        }

        if (configuration.port == null) {
            throw new ApiMessageInterceptionException(argerr("port can not be null"));
        }

        if (configuration.protocol == null) {
            throw new ApiMessageInterceptionException(argerr("protocol can not be null"));
        }

        try {
            Protocol.valueOf(configuration.protocol);
        } catch (IllegalArgumentException e) {
            throw new ApiMessageInterceptionException(argerr("unsupported protocol %s", configuration.protocol));
        }

        if (Protocol.UDP.toString().equals(configuration.protocol)) {
            ShellResult ret = ShellUtils.runAndReturn(String.format("ping -c 1 -W 1 %s", configuration.hostname));
            if (!ret.isReturnCode(0) && !NetworkUtils.isReachable(configuration.hostname, 1000)) {
                throw new ApiMessageInterceptionException(argerr("syslog server[address: %s] is not available", configuration.hostname));
            }
        } else {
            if (CoreGlobalProperty.UNIT_TEST_ON) {
                return;
            }

            if (!NetworkUtils.isRemotePortOpen(configuration.hostname, configuration.port, 10)) {
                throw new ApiMessageInterceptionException(argerr("syslog server[address: %s:%s] is not available", configuration.hostname, configuration.port));
            }
        }
    }

    static class Log4jSystemAppenderConfig {
        String hostname;
        Integer port;
        String facility;
        String protocol;
        boolean immediateFlush;

        static Log4jSystemAppenderConfig fromJson(String json) {
            return JSONObjectUtil.toObject(json, Log4jSystemAppenderConfig.class);
        }
    }

    private static class SyslogLayout extends AbstractStringLayout {
        private final Facility facility;

        /**
         * Date format used if header = true.
         */
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS", Locale.ENGLISH);

        /**
         * Host name used to identify messages from this appender.
         */
        private final String localHostname = NetUtils.getLocalHostname();

        protected SyslogLayout(final Facility facility, final Charset charset) {
            super(charset);
            this.facility = facility;
        }

        /**
         * Formats a {@link org.apache.logging.log4j.core.LogEvent} in conformance with the BSD Log record format.
         *
         * @param event The LogEvent
         * @return the event formatted as a String.
         */
        @Override
        public String toSerializable(final LogEvent event) {
            final StringBuilder buf = getStringBuilder();

            buf.append('<');
            buf.append(Priority.getPriority(facility, event.getLevel()));
            buf.append('>');

            buf.append(localHostname);
            buf.append(Chars.SPACE);

            buf.append(dateFormat.format(new Date(event.getTimeMillis())));
            buf.append(Chars.SPACE);

            buf.append(event.getLevel().toString());
            buf.append(Chars.SPACE);

            if (event.getLoggerName() != null) {
                buf.append('[');
                buf.append(StringUtils.substringAfterLast(event.getLoggerName(), "."));
                buf.append(']');
                buf.append(Chars.SPACE);
            }

            if (event.getContextData() != null && event.getContextData().containsKey("api")) {
                buf.append("{api=");
                buf.append((String) event.getContextData().getValue("api"));
                buf.append('}');
                buf.append(Chars.SPACE);
            } else if (event.getContextData() != null && event.getContextData().containsKey("task")) {
                buf.append("{task=");
                buf.append((String) event.getContextData().getValue("task"));
                buf.append('}');
                buf.append(Chars.SPACE);
            }

            buf.append('(');
            buf.append(event.getThreadName());
            buf.append(')');
            buf.append(Chars.SPACE);

            String message = event.getMessage().getFormattedMessage();
            buf.append(message);
            buf.append('\n');
            return buf.toString();
        }

        @Override
        public Map<String, String> getContentFormat() {
            final Map<String, String> result = new HashMap<>();
            result.put("structured", "false");
            result.put("formatType", "logfilepatternreceiver");
            result.put("dateFormat", dateFormat.toPattern());
            result.put("format", "<LEVEL>TIMESTAMP PROP(HOSTNAME) MESSAGE");
            return result;
        }

        public Facility getFacility() {
            return facility;
        }
    }

    public String createLog4j2Appender(Configuration config, Log4j2ConfigurationStruct struct, String uuid) {
        Log4jSystemAppenderConfig configuration = Log4jSystemAppenderConfig.fromJson(struct.getConfiguration());

        SyslogAppenderConfigurationStruct sstruct = new SyslogAppenderConfigurationStruct();
        sstruct.setFacility(configuration.facility);
        sstruct.setHostname(configuration.hostname);
        sstruct.setImmediateFlush(configuration.immediateFlush);
        sstruct.setPort(configuration.port);
        sstruct.setProtocol(configuration.protocol);

        SyslogLayout layout = new SyslogLayout(Facility.valueOf(configuration.facility), StandardCharsets.UTF_8);
        Appender appender = SyslogAppender.newBuilder()
                .withHost(configuration.hostname)
                .withPort(configuration.port)
                .setConfiguration(config)
                .setConnectTimeoutMillis(5000)
                .setName(uuid)
                .withImmediateFlush(configuration.immediateFlush)
                .withProtocol(Protocol.valueOf(configuration.protocol))
                .setLayout(layout)
                .build();
        appender.start();
        config.addAppender(appender);

        return JSONObjectUtil.toJsonString(sstruct);
    }
}
