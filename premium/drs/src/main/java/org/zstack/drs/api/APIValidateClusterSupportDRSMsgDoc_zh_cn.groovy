package org.zstack.drs.api

import org.zstack.drs.api.APIValidateClusterSupportDRSReply

doc {
	title "ValidateClusterSupportDRS"

	category "drs"

	desc """查看集群是否支持DRS"""

	rest {
		request {
			url "GET /v1/clusters/{clusterUuid}/drs/valid"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIValidateClusterSupportDRSMsg.class

			desc """"""

			params {

				column {
					name "clusterUuid"
					enclosedIn ""
					desc "集群UUID，不是集群DRS的UUID"
					location "url"
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
			clz APIValidateClusterSupportDRSReply.class
		}
	}
}