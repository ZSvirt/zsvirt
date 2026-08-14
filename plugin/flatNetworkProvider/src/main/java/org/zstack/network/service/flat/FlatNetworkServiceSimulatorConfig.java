package org.zstack.network.service.flat;

import org.zstack.network.service.flat.FlatDhcpBackend.*;
import org.zstack.network.service.flat.FlatEipBackend.ApplyEipCmd;
import org.zstack.network.service.flat.FlatEipBackend.BatchApplyEipCmd;
import org.zstack.network.service.flat.FlatEipBackend.BatchDeleteEipCmd;
import org.zstack.network.service.flat.FlatEipBackend.DeleteEipCmd;
import org.zstack.network.service.flat.FlatUserdataBackend.ApplyUserdataCmd;
import org.zstack.network.service.flat.FlatUserdataBackend.BatchApplyUserdataCmd;
import org.zstack.network.service.flat.FlatUserdataBackend.ReleaseUserdataCmd;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 9/19/2015.
 */
public class FlatNetworkServiceSimulatorConfig {
    public List<ApplyDhcpCmd> applyDhcpCmdList = new ArrayList<ApplyDhcpCmd>();
    public List<ReleaseDhcpCmd> releaseDhcpCmds = new ArrayList<ReleaseDhcpCmd>();
    public List<PrepareDhcpCmd> prepareDhcpCmdList = new ArrayList<PrepareDhcpCmd>();
    public List<ApplyUserdataCmd> applyUserdataCmds = new ArrayList<ApplyUserdataCmd>();
    public List<ReleaseUserdataCmd> releaseUserdataCmds = new ArrayList<ReleaseUserdataCmd>();
    public List<FlatUserdataBackend.CleanupUserdataCmd> cleanupUserdataCmds = new ArrayList<>();
    public List<FlatDhcpBackend.ConnectCmd> connectCmds = new ArrayList<FlatDhcpBackend.ConnectCmd>();
    public List<ApplyEipCmd> applyEipCmds = new ArrayList<ApplyEipCmd>();
    public List<DeleteEipCmd> deleteEipCmds = new ArrayList<DeleteEipCmd>();
    public List<BatchApplyEipCmd> batchApplyEipCmds = new ArrayList<BatchApplyEipCmd>();
    public List<BatchDeleteEipCmd> batchDeleteEipCmds = new ArrayList<BatchDeleteEipCmd>();
    public List<ResetDefaultGatewayCmd> resetDefaultGatewayCmds = new ArrayList<ResetDefaultGatewayCmd>();
    public List<BatchApplyUserdataCmd> batchApplyUserdataCmds = new ArrayList<BatchApplyUserdataCmd>();
    public List<DeleteNamespaceCmd> deleteNamespaceCmds = new ArrayList<>();
    public List<FlushDhcpNamespaceCmd> flushNamespaceCmds = new ArrayList<>();
    public List<ArpingCmd> arpingNamespaceCmds = new ArrayList<>();
}
