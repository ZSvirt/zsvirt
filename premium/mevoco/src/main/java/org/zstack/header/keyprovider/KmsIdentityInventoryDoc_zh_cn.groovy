package org.zstack.header.keyprovider

doc {

    title "KMS身份信息清单"

    field {
        name "uuid"
        desc "资源的UUID，唯一标示该资源"
        type "String"
        since "5.0.0"
    }
    field {
        name "kmsUuid"
        desc "KmsUUID"
        type "String"
        since "5.0.0"
    }
    field {
        name "identityType"
        desc "身份类型"
        type "String"
        since "5.0.0"
    }
    field {
        name "clientCertPem"
        desc "KMS客户端证书"
        type "String"
        since "5.0.0"
    }
    field {
        name "csrPem"
        desc "CSR内容"
        type "String"
        since "5.0.0"
    }
    field {
        name "certExpiredDate"
        desc "客户端证书到期时间"
        type "Timestamp"
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
