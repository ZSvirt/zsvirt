package org.zstack.header.volume

import org.zstack.header.volume.APIShrinkVolumeEvent

doc {
	title "ShrinkVolume"

	category "volume"

	desc """离线收缩卷在主存上的物理空间，不改变卷的虚拟容量。卷必须处于 Ready 状态；根盘和已挂载数据盘所属的虚拟机必须处于 Stopped 状态，未挂载数据盘可直接收缩"""

	rest {
		request {
			url "PUT /v1/volumes/shrink/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIShrinkVolumeMsg.class

			desc """离线收缩卷在主存上的物理空间，不改变卷的虚拟容量"""

			params {

				column {
					name "uuid"
					enclosedIn "shrinkVolume"
					desc "卷UUID"
					location "url"
					type "String"
					optional false
					since "5.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "5.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "5.2.0"
				}
			}
		}

		response {
			clz APIShrinkVolumeEvent.class
		}
	}
}