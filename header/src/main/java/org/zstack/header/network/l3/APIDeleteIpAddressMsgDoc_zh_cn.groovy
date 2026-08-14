package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIDeleteIpAddressEvent

doc {
	title "DeleteIpAddress"

	category "network.l3"

	desc """删除IP地址"""

	rest {
		request {
			url "DELETE /v1/l3-networks/{l3NetworkUuid}/ip-address"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteIpAddressMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.16"
				}
				column {
					name "usedIpUuids"
					enclosedIn ""
					desc "被删除地址Uuid"
					location "query"
					type "List"
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
			clz APIDeleteIpAddressEvent.class
		}
	}
}