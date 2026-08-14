package org.zstack.network.service.flat

import java.sql.Timestamp

doc {

    title "IP 使用情况结构"

    field {
        name "ip"
        desc "IP 地址"
        type "String"
        since "3.7.0"
    }
    field {
        name "vipUuid"
        desc "虚拟 IP 的 UUID"
        type "String"
        since "3.7.0"
    }
    field {
        name "vipName"
        desc "虚拟 IP 名字"
        type "String"
        since "3.7.0"
    }
    field {
        name "vmInstanceUuid"
        desc "虚拟机实例 UUID"
        type "String"
        since "3.7.0"
    }
    field {
        name "vmInstanceName"
        desc "虚拟机实例名字"
        type "String"
        since "3.7.0"
    }
    field {
        name "vmInstanceType"
        desc "虚拟机实例类型"
        type "String"
        since "3.7.0"
    }
    field {
        name "applianceVmOwnerUuid"
        desc "应用虚拟机实例的网络服务 UUID"
        type "String"
        since "3.13.12"
    }
    field {
        name "vmDefaultIp"
        desc "虚拟机实例默认 IP"
        type "String"
        since "3.7.0"
    }
    field {
        name "resourceTypes"
        desc "绑定到 IP 地址的资源类型列表"
        type "List"
        since "3.7.0"
    }
    field {
        name "state"
        desc "资源状态"
        type "String"
        since "3.7.0"
    }
    field {
        name "useFor"
        desc "虚拟 IP 绑定的网络服务"
        type "String"
        since "3.7.0"
    }
    field {
        name "createDate"
        desc "创建时间"
        type "Timestamp"
        since "3.7.0"
    }
    field {
        name "ownerName"
        desc "资源所有者名字"
        type "String"
        since "3.7.0"
    }
    field {
        name "resourceOwnerUuid"
        desc "资源所有者的 UUID"
        type "String"
        since "3.11.0"
    }
    field {
        name "usedIpUuid"
        desc "IP 的 UUID"
        type "String"
        since "3.11.0"
    }
}
