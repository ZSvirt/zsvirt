package org.zstack.baremetal.preconfiguration;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.baremetal.preconfiguration.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by GuoYi on 2018-12-29.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PreconfigurationTemplateBase implements Preconfiguration {
    private static final CLogger logger = Utils.getLogger(PreconfigurationTemplateBase.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    private TemplateParamExtractor extractor;

    protected PreconfigurationTemplateVO self;

    public PreconfigurationTemplateBase(PreconfigurationTemplateVO self) {
        this.self = self;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage(msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(Message msg) {
        if (msg instanceof APIUpdatePreconfigurationTemplateMsg) {
            handle((APIUpdatePreconfigurationTemplateMsg) msg);
        } else if (msg instanceof APIDeletePreconfigurationTemplateMsg) {
            handle((APIDeletePreconfigurationTemplateMsg) msg);
        } else if (msg instanceof APIChangePreconfigurationTemplateStateMsg) {
            handle((APIChangePreconfigurationTemplateStateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handle(APIUpdatePreconfigurationTemplateMsg msg) {
        APIUpdatePreconfigurationTemplateEvent evt = new APIUpdatePreconfigurationTemplateEvent(msg.getId());
        self = dbf.reload(self);
        if (msg.getName() != null) {
            self.setName(msg.getName());
        }

        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
        }

        if (msg.getDistribution() != null) {
            self.setDistribution(msg.getDistribution());
        }

        if (msg.getType() != null) {
            self.setType(msg.getType());
        }

        if (msg.getContent() != null && !msg.getContent().equals(self.getContent())) {
            String templateUuid = self.getUuid();
            TemplateParamExtractor.Result result = extractor.extractCustomParams(msg.getContent());
            new SQLBatch() {
                @Override
                protected void scripts() {
                    // update template custom params
                    sql(TemplateCustomParamVO.class)
                            .eq(TemplateCustomParamVO_.templateUuid, templateUuid)
                            .delete();
                    self.setCustomParams(null);

                    for (String param : result.getParams()) {
                        TemplateCustomParamVO pvo = new TemplateCustomParamVO();
                        pvo.setTemplateUuid(templateUuid);
                        pvo.setParam(param);
                        persist(pvo);
                    }

                    // TODO set bm.templateUuid to null in template extension point
                }
            }.execute();

            self.setContent(msg.getContent());
            self.setMd5sum(StringDSL.getMd5Sum(msg.getContent()));
        }

        self = dbf.updateAndRefresh(self);
        evt.setInventory(self.toInventory());
        bus.publish(evt);
    }

    private void handle(APIDeletePreconfigurationTemplateMsg msg) {
        APIDeletePreconfigurationTemplateEvent evt = new APIDeletePreconfigurationTemplateEvent(msg.getId());
        dbf.remove(self);
        bus.publish(evt);
    }

    private void handle(APIChangePreconfigurationTemplateStateMsg msg) {
        APIChangePreconfigurationTemplateStateEvent evt = new APIChangePreconfigurationTemplateStateEvent(msg.getId());
        self = dbf.reload(self);
        if (msg.getStateEvent() == PreconfigurationTemplateStateEvent.enable) {
            self.setState(PreconfigurationTemplateState.Enabled);
        } else {
            self.setState(PreconfigurationTemplateState.Disabled);
        }
        self = dbf.updateAndRefresh(self);
        evt.setInventory(self.toInventory());
        bus.publish(evt);
    }
}
