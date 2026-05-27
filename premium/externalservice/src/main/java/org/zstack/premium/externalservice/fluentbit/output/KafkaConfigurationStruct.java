package org.zstack.premium.externalservice.fluentbit.output;

/**
 * @author shenjin
 * @date 2024/12/2
 */
public class KafkaConfigurationStruct extends ConfigurationStruct {
    String topics;

    public String getTopics() {
        return topics;
    }

    public void setTopics(String topics) {
        this.topics = topics;
    }
}
