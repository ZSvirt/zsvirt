package org.zstack.zsv.telemetry;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zsv.telemetry.api.APICheckTelemetryUpdateEvent;
import org.zstack.zsv.telemetry.api.APICheckTelemetryUpdateMsg;
import org.zstack.zsv.telemetry.api.APIGetTelemetryConsentMsg;
import org.zstack.zsv.telemetry.api.APIGetTelemetryConsentReply;
import org.zstack.zsv.telemetry.api.APIGetTelemetrySettingMsg;
import org.zstack.zsv.telemetry.api.APIGetTelemetrySettingReply;
import org.zstack.zsv.telemetry.api.APIUpdateTelemetryConsentEvent;
import org.zstack.zsv.telemetry.api.APIUpdateTelemetryConsentMsg;
import org.zstack.zsv.telemetry.client.TelemetryHttpClient;
import org.zstack.zsv.telemetry.client.TelemetryLocalClient;
import org.zstack.zsv.telemetry.collect.TelemetryCheckUpdateReport;
import org.zstack.zsv.telemetry.collect.TelemetryCollector;
import org.zstack.zsv.telemetry.collect.TelemetryDailyReport;
import org.zstack.zsv.telemetry.entity.TelemetryConsentView;
import org.zstack.zsv.telemetry.entity.TelemetrySettingView;
import org.zstack.zsv.telemetry.entity.TelemetryUpdateInfoView;
import org.zstack.zsv.telemetry.header.TelemetryDeletePendingMsg;
import org.zstack.zsv.telemetry.header.TelemetryDeletePendingReply;
import org.zstack.zsv.telemetry.header.TelemetryGlobalConfig;
import org.zstack.zsv.telemetry.header.TelemetryRunCollectMsg;
import org.zstack.zsv.telemetry.header.TelemetryRunCollectReply;
import org.zstack.zsv.telemetry.header.TelemetryRunUploadMsg;
import org.zstack.zsv.telemetry.header.TelemetryRunUploadReply;
import org.zstack.zsv.telemetry.header.TelemetrySyncReportMsg;
import org.zstack.zsv.telemetry.header.TelemetrySyncReportReply;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class TelemetryManager extends AbstractService {
    private static final CLogger logger = Utils.getLogger(TelemetryManager.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private TimeHelper timeHelper;
    @Autowired
    private TelemetryCollector telemetryCollector;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetTelemetryConsentMsg) {
            handle((APIGetTelemetryConsentMsg) msg);
        } else if (msg instanceof APIUpdateTelemetryConsentMsg) {
            handle((APIUpdateTelemetryConsentMsg) msg);
        } else if (msg instanceof APIGetTelemetrySettingMsg) {
            handle((APIGetTelemetrySettingMsg) msg);
        } else if (msg instanceof APICheckTelemetryUpdateMsg) {
            handle((APICheckTelemetryUpdateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof TelemetryRunCollectMsg) {
            handle((TelemetryRunCollectMsg) msg);
        } else if (msg instanceof TelemetryRunUploadMsg) {
            handle((TelemetryRunUploadMsg) msg);
        } else if (msg instanceof TelemetrySyncReportMsg) {
            handle((TelemetrySyncReportMsg) msg);
        } else if (msg instanceof TelemetryDeletePendingMsg) {
            handle((TelemetryDeletePendingMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetTelemetryConsentMsg msg) {
        APIGetTelemetryConsentReply reply = new APIGetTelemetryConsentReply();
        reply.setInventory(buildConsentView());
        bus.reply(msg, reply);
    }

    private void handle(APIGetTelemetrySettingMsg msg) {
        APIGetTelemetrySettingReply reply = new APIGetTelemetrySettingReply();
        reply.setInventory(buildSettingView());
        bus.reply(msg, reply);
    }

    private void handle(APIUpdateTelemetryConsentMsg msg) {
        if (TelemetryConstant.CONSENT_ACTION_ENABLED.equals(msg.getAction())) {
            long currentTimeMillis = timeHelper.getCurrentTimeMillis();
            String timeText = Instant.ofEpochMilli(currentTimeMillis).truncatedTo(ChronoUnit.SECONDS).toString();
            TelemetryGlobalConfig.CONSENT_GRANTED_AT.updateValue(timeText);
        } else {
            TelemetryGlobalConfig.CONSENT_GRANTED_AT.updateValue(TelemetryConstant.CONSENT_NOT_GRANTED);
        }

        APIUpdateTelemetryConsentEvent event = new APIUpdateTelemetryConsentEvent(msg.getId());
        event.setInventory(buildConsentView());
        bus.publish(event);
    }

    /**
     * Check update: no consent gate; does not write reports/pending.
     * Order: health → collect mini report → POST /v1/updates/check → Event.
     */
    private void handle(APICheckTelemetryUpdateMsg msg) {
        APICheckTelemetryUpdateEvent event = new APICheckTelemetryUpdateEvent(msg.getId());
        try {
            TelemetryHttpClient httpClient = Platform.New(TelemetryHttpClient::new);
            ErrorableValue<String> health = httpClient.healthCheck();
            if (!health.isSuccess()) {
                logger.warn(String.format("check-update health failed: %s", health.error));
                event.setError(Platform.err(
                        TelemetryErrors.TELEMETRY_CLOUD_UNREACHABLE,
                        "telemetry cloud health check failed: %s",
                        health.error == null ? "unknown" : health.error.getDetails()));
                bus.publish(event);
                return;
            }

            TelemetryCheckUpdateReport report = telemetryCollector.collectCheckUpdateReport();
            logger.info(String.format(
                    "check-update start: source_id=%s version=%s request_type=%s mn_ids=%d host_ids=%d",
                    report.sourceId, report.version, report.requestType,
                    report.mnIds == null ? 0 : report.mnIds.size(),
                    report.hostIds == null ? 0 : report.hostIds.size()));

            ErrorableValue<TelemetryUpdateInfoView> result = httpClient.checkUpdate(report);
            if (!result.isSuccess()) {
                logger.warn(String.format("check-update cloud call failed: %s", result.error));
                event.setError(result.error != null ? result.error : Platform.err(
                        TelemetryErrors.TELEMETRY_UPLOAD_FAILED, "telemetry check-update failed"));
                bus.publish(event);
                return;
            }

            TelemetryUpdateInfoView inventory = result.result;
            inventory.setCurrentVersion(report.version);
            event.setInventory(inventory);
        } catch (Throwable t) {
            logger.warn(String.format("check-update failed: %s", t.getMessage()), t);
            if (t instanceof OperationFailureException) {
                event.setError(((OperationFailureException) t).getErrorCode());
            } else {
                event.setError(Platform.err(
                        TelemetryErrors.TELEMETRY_COLLECT_FAILED,
                        "telemetry check-update failed: %s", t.getMessage()));
            }
        }
        bus.publish(event);
    }

    private void handle(TelemetryRunCollectMsg msg) {
        TelemetryRunCollectReply reply = new TelemetryRunCollectReply();
        try {
            if (!TelemetryUtils.isConsentGranted()) {
                bus.reply(msg, reply);
                return;
            }
            TelemetryDailyReport report = telemetryCollector.collect();
            // Re-check: consent may be revoked while collect() was running.
            if (!TelemetryUtils.isConsentGranted()) {
                logger.debug("consent revoked during collect, skip persist/sync");
                bus.reply(msg, reply);
                return;
            }
            TelemetryLocalClient localClient = Platform.New(TelemetryLocalClient::new);
            String reportJson = localClient.saveReport(report);
            syncReportToPeers(report.snapshotDate, localClient.reportFileName(report.snapshotDate), reportJson);
        } catch (Throwable t) {
            logger.warn(String.format("run collect failed: %s", t.getMessage()), t);
            reply.setError(Platform.err(
                    TelemetryErrors.TELEMETRY_COLLECT_FAILED,
                    "telemetry collect failed: %s", t.getMessage()));
        }
        bus.reply(msg, reply);
    }

    private void handle(TelemetryRunUploadMsg msg) {
        TelemetryRunUploadReply reply = new TelemetryRunUploadReply();
        try {
            uploadPendingReports();
        } catch (Throwable t) {
            logger.warn(String.format("run upload failed: %s", t.getMessage()), t);
            reply.setError(Platform.err(
                    TelemetryErrors.TELEMETRY_UPLOAD_FAILED,
                    "telemetry upload failed: %s", t.getMessage()));
        }
        bus.reply(msg, reply);
    }

    private void handle(TelemetrySyncReportMsg msg) {
        TelemetrySyncReportReply reply = new TelemetrySyncReportReply();
        try {
            if (!TelemetryUtils.isConsentGranted()) {
                logger.debug("consent not granted, skip sync report write");
                bus.reply(msg, reply);
                return;
            }
            TelemetryLocalClient client = Platform.New(TelemetryLocalClient::new);
            client.writeSyncedReport(msg.getSnapshotDate(), msg.getFileName(), msg.getReportJson());
        } catch (Throwable t) {
            logger.warn(String.format("sync report write failed: %s", t.getMessage()), t);
            reply.setError(Platform.err(
                    TelemetryErrors.TELEMETRY_COLLECT_FAILED,
                    "telemetry sync report failed: %s", t.getMessage()));
        }
        bus.reply(msg, reply);
    }

    private void uploadPendingReports() {
        if (!TelemetryUtils.isConsentGranted()) {
            return;
        }

        TelemetryLocalClient localClient = Platform.New(TelemetryLocalClient::new);
        TelemetryHttpClient httpClient = Platform.New(TelemetryHttpClient::new);

        ErrorableValue<String> health = httpClient.healthCheck();
        if (!health.isSuccess()) {
            logger.warn(String.format("cloud health check failed: %s", health.error));
            return;
        }

        for (File file : localClient.listPendingReports()) {
            if (!TelemetryUtils.isConsentGranted()) {
                logger.debug("consent revoked during upload, stop remaining pending");
                return;
            }
            uploadOne(localClient, httpClient, file);
        }
    }

    private void uploadOne(TelemetryLocalClient localClient, TelemetryHttpClient httpClient, File file) {
        try {
            String json = localClient.readFile(file);
            ErrorableValue<String> result = httpClient.uploadReport(json);
            if (!result.isSuccess()) {
                logger.warn(String.format("upload failed for %s: %s", file.getName(), result.error));
                return;
            }

            localClient.deletePending(file);
            notifyPeersDeletePending(file.getName());
            logger.debug(String.format("uploaded pending report %s", file.getName()));
        } catch (Throwable t) {
            logger.warn(String.format("upload error for %s: %s", file.getName(), t.getMessage()), t);
        }
    }

    private void handle(TelemetryDeletePendingMsg msg) {
        TelemetryDeletePendingReply reply = new TelemetryDeletePendingReply();
        try {
            TelemetryLocalClient client = Platform.New(TelemetryLocalClient::new);
            client.deletePendingByFileName(msg.getFileName());
            logger.debug(String.format("deleted pending report by peer notify: %s", msg.getFileName()));
        } catch (Throwable t) {
            logger.warn(String.format("delete pending by peer notify failed: %s", t.getMessage()), t);
            reply.setError(Platform.err(
                    TelemetryErrors.TELEMETRY_UPLOAD_FAILED,
                    "telemetry delete pending failed: %s", t.getMessage()));
        }
        bus.reply(msg, reply);
    }

    private void notifyPeersDeletePending(String fileName) {
        String self = Platform.getManagementServerId();
        for (String peerUuid : TelemetryUtils.listManagementNodeUuids()) {
            if (Objects.equals(self, peerUuid)) {
                continue;
            }

            TelemetryDeletePendingMsg msg = new TelemetryDeletePendingMsg();
            msg.setFileName(fileName);
            bus.makeServiceIdByManagementNodeId(msg, TelemetryConstant.SERVICE_ID, peerUuid);
            bus.send(msg, new CloudBusCallBack(null) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format(
                                "notify peer[%s] delete pending[%s] failed: %s",
                                peerUuid, fileName, reply.getError()));
                        return;
                    }
                    logger.debug(String.format("notify peer[%s] delete pending[%s] success", peerUuid, fileName));
                }
            });
        }
    }

    private void syncReportToPeers(String snapshotDate, String fileName, String reportJson) {
        String self = Platform.getManagementServerId();
        for (String peerUuid : TelemetryUtils.listManagementNodeUuids()) {
            if (Objects.equals(self, peerUuid)) {
                continue;
            }

            TelemetrySyncReportMsg msg = new TelemetrySyncReportMsg();
            msg.setSnapshotDate(snapshotDate);
            msg.setFileName(fileName);
            msg.setReportJson(reportJson);
            bus.makeServiceIdByManagementNodeId(msg, TelemetryConstant.SERVICE_ID, peerUuid);
            bus.send(msg, new CloudBusCallBack(null) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format(
                                "sync report to peer[%s] failed: %s",
                                peerUuid, reply.getError()));
                        return;
                    }
                    logger.debug(String.format("sync report to peer[%s] success", peerUuid));
                }
            });
        }
    }

    private TelemetryConsentView buildConsentView() {
        TelemetryConsentView view = new TelemetryConsentView();
        view.setConsentGrantedAt(TelemetryGlobalConfig.CONSENT_GRANTED_AT.value());
        return view;
    }

    private TelemetrySettingView buildSettingView() {
        TelemetrySettingView view = new TelemetrySettingView();
        view.setDescriptionKey(TelemetryConstant.SETTING_DESCRIPTION_I18N_KEY);
        view.setPrivacyPolicyUrl(TelemetryConstant.SETTING_PRIVACY_POLICY_URL);
        return view;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(TelemetryConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        TelemetryLocalClient client = Platform.New(TelemetryLocalClient::new);
        client.ensureDirectories();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
