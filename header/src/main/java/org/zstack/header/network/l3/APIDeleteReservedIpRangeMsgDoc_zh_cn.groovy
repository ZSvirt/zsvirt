package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIDeleteReservedIpRangeEvent

doc {
	title "DeleteReservedIpRange"

	category "network.l3"

	desc """删除保留地址段"""

	rest {
		request {
			url "DELETE /v1/l3-networks/reserved-ip-ranges/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteReservedIpRangeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.16"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
			}
		}

		response {
			clz APIDeleteReservedIpRangeEvent.class
		}
	}
}