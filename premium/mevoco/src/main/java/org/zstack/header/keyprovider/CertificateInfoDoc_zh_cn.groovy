package org.zstack.header.keyprovider

doc {

    title "证书解析信息"

    field {
        name "subject"
        desc "证书主体DN"
        type "String"
        since "5.0.0"
    }
    field {
        name "issuer"
        desc "证书颁发者DN"
        type "String"
        since "5.0.0"
    }
    field {
        name "commonName"
        desc "证书通用名(CN)"
        type "String"
        since "5.0.0"
    }
    field {
        name "subjectAltNamesDns"
        desc "Subject Alternative Name 中的 DNS 名称列表"
        type "List"
        since "5.0.0"
    }
    field {
        name "subjectAltNamesIp"
        desc "Subject Alternative Name 中的 IP 地址列表"
        type "List"
        since "5.0.0"
    }
    field {
        name "expiredDate"
        desc "证书过期时间"
        type "Timestamp"
        since "5.0.0"
    }
}
