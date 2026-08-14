package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APICleanUpImageCacheOnPrimaryStorageEvent

doc {
	title "清楚主存储镜像缓存(CleanUpImageCacheOnPrimaryStorage)"

	category "storage.primary"

	desc """尝试从主存储清除镜像缓存"""

	rest {
		request {
			url "PUT /v1/primary-storage/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICleanUpImageCacheOnPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "cleanUpImageCacheOnPrimaryStorage"
					desc "主存储的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "force"
					enclosedIn "cleanUpImageCacheOnPrimaryStorage"
					desc "是否强制删除"
					location "body"
					type "boolean"
					optional true
					since "zsv 3.10.0"
				}
			}
		}

		response {
			clz APICleanUpImageCacheOnPrimaryStorageEvent.class
		}
	}
}