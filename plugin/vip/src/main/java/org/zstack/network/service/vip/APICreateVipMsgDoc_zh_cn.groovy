package org.zstack.network.service.vip

import org.zstack.network.service.vip.APICreateVipEvent

doc {
	title "CreateVip"

	category "vip"

	desc """创建VIP"""

	rest {
		request {
			url "POST /v1/vips"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVipMsg.class

			desc """创建VIP"""

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
					optional false
					since "0.6"
				}
				column {
					name "ipRangeUuid"
					enclosedIn "params"
					desc "IP段UUID"
					location "body"
					type "String"
					optional true
					since "3.9"
				}
				column {
					name "allocatorStrategy"
					enclosedIn "params"
					desc "分配策略"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "requiredIp"
					enclosedIn "params"
					desc "请求的IP"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
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
			clz APICreateVipEvent.class
		}
	}
}