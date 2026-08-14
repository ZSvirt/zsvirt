package org.zstack.zsv.telemetry.entity



doc {

	title "Telemetry 检查更新结果"

	field {
		name "version"
		desc "云端返回的版本号"
		type "String"
		since "zsv 5.1.0"
	}
	field {
		name "releaseNotesZh"
		desc "更新内容（中文）"
		type "String"
		since "zsv 5.1.0"
	}
	field {
		name "releaseNotesEn"
		desc "更新内容（英文）"
		type "String"
		since "zsv 5.1.0"
	}
	field {
		name "currentVersion"
		desc "本端当前产品版本（与 GetVersion 同源）"
		type "String"
		since "zsv 5.1.0"
	}
}
