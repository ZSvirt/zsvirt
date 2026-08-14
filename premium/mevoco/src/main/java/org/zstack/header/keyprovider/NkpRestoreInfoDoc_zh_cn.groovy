package org.zstack.header.keyprovider

doc {

    title "原生密钥提供程序导入内容"

    field {
        name "uuid"
        desc "资源的UUID，唯一标示该资源"
        type "String"
        since "5.0.0"
    }
    field {
        name "name"
        desc "资源名称"
        type "String"
        since "5.0.0"
    }
    field {
        name "description"
        desc "资源的详细描述"
        type "String"
        since "5.0.0"
    }
    field {
        name "kdf"
        desc "密钥派生函数"
        type "String"
        since "5.0.0"
    }
    field {
        name "saltPolicy"
        desc "加盐策略"
        type "String"
        since "5.0.0"
    }
    field {
        name "currentVersion"
        desc "当前版本"
        type "Integer"
        since "5.0.0"
    }
    field {
        name "backupTime"
        desc "备份时间（毫秒时间戳）"
        type "Long"
        since "5.0.0"
    }
}
