package org.zstack.log;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/log/configurations",
        method = HttpMethod.POST,
        responseClass = APIAddLogConfigurationEvent.class,
        parameterName = "params"
)
public class APIAddLogConfigurationMsg extends APICreateMessage implements APIAuditor {
    @APIParam
    private String name;
    @APIParam(required = false)
    private String description;
    @APIParam
    private String type;
    @APIParam
    private String configuration;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

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

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return null;
    }

    public static APIAddLogConfigurationMsg __example__() {
        APIAddLogConfigurationMsg msg = new APIAddLogConfigurationMsg();
        msg.setName("syslog");
        msg.setType("log4j2");
        msg.setConfiguration("{\n" +
                "\"appenderType\": \"Syslog\",\n" +
                "\"configuration\": {\n" +
                "\"hostname\": \"192.168.0.11\",\n" +
                "\"port\": \"514\",\n" +
                "\"protocol\": \"UDP\",\n" +
                "\"facility\": \"LOCAL5\"\n" +
                "}\n" +
                "}");

        return msg;
    }
}
