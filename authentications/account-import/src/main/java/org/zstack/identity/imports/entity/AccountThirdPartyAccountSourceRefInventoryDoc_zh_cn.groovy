package org.zstack.identity.imports.entity

doc {

    title "导入用户与账户绑定关系清单"

    field {
        name "id"
        desc "唯一标示该关系"
        type "Long"
        since "4.3.0"
    }
    field {
        name "credentials"
        desc "导入源使用的用户唯一标识，例如LDAP源导入的用户则为登录使用的UID"
        type "String"
        since "4.3.0"
    }
    field {
        name "accountSourceUuid"
        desc "导入源服务器 UUID"
        type "String"
        since "4.3.0"
    }
    field {
        name "accountUuid"
        desc "账户UUID"
        type "String"
        since "4.3.0"
    }
    field {
        name "createDate"
        desc "创建时间"
        type "Timestamp"
        since "4.3.0"
    }
    field {
        name "lastOpDate"
        desc "最后一次修改时间"
        type "Timestamp"
        since "4.3.0"
    }
}
