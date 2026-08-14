package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIGetZWatchAlertHistogramReply

doc {
	title "GetZWatchAlertHistogram"

	category "zwatch"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/zwatch/alert-histories/histogram"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetZWatchAlertHistogramMsg.class

			desc """"""

			params {

				column {
					name "tableName"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "startTime"
					enclosedIn ""
					desc ""
					location "query"
					type "Long"
					optional false
					since "0.6"
				}
				column {
					name "endTime"
					enclosedIn ""
					desc ""
					location "query"
					type "Long"
					optional false
					since "0.6"
				}
				column {
					name "intervalHours"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional false
					since "0.6"
				}
				column {
					name "groupColumns"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetZWatchAlertHistogramReply.class
		}
	}
}