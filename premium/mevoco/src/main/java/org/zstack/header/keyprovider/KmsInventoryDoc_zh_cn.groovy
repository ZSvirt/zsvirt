package org.zstack.header.keyprovider

doc {

    title "KMS密钥提供程序清单"

    field {
        name "endpoint"
        desc "KMS服务端地址"
        type "String"
        since "5.0.0"
    }
    field {
        name "port"
        desc "KMS服务端端口"
        type "Integer"
        since "5.0.0"
    }
    field {
        name "kmipVersion"
        desc "KMIP协议版本"
        type "String"
        since "5.0.0"
    }
    field {
        name "username"
        desc "认证用户名"
        type "String"
        since "5.0.0"
    }
    field {
        name "trustState"
        desc "KMS双边信任状态，支持双边均不信任、仅MN信任KMS、仅KMS信任MN、双边信任"
        type "String"
        since "5.0.0"
    }
    field {
        name "activeIdentityUuid"
        desc "当前生效的客户端身份UUID"
        type "String"
        since "5.0.0"
    }
    field {
        name "serverCertPem"
        desc "KMS服务端证书内容(PEM)"
        type "String"
        since "5.0.0"
    }
    ref {
        name "serverCertInfo"
        path "org.zstack.header.keyprovider.KmsInventory.serverCertInfo"
        desc "KMS服务端证书解析信息"
        type "CertificateInfo"
        since "5.0.0"
        clz CertificateInfo.class
    }
    ref {
        name "activeIdentity"
        path "org.zstack.header.keyprovider.KmsInventory.activeIdentity"
        desc "当前生效的客户端身份信息"
        type "KmsIdentityInventory"
        since "5.0.0"
        clz KmsIdentityInventory.class
    }
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
