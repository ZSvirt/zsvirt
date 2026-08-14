package org.zstack.guesttools



doc {

	title "虚拟机 GuestTools QGA 状态信息"

	field {
		name "NotInstalled"
		desc "未安装"
		type "GuestToolsQgaState"
		since "3.16"
	}
	field {
		name "Running"
		desc "运行中"
		type "GuestToolsQgaState"
		since "3.16"
	}
	field {
		name "NotRunning"
		desc "停止"
		type "GuestToolsQgaState"
		since "3.16"
	}
	field {
		name "NotUpgraded"
		desc "待升级"
		type "GuestToolsQgaState"
		since "3.16"
	}
}
