package org.zstack.network.securitygroup

import java.lang.Integer
import java.sql.Timestamp

doc {

	title "安全组规则清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "securityGroupUuid"
		desc "安全组UUID"
		type "String"
		since "0.6"
	}
	field {
		name "type"
		desc "流量类型"
		type "String"
		since "0.6"
	}
	field {
		name "ipVersion"
		desc "ip协议号"
		type "Integer"
		since "3.1"
	}
	field {
        name "protocol"
        desc "流量协议类型"
        type "String"
        since "0.6"
    }
	field {
        name "state"
        desc "规则的可用状态"
        type "String"
        since "0.6"
    }
	field {
        name "priority"
        desc "规则优先级"
        type "Integer"
        since "3.17.21"
    }
	field {
        name "description"
        desc "规则描述"
        type "String"
        since "3.17.21"
    }
	field {
		name "srcIpRange"
		desc "源IP范围"
		type "String"
		since "3.17.21"
	}
	field {
		name "dstIpRange"
		desc "目的IP范围"
		type "String"
		since "3.17.21"
	}
	field {
		name "srcPortRange"
		desc "源端口范围，当前版本未实现"
		type "String"
		since "3.17.21"
	}
	field {
		name "dstPortRange"
		desc "目的端口范围"
		type "String"
		since "3.17.21"
	}
	field {
		name "action"
		desc "规则的默认动作"
		type "String"
		since "3.17.21"
	}
	field {
		name "remoteSecurityGroupUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "allowedCidr"
		desc "允许的CIDR,根据流量类型的不同, 允许的CIDR有不同的含义,如果流量类型是Ingress, 允许的CIDR是允许访问虚拟机网卡的源CIDR,如果流量类型是Egress, 允许的CIDR是允许从虚拟机网卡离开并到达的目的地CIDR"
		type "String"
		since "0.6"
	}
	field {
		name "startPort"
		desc "如果协议是TCP/UDP, 它是端口范围（port range）的起始端口号; 如果协议是ICMP, 它是ICMP类型（type）"
		type "Integer"
		since "0.6"
	}
	field {
		name "endPort"
		desc "如果协议是TCP/UDP, 它是端口范围（port range）的起始端口号; 如果协议是ICMP, 它是ICMP类型（type）"
		type "Integer"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
}
