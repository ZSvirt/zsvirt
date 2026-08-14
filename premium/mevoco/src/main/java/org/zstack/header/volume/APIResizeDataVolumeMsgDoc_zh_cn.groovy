package org.zstack.header.volume

import org.zstack.header.volume.APIResizeDataVolumeEvent

doc {
	title "ResizeDataVolume"

	category "mevoco"

	desc """扩展数据云盘"""

	rest {
		request {
			url "PUT /v1/volumes/data/resize/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIResizeDataVolumeMsg.class

			desc """扩展数据云盘"""

			params {

				column {
					name "uuid"
					enclosedIn "resizeDataVolume"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "size"
					enclosedIn "resizeDataVolume"
					desc "扩展后大小"
					location "body"
					type "long"
					optional false
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.2"
				}
			}
		}

		response {
			clz APIResizeDataVolumeEvent.class
		}
	}
}