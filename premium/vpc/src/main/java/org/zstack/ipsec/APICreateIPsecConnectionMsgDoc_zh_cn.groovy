package org.zstack.ipsec

import org.zstack.ipsec.APICreateIPsecConnectionEvent

doc {
	title "创建IPSec连接(CreateIPsecConnection)"

	category "ipsec"

	desc """创建IPSec连接"""

	rest {
		request {
			url "POST /v1/ipsec"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateIPsecConnectionMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "peerAddress"
					enclosedIn "params"
					desc "远端网络地址"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "authMode"
					enclosedIn "params"
					desc "认证模式"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("psk","certs")
				}
				column {
					name "authKey"
					enclosedIn "params"
					desc "认证密钥"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "idType"
					enclosedIn "params"
					desc "ID配置方法"
					location "body"
					type "String"
					optional true
					since "3.15"
					values ("ip","name","fqdn")
				}
				column {
					name "vipUuid"
					enclosedIn "params"
					desc "VIP UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "localId"
					enclosedIn "params"
					desc "本端ID"
					location "body"
					type "String"
					optional true
					since "3.15"
				}
				column {
					name "remoteId"
					enclosedIn "params"
					desc "对端ID"
					location "body"
					type "String"
					optional true
					since "3.15"
				}
				column {
					name "peerCidrs"
					enclosedIn "params"
					desc "目的网络CIDR"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "ikeVersion"
					enclosedIn "params"
					desc "IKE版本"
					location "body"
					type "String"
					optional true
					since "3.15"
					values ("ike","ikev1","ikev2")
				}
				column {
					name "ikeAuthAlgorithm"
					enclosedIn "params"
					desc "IKE验证算法"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("md5","sha1","sha256","sha384","sha512")
				}
				column {
					name "ikeEncryptionAlgorithm"
					enclosedIn "params"
					desc "IKE加密算法"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("3des","aes-128","aes-192","aes-256")
				}
				column {
					name "ikeDhGroup"
					enclosedIn "params"
					desc "IKE DH组"
					location "body"
					type "int"
					optional true
					since "0.6"
				}
				column {
					name "ikeLifeTime"
					enclosedIn "params"
					desc "IKE存活时间"
					location "body"
					type "int"
					optional true
					since "3.15"
				}
				column {
					name "policyAuthAlgorithm"
					enclosedIn "params"
					desc "ESP认证算法"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("md5","sha1","sha256","sha384","sha512")
				}
				column {
					name "policyEncryptionAlgorithm"
					enclosedIn "params"
					desc "ESP加密算法"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("3des","aes-128","aes-192","aes-256")
				}
				column {
					name "pfs"
					enclosedIn "params"
					desc "PFS DH组"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("none","dh-group0","dh-group2","dh-group5","dh-group14","dh-group15","dh-group16","dh-group17","dh-group18","dh-group19","dh-group20","dh-group21","dh-group22","dh-group23","dh-group24","dh-group25","dh-group26")
				}
				column {
					name "policyMode"
					enclosedIn "params"
					desc "封装模式"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("tunnel","transport")
				}
				column {
					name "transformProtocol"
					enclosedIn "params"
					desc "安全协议"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("esp","ah","ah-esp")
				}
				column {
					name "lifeTime"
					enclosedIn "params"
					desc "IPSec存活时间"
					location "body"
					type "int"
					optional true
					since "3.15"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.15"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.15"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.15"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.15"
				}
			}
		}

		response {
			clz APICreateIPsecConnectionEvent.class
		}
	}
}