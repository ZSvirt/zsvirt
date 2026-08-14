package org.zstack.header.keyprovider

doc {

    title "密钥提供程序清单"

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
        name "type"
        desc "密钥提供程序类型"
        type "String"
        since "5.0.0"
    }
    field {
        name "connected"
        desc "是否已连接"
        type "boolean"
        since "5.0.0"
    }
    field {
        name "createDate"
        desc "创建时间"
        type "Timestamp"
        since "5.0.0"
    }
    field {
        name "lastOpDate"
        desc "最后一次修改时间"
        type "Timestamp"
        since "5.0.0"
    }
}
