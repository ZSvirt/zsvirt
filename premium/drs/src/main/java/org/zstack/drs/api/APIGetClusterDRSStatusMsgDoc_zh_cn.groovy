package org.zstack.drs.api

import org.zstack.drs.api.APIGetClusterDRSStatusReply

doc {
	title "GetClusterDRSStatus"

	category "drs"

	desc """获取集群DRS详情"""

	rest {
		request {
			url "GET /v1/clusters/drs/status"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetClusterDRSStatusMsg.class

			desc """"""

			params {

				column {
					name "drsUuid"
					enclosedIn ""
					desc "集群DRS的UUID"
					location "query"
					type "String"
					optional false
					since "4.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.0.0"
				}
			}
		}

		response {
			clz APIGetClusterDRSStatusReply.class
		}
	}
}