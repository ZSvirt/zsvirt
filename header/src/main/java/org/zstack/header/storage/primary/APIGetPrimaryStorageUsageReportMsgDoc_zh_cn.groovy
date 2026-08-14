package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIGetPrimaryStorageUsageReportReply

doc {
	title "GetPrimaryStorageUsageReport"

	category "storage.primary"

	desc """获取主存储预测容量报告"""

	rest {
		request {
			url "GET /v1/primary-storage/{primaryStorageUuid}/usage/report"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetPrimaryStorageUsageReportMsg.class

			desc """"""

			params {

				column {
					name "primaryStorageUuid"
					enclosedIn ""
					desc "主存储UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.17.21"
				}
				column {
					name "uris"
					enclosedIn ""
					desc "存储协议"
					location "query"
					type "List"
					optional true
					since "zsv 3.17.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.17.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.17.21"
				}
			}
		}

		response {
			clz APIGetPrimaryStorageUsageReportReply.class
		}
	}
}