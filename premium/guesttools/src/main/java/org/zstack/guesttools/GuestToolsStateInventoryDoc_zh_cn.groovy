package org.zstack.guesttools

import org.zstack.guesttools.GuestToolsQgaState
import org.zstack.guesttools.GuestToolsZWatchState
import java.sql.Timestamp

doc {

	title "虚拟机GuestTools状态清单"

	field {
		name "vmInstanceUuid"
		desc "虚拟机 UUID"
		type "String"
		since "3.16"
	}
	ref {
		name "qgaState"
		path "org.zstack.guesttools.GuestToolsStateInventory.qgaState"
		desc "虚拟机 GuestTools QGA 状态"
		type "GuestToolsQgaState"
		since "3.16"
		clz GuestToolsQgaState.class
	}
	ref {
		name "zwatchState"
		path "org.zstack.guesttools.GuestToolsStateInventory.zwatchState"
		desc "虚拟机 GuestTools ZWatch 组件状态"
		type "GuestToolsZWatchState"
		since "3.16"
		clz GuestToolsZWatchState.class
	}
	field {
		name "version"
		desc "虚拟机 GuestTools 版本"
		type "String"
		since "3.16"
	}
	field {
		name "platform"
		desc "虚拟机操作系统平台类型"
		type "String"
		since "3.16"
	}
	field {
		name "osType"
		desc "虚拟机操作系统类型"
		type "String"
		since "3.16"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.16"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.16"
	}
}
