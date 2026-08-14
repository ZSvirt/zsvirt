package org.zstack.header.cbt

import org.zstack.kvm.VolumeTO

doc {

	title "CBT 任务硬盘信息清单"

	ref {
		name "volume"
		path "org.zstack.header.cbt.VolumeCbtBackupInfo.volume"
		desc "null"
		type "VolumeTO"
		since "4.10.10"
		clz VolumeTO.class
	}
	field {
		name "bitmapBase64"
		desc ""
		type "String"
		since "4.10.10"
	}
	field {
		name "target"
		desc ""
		type "String"
		since "4.10.10"
	}
	field {
		name "scratchNodeName"
		desc ""
		type "String"
		since "4.10.10"
	}
	field {
		name "metadata"
		desc ""
		type "String"
		since "4.10.10"
	}
	field {
		name "nbdPort"
		desc ""
		type "Long"
		since "4.10.10"
	}
	field {
		name "nbdServer"
		desc ""
		type "String"
		since "4.10.10"
	}
	field {
		name "mode"
		desc ""
		type "String"
		since "4.10.10"
	}
	field {
		name "bitmapName"
		desc ""
		type "String"
		since "4.10.10"
	}
}
