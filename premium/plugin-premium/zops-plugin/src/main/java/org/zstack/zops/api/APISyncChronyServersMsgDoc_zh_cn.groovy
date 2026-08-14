package org.zstack.zops.api

import org.zstack.zops.api.APISyncChronyServersEvent

doc {
	title "SyncChronyServers"

	category "zops"

	desc """同步chrony时间源返回"""

	rest {
		request {
			url "PUT /v1/zops/chrony/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISyncChronyServersMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
			}
		}

		response {
			clz APISyncChronyServersEvent.class
		}
	}
}