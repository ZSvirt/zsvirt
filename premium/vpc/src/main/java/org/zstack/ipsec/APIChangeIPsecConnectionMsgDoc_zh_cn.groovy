package org.zstack.ipsec

import org.zstack.ipsec.APIChangeIPsecConnectionEvent

doc {
	title "ChangeIPsecConnection"

	category "ipsec"

	desc """修改IPSec配置"""

	rest {
		request {
			url "PUT /v1/ipsec/config/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeIPsecConnectionMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeIPsecConnection"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "peerAddress"
					enclosedIn "changeIPsecConnection"
					desc "远端网络地址"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "authMode"
					enclosedIn "changeIPsecConnection"
					desc "认证模式"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("psk","certs")
				}
				column {
					name "authKey"
					enclosedIn "changeIPsecConnection"
					desc "认证密钥"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "idType"
					enclosedIn "changeIPsecConnection"
					desc "ID配置方法"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("ip","name","fqdn")
				}
				column {
					name "localId"
					enclosedIn "changeIPsecConnection"
					desc "本端ID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "remoteId"
					enclosedIn "changeIPsecConnection"
					desc "对端ID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "ikeVersion"
					enclosedIn "changeIPsecConnection"
					desc "IKE版本"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("ike","ikev1","ikev2")
				}
				column {
					name "ikeAuthAlgorithm"
					enclosedIn "changeIPsecConnection"
					desc "IKE验证算法"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("md5","sha1","sha256","sha384","sha512")
				}
				column {
					name "ikeEncryptionAlgorithm"
					enclosedIn "changeIPsecConnection"
					desc "IKE加密算法"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("3des","aes-128","aes-192","aes-256")
				}
				column {
					name "ikeDhGroup"
					enclosedIn "changeIPsecConnection"
					desc "IKE DH组"
					location "body"
					type "int"
					optional true
					since "0.6"
				}
				column {
					name "ikeLifeTime"
					enclosedIn "changeIPsecConnection"
					desc "IKE存活时间"
					location "body"
					type "int"
					optional true
					since "0.6"
				}
				column {
					name "policyAuthAlgorithm"
					enclosedIn "changeIPsecConnection"
					desc "ESP认证算法"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("md5","sha1","sha256","sha384","sha512")
				}
				column {
					name "policyEncryptionAlgorithm"
					enclosedIn "changeIPsecConnection"
					desc "ESP加密算法"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("3des","aes-128","aes-192","aes-256")
				}
				column {
					name "pfs"
					enclosedIn "changeIPsecConnection"
					desc "PFS DH组"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("none","dh-group2","dh-group5","dh-group14","dh-group15","dh-group16","dh-group17","dh-group18","dh-group19","dh-group20","dh-group21","dh-group22","dh-group23","dh-group24","dh-group25","dh-group26")
				}
				column {
					name "policyMode"
					enclosedIn "changeIPsecConnection"
					desc "封装模式"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("tunnel","transport")
				}
				column {
					name "transformProtocol"
					enclosedIn "changeIPsecConnection"
					desc "安全协议"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("esp","ah","ah-esp")
				}
				column {
					name "lifeTime"
					enclosedIn "changeIPsecConnection"
					desc "IPSec存活时间"
					location "body"
					type "int"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIChangeIPsecConnectionEvent.class
		}
	}
}