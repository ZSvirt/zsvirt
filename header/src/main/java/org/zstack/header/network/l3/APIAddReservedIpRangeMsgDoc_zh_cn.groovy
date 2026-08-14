package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIAddReservedIpRangeEvent

doc {
	title "AddReservedIpRange"

	category "network.l3"

	desc """添加保留地址段"""

	rest {
		request {
			url "POST /v1/l3-networks/{l3NetworkUuid}/reserved-ip-ranges"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddReservedIpRangeMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.16"
				}
				column {
					name "startIp"
					enclosedIn "params"
					desc "开始IP(包含在地址段内)"
					location "body"
					type "String"
					optional false
					since "zsv 4.10.16"
				}
				column {
					name "endIp"
					enclosedIn "params"
					desc "结束IP(包含在地址段内)"
					location "body"
					type "String"
					optional false
					since "zsv 4.10.16"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
			}
		}

		response {
			clz APIAddReservedIpRangeEvent.class
		}
	}
}