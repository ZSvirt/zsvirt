package org.zstack.network.service.portforwarding

import org.zstack.network.service.portforwarding.APICreatePortForwardingRuleEvent

doc {
	title "CreatePortForwardingRule"

	category "portForwarding"

	desc """用户可以使用CreatePortForwardingRule来创建一个端口转发规则, 并可以同时挂载或者不挂载到虚拟机网卡上"""

	rest {
		request {
			url "POST /v1/port-forwarding"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreatePortForwardingRuleMsg.class

			desc """用户可以使用CreatePortForwardingRule来创建一个端口转发规则, 并可以同时挂载或者不挂载到虚拟机网卡上"""

			params {

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
					name "vipPortStart"
					enclosedIn "params"
					desc "VIP的起始端口号"
					location "body"
					type "Integer"
					optional false
					since "0.6"
				}
				column {
					name "vipPortEnd"
					enclosedIn "params"
					desc "VIP的结束端口号; 如果忽略不设置, 会默认设置为vipPortStart"
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "privatePortStart"
					enclosedIn "params"
					desc "客户IP（虚拟机网卡的IP地址）的起始端口号; 如果忽略不设置, 会默认设置为vipPortStart"
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "privatePortEnd"
					enclosedIn "params"
					desc "客户IP（虚拟机网卡的IP地址）的结束端口号; 如果忽略不设置, 会默认设置为vipPortEnd"
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "protocolType"
					enclosedIn "params"
					desc "网络流量协议类型"
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("TCP","UDP")
				}
				column {
					name "vmNicUuid"
					enclosedIn "params"
					desc "云主机网卡UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "allowedCidr"
					enclosedIn "params"
					desc "源CIDR; 端口转发规则只作用于源CIDR的流量; 如果忽略不设置, 会默认设置为to 0.0.0.0/0"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
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
			clz APICreatePortForwardingRuleEvent.class
		}
	}
}