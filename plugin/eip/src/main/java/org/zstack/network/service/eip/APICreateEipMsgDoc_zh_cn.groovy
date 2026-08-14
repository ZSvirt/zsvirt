package org.zstack.network.service.eip

import org.zstack.network.service.eip.APICreateEipEvent

doc {
	title "创建弹性IP(CreateEip)"

	category "弹性IP"

	desc """创建弹性IP"""

	rest {
		request {
			url "POST /v1/eips"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateEipMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "弹性IP名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "弹性IP的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
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
					name "vmNicUuid"
					enclosedIn "params"
					desc "云主机网卡UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "usedIpUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.1"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，云主机会使用该字段值作为UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "EIP的系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "EIP的用户标签"
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
			clz APICreateEipEvent.class
		}
	}
}