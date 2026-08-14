package org.zstack.zops.api

import org.zstack.zops.api.APICheckCephHealthStatusReply

doc {
	title "CheckCephHealthStatus"

	category "zops"

	desc """检查ceph健康状态"""

	rest {
		request {
			url "POST /v1/zops/check-ceph-health"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICheckCephHealthStatusMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.10"
				}
			}
		}

		response {
			clz APICheckCephHealthStatusReply.class
		}
	}
}