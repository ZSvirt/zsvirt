package org.zstack.header.volume.block

import org.zstack.header.volume.block.APIUpdateBlockVolumeEvent

doc {
	title "UpdateXskyBlockVolume"

	category "mevoco"

	desc """更新xsky类型的块存储卷"""

	rest {
		request {
			url "PUT /v1/xsky/block-volumes/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateXskyBlockVolumeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateXskyBlockVolume"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "name"
					enclosedIn "updateXskyBlockVolume"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.17.11"
				}
				column {
					name "description"
					enclosedIn "updateXskyBlockVolume"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.17.11"
				}
				column {
					name "burstTotalBw"
					enclosedIn "updateXskyBlockVolume"
					desc ""
					location "body"
					type "Long"
					optional true
					since "3.17.11"
				}
				column {
					name "burstTotalIops"
					enclosedIn "updateXskyBlockVolume"
					desc ""
					location "body"
					type "Long"
					optional true
					since "3.17.11"
				}
				column {
					name "maxTotalBw"
					enclosedIn "updateXskyBlockVolume"
					desc ""
					location "body"
					type "Long"
					optional true
					since "3.17.11"
				}
				column {
					name "maxTotalIops"
					enclosedIn "updateXskyBlockVolume"
					desc ""
					location "body"
					type "Long"
					optional true
					since "3.17.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
			}
		}

		response {
			clz APIUpdateBlockVolumeEvent.class
		}
	}
}