package org.zstack.header.network.l3

import org.zstack.header.network.l3.APICheckIpAvailabilityReply

doc {
	title "检查IP可用性(CheckIpAvailability)"

	category "三层网络"

	desc """检查IP可用性"""

	rest {
		request {
			url "GET /v1/l3-networks/{l3NetworkUuid}/ip/{ip}/availability"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICheckIpAvailabilityMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "ip"
					enclosedIn ""
					desc "IP地址"
					location "url"
					type "String"
					optional false
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
				column {
					name "arpCheck"
					enclosedIn ""
					desc "使用arping检测"
					location "query"
					type "Boolean"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "ipRangeCheck"
					enclosedIn ""
					desc "在数据库可用地址段内检测"
					location "query"
					type "Boolean"
					optional true
					since "zsv 4.10.16"
				}
			}
		}

		response {
			clz APICheckIpAvailabilityReply.class
		}
	}
}