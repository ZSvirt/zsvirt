package org.zstack.storage.primary.sharedblock

import org.zstack.storage.primary.sharedblock.APIExtendSharedBlockVolumeEvent

doc {
	title "ExtendSharedBlockVolume"

	category "storage.primary"

	desc """离线扩容共享块存储卷的逻辑卷，不改变卷内 qcow2 的虚拟容量。所有卷必须处于 Ready 状态；根盘和已挂载数据盘所属的虚拟机必须处于 Stopped 状态，未挂载数据盘可直接扩容"""

	rest {
		request {
			url "PUT /v1/primary-storage/sharedblockgroup/volumes/{volumeUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIExtendSharedBlockVolumeMsg.class

			desc """离线扩容共享块存储卷的逻辑卷，不改变卷内 qcow2 的虚拟容量。所有卷必须处于 Ready 状态；根盘和已挂载数据盘所属的虚拟机必须处于 Stopped 状态，未挂载数据盘可直接扩容"""

			params {

				column {
					name "volumeUuid"
					enclosedIn "extendSharedBlockVolume"
					desc "卷UUID"
					location "url"
					type "String"
					optional false
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "size"
					enclosedIn "extendSharedBlockVolume"
					desc "扩展后的目标大小，单位为字节，不含逻辑卷元数据预留空间，不能超过卷的虚拟容量"
					location "body"
					type "long"
					optional false
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
			}
		}

		response {
			clz APIExtendSharedBlockVolumeEvent.class
		}
	}
}