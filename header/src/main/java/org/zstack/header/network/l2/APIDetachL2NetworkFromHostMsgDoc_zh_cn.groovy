package org.zstack.header.network.l2

import org.zstack.header.network.l2.APIDetachL2NetworkFromHostEvent

doc {
	title "从物理机上卸载二层网络(DetachL2NetworkFromHost)"

	category "二层网络"

	desc """从物理机上卸载二层网络"""

	rest {
		request {
			url "DELETE /v1/l2-networks/{l2NetworkUuid}/hosts/{hostUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachL2NetworkFromHostMsg.class

			desc """"""

			params {

				column {
					name "l2NetworkUuid"
					enclosedIn ""
					desc "二层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.18.0"
				}
				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.18.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.18.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.18.0"
				}
			}
		}

		response {
			clz APIDetachL2NetworkFromHostEvent.class
		}
	}
}