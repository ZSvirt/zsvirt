package org.zstack.core.captcha;

import org.apache.commons.codec.binary.Base64;
import org.patchca.color.ColorFactory;
import org.patchca.filter.predefined.*;
import org.patchca.service.ConfigurableCaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.captcha.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.imageio.ImageIO;
import javax.persistence.Query;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


/**
 * Created by kayo on 2018/7/4.
 */
public class CaptchaImpl extends AbstractService implements Component, Captcha {
    private static final CLogger logger = Utils.getLogger(CaptchaImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;

    private String CAPTCHA_FILE_TYPE = "png";

    private static ConfigurableCaptchaService cs;

    private Future<Void> staleCaptchaCollector;

    static {
        cs = new ConfigurableCaptchaService();

        cs.setColorFactory(new ColorFactory() {
            public Color getColor(int x) {
                int[] c = new int[3];
                int i = ThreadLocalRandom.current().nextInt(c.length);
                for (int fi = 0; fi < c.length; fi++) {
                    if (fi == i) {
                        c[fi] = ThreadLocalRandom.current().nextInt(55, 94);
                    } else {
                        c[fi] = ThreadLocalRandom.current().nextInt(1, 255);
                    }
                }
                return new Color(c[0], c[1], c[2]);
            }
        });
    }

    private String bytesToBase64(byte[] bytes) {
        return Base64.encodeBase64String(bytes);// 返回Base64编码过的字节数组字符串
    }

    private void useRandomFilter() {
        switch (ThreadLocalRandom.current().nextInt(5)) {
            case 0:
                cs.setFilterFactory(new CurvesRippleFilterFactory(cs.getColorFactory()));
                break;
            case 1:
                cs.setFilterFactory(new MarbleRippleFilterFactory());
                break;
            case 2:
                cs.setFilterFactory(new DoubleRippleFilterFactory());
                break;
            case 3:
                cs.setFilterFactory(new WobbleRippleFilterFactory());
                break;
            case 4:
                cs.setFilterFactory(new DiffuseRippleFilterFactory());
                break;
        }
    }

    @Override
    public CaptchaVO generateCaptcha(String targetResourceIdentity) {
        String verifyCode = "";
        String base64Image = "";
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            useRandomFilter();
            org.patchca.service.Captcha captcha = cs.getCaptcha();
            ImageIO.write(captcha.getImage(), CAPTCHA_FILE_TYPE, baos);
            baos.flush();
            byte[] imageInByte = baos.toByteArray();
            base64Image = bytesToBase64(imageInByte);
            verifyCode = captcha.getChallenge();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        CaptchaVO vo = new CaptchaVO();
        vo.setVerifyCode(verifyCode);
        vo.setCaptcha(base64Image);
        vo.setTargetResourceIdentity(targetResourceIdentity);
        vo.setUuid(Platform.getUuid());
        return dbf.persistAndRefresh(vo);
    }

    public CaptchaVO refreshCaptcha(String uuid) {
        String verifyCode = "";
        String base64Image = "";
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            useRandomFilter();
            org.patchca.service.Captcha captcha = cs.getCaptcha();
            ImageIO.write(captcha.getImage(), CAPTCHA_FILE_TYPE, baos);
            baos.flush();
            byte[] imageInByte = baos.toByteArray();
            base64Image = bytesToBase64(imageInByte);
            verifyCode = captcha.getChallenge();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        CaptchaVO vo = Q.New(CaptchaVO.class).eq(CaptchaVO_.uuid, uuid).find();
        vo.setCaptcha(base64Image);
        vo.setVerifyCode(verifyCode);
        return dbf.updateAndRefresh(vo);
    }

    @Override
    public void refreshCaptcha(String uuid, ReturnValueCompletion<CaptchaStruct> completion) {
        CaptchaVO vo = refreshCaptcha(uuid);

        CaptchaStruct struct = new CaptchaStruct();
        struct.setCaptcha(vo.getCaptcha());
        struct.setUuid(vo.getUuid());

        completion.success(struct);
    }

    @Override
    public boolean verifyCaptcha(String uuid, String verifyCode, String targetResourceIdentify) {
        if (Q.New(CaptchaVO.class)
                .eq(CaptchaVO_.uuid, uuid)
                .eq(CaptchaVO_.verifyCode, verifyCode)
                .eq(CaptchaVO_.targetResourceIdentity, targetResourceIdentify)
                .isExists()) {
            return true;
        }

        return false;
    }

    @Override
    public boolean start() {
        startStaleCaptchaCollector();

        return true;
    }

    private void startStaleCaptchaCollector() {
        final int interval = CaptchaGlobalConfig.CAPTCHA_CLEANUP_INTERVAL.value(Integer.class);
        staleCaptchaCollector = thdf.submitPeriodicTask(new PeriodicTask() {
            @Transactional(readOnly = true)
            private Timestamp getCurrentSqlDate() {
                Query query = dbf.getEntityManager().createNativeQuery("select current_timestamp()");
                return (Timestamp) query.getSingleResult();
            }

            @Override
            public void run() {
                Long finalExtendPeriod = CaptchaGlobalConfig.CAPTCHA_VALID_PERIOD.value(Long.class);

                Timestamp expiredDate = new Timestamp(getCurrentSqlDate().getTime() - TimeUnit.SECONDS.toMillis(finalExtendPeriod));

                SQL.New(CaptchaVO.class).lt(CaptchaVO_.createDate, expiredDate).delete();
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return interval;
            }

            @Override
            public String getName() {
                return "StaleCaptchaCleanupThread";
            }

        });
    }

    @Override
    public boolean stop() {
        if (staleCaptchaCollector != null) {
            staleCaptchaCollector.cancel(true);
        }

        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIRefreshCaptchaMsg) {
            handle((APIRefreshCaptchaMsg) msg);
        }
    }

    private void handle(APIRefreshCaptchaMsg msg) {
        APIRefreshCaptchaReply reply = new APIRefreshCaptchaReply();
        CaptchaVO vo = dbf.findByUuid(msg.getUuid(), CaptchaVO.class);

        refreshCaptcha(vo.getUuid(), new ReturnValueCompletion<CaptchaStruct>(msg) {
            @Override
            public void success(CaptchaStruct struct) {
                reply.setCaptcha(struct.getCaptcha());
                reply.setCaptchaUuid(struct.getUuid());

                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);

                bus.reply(msg, reply);
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(CaptchaConstant.SERVICE_ID);
    }
}
