package org.zstack.log.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/log/servers",
        method = HttpMethod.POST,
        responseClass = APIAddLogServerEvent.class,
        parameterName = "params"
)
public class APIAddLogServerMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(validValues = {"ManagementNodeLog", "PlatformOperationLog"})
    private String category;

    @APIParam(validValues = {"Log4j2", "FluentBit"})
    private String type;

    @APIParam(required = false, validValues = {"OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL"})
    private String level;

    @APIParam(maxLength = 8192)
    private String configuration;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public static APIAddLogServerMsg __example__() {
        APIAddLogServerMsg msg = new APIAddLogServerMsg();
        msg.setName("log-server");
        msg.setDescription("log server");
        msg.setCategory("ManagementNodeLog");
        msg.setType("Log4j2");
        msg.setLevel("ALL");
        msg.setConfiguration("{\n" +
            "\"type\": \"Syslog\",\n" +
                "\"configuration\": {\n" +
                "\"hostname\": \"192.168.0.11\",\n" +
                "\"port\": \"514\",\n" +
                "\"protocol\": \"UDP\",\n" +
                "\"facility\": \"LOCAL5\"\n" +
                "}\n" +
                "}");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        APIAddLogServerEvent event = (APIAddLogServerEvent) rsp;
        return new Result(rsp.isSuccess() ? event.getInventory().getUuid() : "", LogServerVO.class);
    }
}
