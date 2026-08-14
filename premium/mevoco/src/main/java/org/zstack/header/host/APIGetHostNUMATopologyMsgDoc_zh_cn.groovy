package org.zstack.header.host

import org.zstack.header.host.APIGetHostNUMATopologyEvent

doc {
	title "获取物理机NUMA拓扑"

	category "host"

	desc """null"""

	rest {
		request {
			url "POST /v1/hosts/{uuid}/numa"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetHostNUMATopologyMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "url"
					type "String"
					optional false
					since "3.13.12"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.13.12"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.13.12"
				}
			}
		}

		response {
			clz APIGetHostNUMATopologyEvent.class
		}
	}
}