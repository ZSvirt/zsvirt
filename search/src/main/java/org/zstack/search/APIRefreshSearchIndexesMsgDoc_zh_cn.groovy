package org.zstack.search

import org.zstack.search.APIRefreshSearchIndexesReply

doc {
	title "RefreshSearchIndexes"

	category "search"

	desc """重新生成索引"""

	rest {
		request {
			url "GET /v1/search/indexes/refresh"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRefreshSearchIndexesMsg.class

			desc """"""

			params {

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
			clz APIRefreshSearchIndexesReply.class
		}
	}
}