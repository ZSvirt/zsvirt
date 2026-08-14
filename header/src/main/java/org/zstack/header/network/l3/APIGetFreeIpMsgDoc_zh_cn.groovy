package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIGetFreeIpReply

doc {
	title "获取空闲IP(GetFreeIp)"

	category "三层网络"

	desc """获取空闲IP"""

	rest {
		request {
			url "GET /v1/l3-networks/ip/free"
			url "GET /v1/l3-networks/{l3NetworkUuid}/ip/free"
			url "GET /v1/l3-networks/ip-ranges/{ipRangeUuid}/ip/free"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetFreeIpMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID，注意：l3NetworkUuid 和 ipRangeUuid 二选一"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "ipRangeUuid"
					enclosedIn ""
					desc "IP段UUID，注意：l3NetworkUuid 和 ipRangeUuid 二选一"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "start"
					enclosedIn ""
					desc "起始值"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "ipRangeType"
					enclosedIn ""
					desc "地址类型"
					location "query"
					type "String"
					optional true
					since "zsv 3.9"
					values ("Normal","AddressPool")
				}
				column {
					name "ipVersion"
					enclosedIn ""
					desc "IP地址版本号"
					location "query"
					type "Integer"
					optional true
					since "zsv 3.10"
					values ("4","6")
				}
				column {
					name "limit"
					enclosedIn ""
					desc "数量限制"
					location "query"
					type "int"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIGetFreeIpReply.class
		}
	}
}