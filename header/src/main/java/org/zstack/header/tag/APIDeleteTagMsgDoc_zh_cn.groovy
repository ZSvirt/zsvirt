package org.zstack.header.tag

import org.zstack.header.tag.APIDeleteTagEvent

doc {
	title "DeleteTag"

	category "tag"

	desc """删除标签"""

	rest {
		request {
			url "DELETE /v1/tags/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteTagMsg.class

			desc """删除标签"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "当设置成Permissive时, 如果删除过程中发生错误或者删除不被允许ZStack会停止删除操作; 在这种情况下, 包含失败原因的错误代码会被返回.当设置成Enforcing, ZStack会忽略所有错误和权限而直接删除资源; 在这种情况下, 删除操作总是会成功."
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIDeleteTagEvent.class
		}
	}
}