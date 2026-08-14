package org.zstack.ipsec.vyos;

import org.zstack.ipsec.vyos.VyosIPsecBackend.CreateIPsecConnectionCmd;
import org.zstack.ipsec.vyos.VyosIPsecBackend.DeleteIPsecConnectionCmd;
import org.zstack.ipsec.vyos.VyosIPsecBackend.SyncIPsecConnectionCmd;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing5 on 2016/11/8.
 */
public class VyosIPsecSimulatorConfig {
    public volatile boolean createIPsecConnectionSuccess = true;
    public List<CreateIPsecConnectionCmd> createIPsecConnectionCmdList = new ArrayList<>();
    public List<DeleteIPsecConnectionCmd> deleteIPsecConnectionCmds = new ArrayList<>();
    public List<SyncIPsecConnectionCmd> syncIPsecConnectionCmds = new ArrayList<>();
}
