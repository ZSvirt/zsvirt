package org.zstack.header.volume

import org.zstack.header.volume.APIResizeRootVolumeEvent

doc {
	title "ResizeRootVolume"

	category "mevoco"

	desc """扩展根云盘"""

	rest {
		request {
			url "PUT /v1/volumes/resize/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIResizeRootVolumeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "resizeRootVolume"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "size"
					enclosedIn "resizeRootVolume"
					desc "扩展后大小"
					location "body"
					type "long"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn "resizeRootVolume"
					desc "云主机UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIResizeRootVolumeEvent.class
		}
	}
}