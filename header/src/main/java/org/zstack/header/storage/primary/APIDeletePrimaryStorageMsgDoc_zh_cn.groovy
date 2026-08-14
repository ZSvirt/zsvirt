package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIDeletePrimaryStorageEvent

doc {
	title "删除主存储(DeletePrimaryStorage)"

	category "storage.primary"

	desc """删除主存储"""

	rest {
		request {
			url "DELETE /v1/primary-storage/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeletePrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "主存储的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive 或者 Enforcing, 默认 Permissive)"
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
			clz APIDeletePrimaryStorageEvent.class
		}
	}
}