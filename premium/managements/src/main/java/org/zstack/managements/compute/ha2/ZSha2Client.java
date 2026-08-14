package org.zstack.managements.compute.ha2;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.externalservice.vops.VOpsAgent;
import org.zstack.externalservice.vops.VOpsClient;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.managements.entity.common.ManagementNodeStatusView;
import org.zstack.managements.entity.ha2.ZSha2StatusView;
import org.zstack.utils.Bash;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.zstack.core.Platform.err;
import static org.zstack.externalservice.vops.VOpsCommands.*;
import static org.zstack.managements.header.ManagementsErrors.*;
import static org.zstack.managements.header.PremiumManagementsConstant.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ZSha2Client {
    private static final CLogger logger = Utils.getLogger(ZSha2Client.class);

    protected final VOpsClient vopsClient = new VOpsAgent().createClient();

    public ZSha2Client withSession(String sessionUuid) {
        vopsClient.withSession(sessionUuid);
        return this;
    }

    public ErrorCode makeSureZSha2ToolsAvailable() {
        final boolean exists = Files.exists(Paths.get(HA2_PATH));
        return exists ? null : err(MISSING_HA2_TOOLS, "zsha2 tools not available");
    }

    public boolean isHa2Installed() {
        return Files.exists(Paths.get(HA2_STATUS_PATH));
    }

    public ErrorableValue<ZSha2StatusView> getStatusInfo() {
        final ErrorCode errorCode = makeSureZSha2ToolsAvailable();
        if (errorCode != null) {
            return ErrorableValue.ofErrorCode(errorCode);
        }

        ErrorableValue<String> outputHolder = statusOutput();
        if (outputHolder.isSuccess()) {
            return parseZSha2StatusJsonInfo(outputHolder.result);
        }

        int code = (int) outputHolder.error.getOpaque().get("bash.code");
        String errorMessage = (String) outputHolder.error.getOpaque().get("bash.error");
        if (code == 1 && errorMessage.contains("HA environment not installed")) {
            return ErrorableValue.ofErrorCode(err(HA2_NOT_INSTALLED, "HA environment not installed"));
        } else if (code == 1 && errorMessage.contains("to use SSH port N other than 22")) {
            logger.trace("can not get remote node status, but status in current node is available");
            String output = (String) outputHolder.error.getOpaque().get("bash.output");
            final ErrorableValue<ZSha2StatusView> viewHolder = parseZSha2StatusJsonInfo(output);
            if (viewHolder.isSuccess()) {
                final ZSha2StatusView view = viewHolder.result;
                ManagementNodeStatusView remoteView = new ManagementNodeStatusView();
                remoteView.setError(err(HA2_STATUS_GET_ERROR, "failed to get status on peer status"));
                view.getNodes().add(remoteView);
                return ErrorableValue.of(view);
            }
            return viewHolder;
        }

        return ErrorableValue.ofErrorCode(outputHolder.error);
    }

    protected ErrorableValue<String> statusOutput() {
        String[] outputs = new String[1];
        String[] errors = new String[1];
        int[] codes = new int[1];
        new Bash() {
            @Override
            protected void scripts() {
                codes[0] = sudoRunScripts(HA2_PATH, "status");
                errors[0] = stderr().trim();
                outputs[0] = stdout().trim();
            }
        }.execute();

        if (codes[0] != 0) {
            return ErrorableValue.ofErrorCode(err(HA2_STATUS_GET_ERROR, "failed to get zsha2 status")
                    .withOpaque("bash.code", codes[0])
                    .withOpaque("bash.output", outputs[0])
                    .withOpaque("bash.error", errors[0]));
        }

        return ErrorableValue.of(outputs[0] + "\n" + errors[0]);
    }

    private static ErrorableValue<ZSha2StatusView> parseZSha2StatusJsonInfo(String output) {
        final String[] lines = output.split("\n");
        ZSha2StatusView view = new ZSha2StatusView();
        view.setNodes(new ArrayList<>());
        ManagementNodeStatusView nodeView = null;

        try {
            for (String line : lines) {
                line = line.replaceAll("\\u001b\\[[;\\d]*m", "");
                if (line.startsWith("Status report from ")) {
                    String ip = line.substring("Status report from ".length());
                    nodeView = new ManagementNodeStatusView();
                    nodeView.setIp(ip);
                    view.getNodes().add(nodeView);
                    continue;
                }

                if (nodeView == null) {
                    continue;
                }

                if (line.startsWith("Owns virtual address: ")) {
                    String[] scripts = line.split(":");
                    if (scripts.length == 2) {
                        nodeView.setOwnsVip("yes".equalsIgnoreCase(scripts[1].trim()));
                    }
                } else if (line.startsWith("Gateway ")) {
                    int reachableIndex = line.indexOf("reachable: ");
                    if (reachableIndex <= 0) {
                        continue;
                    }
                    String gatewayIp = line.substring("Gateway ".length(), reachableIndex).trim();
                    nodeView.setGatewayIp(gatewayIp);
                    String result = line.substring("reachable: ".length() + reachableIndex).trim();
                    nodeView.setGatewayReachable("yes".equalsIgnoreCase(result));
                } else if (line.startsWith("VIP ")) {
                    int reachableIndex = line.indexOf("reachable: ");
                    if (reachableIndex <= 0) {
                        continue;
                    }
                    String vip = line.substring("VIP ".length(), reachableIndex).trim();
                    view.setVip(vip);
                    String result = line.substring("reachable: ".length() + reachableIndex).trim();
                    nodeView.setVipReachable("yes".equalsIgnoreCase(result));
                } else if (line.startsWith("Peer ")) {
                    String[] scripts = line.split(":");
                    if (scripts.length == 2) {
                        nodeView.setPeerReachable("yes".equalsIgnoreCase(scripts[1].trim()));
                    }
                } else if (line.startsWith("Keepalived status:")) {
                    String[] scripts = line.split(":");
                    if (scripts.length == 2) {
                        nodeView.setKeepalivedStatus(scripts[1].trim().toLowerCase());
                    }
                } else if (line.startsWith("ZStack HA Monitor:")) {
                    String[] scripts = line.split(":");
                    if (scripts.length == 2) {
                        nodeView.setHaMonitorStatus(scripts[1].trim().toLowerCase());
                    }
                } else if (line.startsWith("MySQL status:")) {
                    String[] scripts = line.split(":");
                    if (scripts.length == 2) {
                        nodeView.setDatabaseStatus(scripts[1].trim().toLowerCase());
                    }
                } else if (line.startsWith("MN status:")) {
                    String[] scripts = line.split(":");
                    if (scripts.length < 2) { // scripts[1] = " Running [PID", we need "Running"
                        continue;
                    }

                    String[] innerScripts = scripts[1].split("\\[");
                    nodeView.setManagementsNodeStatus(innerScripts[0].trim().toLowerCase());
                } else if (line.startsWith("UI status:")) {
                    String[] scripts = line.split(":");
                    if (scripts.length < 2) { // scripts[1] = " Running [PID", we need "Running"
                        continue;
                    }

                    String[] innerScripts = scripts[1].split("\\[");
                    nodeView.setUiStatus(innerScripts[0].trim().toLowerCase());
                } else if (line.startsWith("Note: visit ZStack UI with")) {
                    view.setUiHttpPath(line.substring("Note: visit ZStack UI with ".length()).trim());
                } else {
                    String trim = line.trim();
                    if (trim.startsWith("Slave_IO_Running:")) {
                        String[] scripts = line.split(":");
                        if (scripts.length == 2) {
                            nodeView.setSlaveIoRunning("yes".equalsIgnoreCase(scripts[1].trim()));
                        }
                    } else if (trim.startsWith("Slave_SQL_Running:")) {
                        String[] scripts = line.split(":");
                        if (scripts.length == 2) {
                            nodeView.setSlaveSqlRunning("yes".equalsIgnoreCase(scripts[1].trim()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.trace("failed to parse zsha2 status: " + e.getMessage() + "\n" + output);
            return ErrorableValue.ofErrorCode(err(HA2_STATUS_PARSE_ERROR, "failed to parse zsha2 status")
                    .withOpaque("exception", e.getMessage()));
        }

        return ErrorableValue.of(view);
    }

    public ErrorCode demote() {
        final ErrorableValue<JsonObject> response = vopsClient.createHttp(ZSHA2_DEMOTE_PATH)
                .withBodyJson(new ZSha2DemoteCmd())
                .putWithErrorCode();
        return response.error;
    }
}
