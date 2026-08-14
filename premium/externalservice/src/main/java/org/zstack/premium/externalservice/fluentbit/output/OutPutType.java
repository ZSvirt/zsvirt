package org.zstack.premium.externalservice.fluentbit.output;

/**
 * @author shenjin
 * @date 2024/12/2
 */
public enum OutPutType {
    Forward,
    Syslog,
    Elasticsearch,
    Kafka,
    Loki
}
