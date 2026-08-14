package org.zstack.ldap.entity

doc {

    title "LDAP服务器清单"

    field {
        name "uuid"
        desc "资源的UUID，唯一标示该资源"
        type "String"
        since "4.3.0"
    }
    field {
        name "name"
        desc "资源名称"
        type "String"
        since "4.3.0"
    }
    field {
        name "type"
        desc "用户源类型，一般是ldap"
        type "String"
        since "4.3.0"
    }
    field {
        name "description"
        desc "资源的详细描述"
        type "String"
        since "4.3.0"
    }
    field {
        name "url"
        desc ""
        type "String"
        since "4.3.0"
    }
    field {
        name "base"
        desc "LDAP服务器访问地址"
        type "String"
        since "4.3.0"
    }
    field {
        name "username"
        desc "用于访问LDAP服务器的用户名"
        type "String"
        since "4.3.0"
    }
    field {
        name "password"
        desc "密码"
        type "String"
        since "4.3.0"
    }
    field {
        name "encryption"
        desc "加密方式"
        type "String"
        since "4.3.0"
    }
    field {
        name "serverType"
        desc "服务器类型，可能是OpenLdap或WindowsAD，如果不确定会返回Unknown"
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
