package org.zstack.zwatch;

public interface ZWatchConstants {
    String SERVICE_ID = "zwatch";
    String CATEGORY = "zwatch";

    String DATA_ACCOUNT_UUID = "accountUuid";
    String DATA_UUID = "dataUuid";

    String DATA_READ_STATUS = "readStatus";
    String DATA_READ_STATUS_UNREAD = "Unread";
    String DATA_READ_STATUS_READ = "Read";

    String LABEL_NAME = "name";
    String LABEL_NAMESPACE = "namespace";

    //Modify only one piece of data
    String UPDATE_DATA_MODE_ONLYONE = "OnlyOne";
    //Modify multiple pieces of data
    String UPDATE_DATA_MODE_INRANGE = "InRange";
    //Modify all data
    String UPDATE_DATA_MODE_ALL = "All";

    String AUDIT_SUCCESS = "success";
    String AUDIT_FAILED = "failed";

    String NODE_LIFECYCLE_LEFT_CANONICAL_EVENT_PATH = "/managementnode/lifecycle/left";

    String readStatus = "readStatus!=''";

    String alarmAllString = "(alarmUuid =~ /%s/ and time >= %s and time <= %s)";
    String alarmStartTimeString = "(alarmUuid =~ /%s/ and time >= %s)";
    String alarmEndTimeString = "(alarmUuid =~ /%s/ and time <= %s)";
    String alarmNotTimeString = "(alarmUuid =~ /%s/)";
    String alarmMysqlAllString = "(a.alarmUuid = \'%s\' and a.createTime >= %s and a.createTime <= %s)";
    String alarmMysqlStartTimeString = "(a.alarmUuid = \'%s\'  and a.createTime >= %s)";
    String alarmMysqlEndTimeString = "(a.alarmUuid = \'%s\' and a.createTime <= %s)";
    String alarmMysqlNotTimeString = "(a.alarmUuid = \'%s\')";
    String subscriptionAllString = "(subscriptionUuid =~ /%s/ and time >= %s and time <= %s and readStatus!='')";
    String subscriptionStartString = "(subscriptionUuid =~ /%s/ and time >= %s and readStatus!='')";
    String subscriptionEndTimeString = "(subscriptionUuid =~ /%s/ and time <= %s and readStatus!='')";
    String subscriptionNotTimeString = "(subscriptionUuid =~ /%s/ and readStatus!='')";
    String subscriptionMysqlAllString = "(a.subscriptionUuid = \'%s\' and a.createTime >= %s and a.createTime <= %s and a.readStatus!='')";
    String subscriptionMysqlStartString = "(a.subscriptionUuid = \'%s\' and a.createTime >= %s and a.readStatus!='')";
    String subscriptionMysqlEndTimeString = "(a.subscriptionUuid = \'%s\' and a.createTime <= %s and a.readStatus!='')";
    String subscriptionMysqlNotTimeString = "(a.subscriptionUuid = \'%s\' and a.readStatus!='')";

    String ALARM_DATA_TYPE = "alarm";
    String EVENT_DATA_TYPE = "event";

    String WINDOWS_HARDDISK_PREFIX = "Harddisk.*";

    String IGNORED_FS_TYPE = "proc|tmpfs|rootfs|ramfs|iso9660|rpc_pipefs|fuse.lxcfs|nsfs";
}
