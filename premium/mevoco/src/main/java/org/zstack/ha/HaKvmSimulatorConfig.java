package org.zstack.ha;

import org.zstack.ha.HaKvmHostSiblingChecker.ScanCmd;
import org.zstack.ha.SelfFencerKvmBackend.CancelSelfFencerCmd;
import org.zstack.ha.SelfFencerKvmBackend.SetupSelfFencerCmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2016/3/29.
 */
public class HaKvmSimulatorConfig {
    public List<ScanCmd> scanCmds = new ArrayList<ScanCmd>();
    public Map<String, Boolean> scanSuccess = new HashMap<String, Boolean>();
    public volatile String scanResult = HaKvmHostSiblingChecker.RET_SUCCESS;
    public List<SetupSelfFencerCmd> setupSelfFencerCmds = new ArrayList<SetupSelfFencerCmd>();
    public List<CancelSelfFencerCmd> cancelSelfFencerCmds = new ArrayList<>();
}
