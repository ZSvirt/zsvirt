package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APICreateSecurityGroupEvent

doc {
	title "CreateSecurityGroup"

	category "securityGroup"

	desc """用户可以使用CreateSecurityGroup来创建一个安全组"""

	rest {
		request {
			url "POST /v1/security-groups"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSecurityGroupMsg.class

			desc """用户可以使用CreateSecurityGroup来创建一个安全组"""

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
					name "ipVersion"
					enclosedIn "params"
					desc "ip协议号"
					location "body"
					type "Integer"
					optional true
					since "3.1"
					values ("4","6")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "用户指定的资源UUID，若指定，系统不会为该资源随机分配UUID"
					location "body"
					type "String"
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
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APICreateSecurityGroupEvent.class
		}
	}
}