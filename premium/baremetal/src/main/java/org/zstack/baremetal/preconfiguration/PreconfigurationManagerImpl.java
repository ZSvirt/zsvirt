package org.zstack.baremetal.preconfiguration;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.AbstractService;
import org.zstack.header.baremetal.preconfiguration.*;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by GuoYi on 2018-12-26.
 */
public class PreconfigurationManagerImpl extends AbstractService implements PreconfigurationManager {
    private static final CLogger logger = Utils.getLogger(PreconfigurationManagerImpl.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    private TemplateParamExtractor extractor;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof PreconfigurationTemplateMessage) {
            passThrough((PreconfigurationTemplateMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage(msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(PreconfigurationTemplateMessage msg) {
        PreconfigurationTemplateVO vo = dbf.findByUuid(msg.getTemplateUuid(), PreconfigurationTemplateVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("cannot find PreconfigurationTemplateVO[uuid:%s], it may have been deleted", msg.getTemplateUuid()));
        }

        new PreconfigurationTemplateBase(vo).handleMessage((Message) msg);
    }

    private void handleApiMessage(Message msg) {
        if (msg instanceof APIAddPreconfigurationTemplateMsg) {
            handle((APIAddPreconfigurationTemplateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof AddPreconfigurationTemplateMsg) {
            handle((AddPreconfigurationTemplateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(AddPreconfigurationTemplateMsg msg) {
        AddPreconfigurationTemplateReply reply = new AddPreconfigurationTemplateReply();

        // check common params in pre-defined configurations
        TemplateParamExtractor.Result commonParams = extractor.extractCommonParams(msg.getContent());
        if (!commonParams.isSuccess()) {
            reply.setError(operr(commonParams.getError()));
            bus.reply(msg, reply);
            return;
        }

        TemplateParamExtractor.Result result = extractor.extractCustomParams(msg.getContent());
        PreconfigurationTemplateVO vo = new SQLBatchWithReturn<PreconfigurationTemplateVO>() {
            @Override
            protected PreconfigurationTemplateVO scripts() {
                PreconfigurationTemplateVO vo = new PreconfigurationTemplateVO();
                vo.setUuid(Platform.getUuid());
                vo.setName(msg.getName());
                vo.setDescription(msg.getDescription());
                vo.setDistribution(msg.getDistribution());
                vo.setType(msg.getType());
                vo.setContent(msg.getContent());
                vo.setMd5sum(StringDSL.getMd5Sum(msg.getContent()));
                vo.setPredefined(msg.getPredefined());
                vo.setState(PreconfigurationTemplateState.Enabled);
                vo.setAccountUuid(msg.getAccountUuid());
                vo = persist(vo);

                // share to public if isPredefined
                if (msg.getPredefined()) {
                    AccountResourceRefVO ref = new AccountResourceRefVO();
                    ref.setResourceUuid(vo.getUuid());
                    ref.setResourceType(PreconfigurationTemplateVO.class.getSimpleName());
                    ref.setType(AccessLevel.SharePublic);
                    persist(ref);
                }

                // record custom params
                for (String param : result.getParams()) {
                    TemplateCustomParamVO pvo = new TemplateCustomParamVO();
                    pvo.setTemplateUuid(vo.getUuid());
                    pvo.setParam(param);
                    persist(pvo);
                }

                return vo;
            }
        }.execute();

        reply.setInventory(vo.toInventory());
        bus.reply(msg, reply);
    }

    private void handle(APIAddPreconfigurationTemplateMsg msg) {
        APIAddPreconfigurationTemplateEvent evt = new APIAddPreconfigurationTemplateEvent(msg.getId());
        AddPreconfigurationTemplateMsg amsg = AddPreconfigurationTemplateMsg.valueOf(msg);
        bus.makeLocalServiceId(amsg, PreconfigurationConstant.SERVICE_ID);
        bus.send(amsg, new CloudBusCallBack(evt) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    AddPreconfigurationTemplateReply rly = reply.castReply();
                    evt.setInventory(rly.getInventory());
                } else {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    // add or update predefined templates
    private void preparePredefinedTemplates() {
        List<AddPreconfigurationTemplateMsg> amsgs = new ArrayList<>();
        List<String> predefinedTemplates = PathUtil.scanFolderOnClassPath(PreconfigurationConstant.predefinedTemplateFolder);

        Map<String, String> newPredefinedTemplates = new HashMap<>();
        for (String template : predefinedTemplates) {
            String name = PathUtil.fileName(template);
            // remove extension from predefined template filename
            if(name.contains(".")) {
                name = name.substring(0, name.lastIndexOf('.'));
            }
            newPredefinedTemplates.put(template, name);
        }

        // delete outdated predefined templates from database
        SQL.New(PreconfigurationTemplateVO.class)
                .eq(PreconfigurationTemplateVO_.isPredefined, true)
                .notIn(PreconfigurationTemplateVO_.name, newPredefinedTemplates.values())
                .delete();

        // add new predefined templates to database
        for (String template : predefinedTemplates) {
            String name = newPredefinedTemplates.get(template);
            boolean exists = Q.New(PreconfigurationTemplateVO.class)
                    .eq(PreconfigurationTemplateVO_.isPredefined, true)
                    .eq(PreconfigurationTemplateVO_.name, name)
                    .isExists();
            if (exists) continue;

            String content;
            File file = new File(template);
            try {
                content = FileUtils.readFileToString(file);
            } catch (IOException e) {
                logger.warn(String.format("read predefined preconfiguration template file failed, due to: %s", e.getMessage()));
                continue;
            }

            AddPreconfigurationTemplateMsg amsg = new AddPreconfigurationTemplateMsg();
            amsg.setPredefined(true);
            amsg.setName(name);
            amsg.setContent(content);
            amsg.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);

            if (name.matches("centos_.*_x86_64.*")) {
                amsg.setDistribution("centos-x86_64");
                amsg.setDescription("kickstart template for centos x86_64");
                amsg.setType(PreconfigurationTemplateType.kickstart.toString());
            } else if (name.matches("zstack_.*_x86_64.*")) {
                amsg.setDistribution("zstack-x86_64");
                amsg.setDescription("kickstart template for zstack x86_64");
                amsg.setType(PreconfigurationTemplateType.kickstart.toString());
            } else if (name.matches("ubuntu_.*_live_server_x86_64_.*")) {
                amsg.setDistribution("ubuntu-live-server-x86_64");
                amsg.setDescription("autoinstall template for ubuntu x86_64");
                amsg.setType(PreconfigurationTemplateType.autoinstall.toString());
            } else if (name.matches("ubuntu_.*_x86_64.*")) {
                amsg.setDistribution("ubuntu-x86_64");
                amsg.setDescription("preseed template for ubuntu x86_64");
                amsg.setType(PreconfigurationTemplateType.preseed.toString());
            } else if (name.matches("opensuse_.*_x86_64.*")) {
                amsg.setDistribution("opensuse-x86_64");
                amsg.setDescription("autoyast template for opensuse x86_64");
                amsg.setType(PreconfigurationTemplateType.autoyast.toString());
            } else if (name.startsWith("cloud")) {
                amsg.setDistribution("cloud-x86_64");
                amsg.setDescription("kickstart template for cloud x86_64");
                amsg.setType(PreconfigurationTemplateType.kickstart.toString());
            }  else if (name.matches("kylin_.*_x86_64.*")) {
                amsg.setDistribution("kylin-x86_64");
                amsg.setDescription("kickstart template for kylin x86_64");
                amsg.setType(PreconfigurationTemplateType.kickstart.toString());
            }  else if (name.matches("kylin_.*_aarch64.*")) {
                amsg.setDistribution("kylin-aarch64");
                amsg.setDescription("kickstart template for kylin aarch64");
                amsg.setType(PreconfigurationTemplateType.kickstart.toString());
            }  else if (name.matches("centos_.*_aarch64.*")) {
                amsg.setDistribution("centos-aarch64");
                amsg.setDescription("kickstart template for centos aarch64");
                amsg.setType(PreconfigurationTemplateType.kickstart.toString());
            }  else if (name.matches("openEuler_.*_aarch64.*")) {
                amsg.setDistribution("openEuler-aarch64");
                amsg.setDescription("kickstart template for openEuler aarch64");
                amsg.setType(PreconfigurationTemplateType.kickstart.toString());
            } else {
                logger.warn("unknown predefined preconfiguration template: " + name);
                continue;
            }

            bus.makeLocalServiceId(amsg, PreconfigurationConstant.SERVICE_ID);
            amsgs.add(amsg);
        }

        if (amsgs.isEmpty()) {
            return;
        }

        new While<>(amsgs).each((amsg, comp) -> bus.send(amsg, new CloudBusCallBack(comp) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.error("failed to create predefined preconfigurate template named " + amsg.getName());
                }
                comp.done();
            }
        })).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                logger.debug("successfully prepared all predefined preconfiguration templates");
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(PreconfigurationConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        preparePredefinedTemplates();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
