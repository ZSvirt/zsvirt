package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIValidateSecurityGroupRuleReply

doc {
	title "ValidateSecurityGroupRule"

	category "securityGroup"

	desc """检查安全组规则是否可用"""

	rest {
		request {
			url "GET /v1/security-groups/{securityGroupUuid}/rules/validation"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIValidateSecurityGroupRuleMsg.class

			desc """检查安全组规则是否可用"""

			params {

				column {
					name "securityGroupUuid"
					enclosedIn ""
					desc "安全组的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "type"
					enclosedIn ""
					desc "安全组规则的方向"
					location "query"
					type "String"
					optional false
					since "3.17.21"
					values ("Ingress","Egress")
				}
				column {
					name "protocol"
					enclosedIn ""
					desc "安全组规则的协议类型"
					location "query"
					type "String"
					optional false
					since "3.17.21"
					values ("TCP","UDP","ICMP","ALL")
				}
				column {
					name "remoteSecurityGroupUuid"
					enclosedIn ""
					desc "远端安全组的UUID，唯一标示该资源"
					location "query"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "ipVersion"
					enclosedIn ""
					desc "安全组规则的IP版本"
					location "query"
					type "Integer"
					optional true
					since "3.17.21"
					values ("4","6")
				}
				column {
					name "srcIpRange"
					enclosedIn ""
					desc "安全组规则的源ip范围"
					location "query"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "dstIpRange"
					enclosedIn ""
					desc "安全组规则的目的ip范围"
					location "query"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "dstPortRange"
					enclosedIn ""
					desc "安全组规则的目的端口范围"
					location "query"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "action"
					enclosedIn ""
					desc "安全组规则的动作"
					location "query"
					type "String"
					optional true
					since "3.17.21"
					values ("ACCEPT","DROP")
				}
				column {
					name "startPort"
					enclosedIn ""
					desc "安全组规则的起始端口"
					location "query"
					type "Integer"
					optional true
					since "3.17.21"
				}
				column {
					name "endPort"
					enclosedIn ""
					desc "安全组规则的结束端口"
					location "query"
					type "Integer"
					optional true
					since "3.17.21"
				}
				column {
					name "allowedCidr"
					enclosedIn ""
					desc "安全组规则的ip范围"
					location "query"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.17.21"
				}
			}
		}

		response {
			clz APIValidateSecurityGroupRuleReply.class
		}
	}
}