package org.zstack.ipsec

import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.ipsec.IPsecPeerCidrInventory
import org.zstack.ipsec.IPsecL3NetworkRefInventory

doc {

	title "ipsec"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "peerAddress"
		desc "远端网络地址"
		type "String"
		since "0.6"
	}
	field {
		name "authMode"
		desc "认证模式"
		type "String"
		since "0.6"
	}
	field {
		name "authKey"
		desc "认证密钥"
		type "String"
		since "0.6"
	}
	field {
		name "vipUuid"
		desc "VIP UUID"
		type "String"
		since "0.6"
	}
	field {
		name "ikeAuthAlgorithm"
		desc "IKE验证算法"
		type "String"
		since "0.6"
	}
	field {
		name "ikeEncryptionAlgorithm"
		desc "IKE加密算法"
		type "String"
		since "0.6"
	}
	field {
		name "ikeDhGroup"
		desc "IKE DH组"
		type "Integer"
		since "0.6"
	}
	field {
		name "policyAuthAlgorithm"
		desc "ESP认证算法"
		type "String"
		since "0.6"
	}
	field {
		name "policyEncryptionAlgorithm"
		desc "ESP加密算法"
		type "String"
		since "0.6"
	}
	field {
		name "pfs"
		desc "PFS DH组"
		type "String"
		since "0.6"
	}
	field {
		name "policyMode"
		desc "封装模式"
		type "String"
		since "0.6"
	}
	field {
		name "transformProtocol"
		desc "安全协议"
		type "String"
		since "0.6"
	}
	field {
		name "ikeVersion"
		desc "IKE版本"
		type "String"
		since "4.5"
	}
	field {
		name "idType"
		desc "ID配置方法"
		type "String"
		since "4.5"
	}
	field {
		name "localId"
		desc "本端ID"
		type "String"
		since "4.5"
	}
	field {
		name "remoteId"
		desc "对端ID"
		type "String"
		since "4.5"
	}
	field {
		name "state"
		desc "启用状态"
		type "String"
		since "0.6"
	}
	field {
		name "status"
		desc "就绪状态"
		type "String"
		since "0.6"
	}
	field {
		name "ikeLifeTime"
		desc "IKE存活时间"
		type "int"
		since "4.5"
	}
	field {
		name "lifeTime"
		desc "IPSec存活时间"
		type "int"
		since "4.5"
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
	ref {
		name "peerCidrs"
		path "org.zstack.ipsec.IPsecConnectionInventory.peerCidrs"
		desc "目的网络CIDR"
		type "List"
		since "0.6"
		clz IPsecPeerCidrInventory.class
	}
	ref {
		name "l3NetworkRefs"
		path "org.zstack.ipsec.IPsecConnectionInventory.l3NetworkRefs"
		desc "源网络CIDR"
		type "List"
		since "2.3"
		clz IPsecL3NetworkRefInventory.class
	}
}
