package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIDeleteIpRangeEvent

doc {
	title "删除IP地址范围(DeleteIpRange)"

	category "三层网络"

	desc """删除IP地址范围"""

	rest {
		request {
			url "DELETE /v1/l3-networks/ip-ranges/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteIpRangeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
					location "query"
					type "String"
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
			clz APIDeleteIpRangeEvent.class
		}
	}
}