package org.zstack.drs.api

import org.zstack.drs.api.APIDeleteClusterDRSEvent

doc {
	title "DeleteClusterDRS"

	category "drs"

	desc """删除集群DRS"""

	rest {
		request {
			url "DELETE /v1/clusters/drs/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteClusterDRSMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "4.0.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
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
			clz APIDeleteClusterDRSEvent.class
		}
	}
}