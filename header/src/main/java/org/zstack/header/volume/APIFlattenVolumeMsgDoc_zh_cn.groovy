package org.zstack.header.volume

import org.zstack.header.volume.APIFlattenVolumeEvent

doc {
	title "FlattenVolume"

	category "volume"

	desc """扁平合并云盘"""

	rest {
		request {
			url "PUT /v1/volumes/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIFlattenVolumeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "flattenVolume"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.17.0"
				}
				column {
					name "dryRun"
					enclosedIn "flattenVolume"
					desc "试运行，可用于预测数据用量"
					location "body"
					type "boolean"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
			}
		}

		response {
			clz APIFlattenVolumeEvent.class
		}
	}
}