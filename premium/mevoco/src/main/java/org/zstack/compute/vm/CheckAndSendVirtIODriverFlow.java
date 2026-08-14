package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.SingleFlightTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.kvm.KVMHostVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.compute.vm.PremiumVmInstanceConstant.*;
import static org.zstack.core.Platform.getManagementServerId;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by Wenhao.Zhang on 21/09/06
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CheckAndSendVirtIODriverFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(CheckAndSendVirtIODriverFlow.class);

    private KVMHostVO kvm;
    /**
     * key: source path in management node
     * value: destination path in host
     */
    private Map<String, String> dstPathsMap;
    /**
     * key: source path in management node
     * value: expect file MD5 sum
     */
    private Map<String, String> expectDriverMd5s;
    /**
     * key: source path in management node
     * value: source absolute path in management node
     */
    private Map<String, String> srcAbsolutePathMap = new HashMap<>();

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;

    @Override
    @SuppressWarnings("rawtypes")
    public void run(FlowTrigger trigger, Map data) {
        final String hostUuid = kvm.getUuid();

        thdf.singleFlightSubmit(new SingleFlightTask(trigger)
                .setSyncSignature(String.format("instantiate-virtio-driver-iso-on-host-%s", hostUuid))
                .run((completion) -> instantiateVirtIODriverISO(hostUuid, new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success(null);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                }))
                .done(((result) -> {
                    if (!result.isSuccess()) {
                        trigger.fail(result.getErrorCode());
                        return;
                    }

                    trigger.next();
                })));
    }

    @SuppressWarnings("rawtypes")
    private void instantiateVirtIODriverISO(String hostUuid, Completion completion) {
        FlowChain chain = new SimpleFlowChain();
        chain.setName("instantiate-virtio-driver-iso-on-host");
        chain.then(new NoRollbackFlow() {
            String __name__ = "check-driver-file-in-MN";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                checkDriverFileInMN(new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
            @Override
            public boolean skip(Map data) {
                return CoreGlobalProperty.UNIT_TEST_ON;
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("send-file-from-MN-to-host-%s", hostUuid);
            @Override
            public void run(FlowTrigger trigger, Map data) {
                transferFileFromMNToHost(new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        });

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void checkDriverFileInMN(Completion completion) {
        for (Map.Entry<String, String> entry : this.expectDriverMd5s.entrySet()) {
            String srcPath = entry.getKey();
            String expectDriverMd5 = entry.getValue();

            File file = PathUtil.findFileOnClassPath(srcPath);
            if (file == null) {
                logger.error(String.format("fail to read md5 of file[%s] because file not found on classpath", srcPath));
                completion.fail(operr("fail to attach virtio driver because read md5 of file[%s] fail in mn[uuid:%s]: file not found on classpath",
                srcPath, getManagementServerId()));
                return;
            }
            try {
                boolean valid = PathUtil.checkFileByMd5(file, expectDriverMd5);
                if (!valid) {
                    completion.fail(operr("fail to attach virtio driver because of invalid md5 of file[%s] in mn[uuid:%s]", srcPath, getManagementServerId()));
                    return;
                }
            } catch (RuntimeException e) {
                logger.error(String.format("fail to read md5 of file[%s] because %s", srcPath, e.getMessage()));
                completion.fail(operr("fail to attach virtio driver because read md5 of file[%s] fail in mn[uuid:%s]: %s", srcPath, getManagementServerId(), e.getMessage()));
                return;
            }
            srcAbsolutePathMap.put(srcPath, file.getAbsolutePath());
        }
        completion.success();
    }

    private void transferFileFromMNToHost(Completion completion) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            completion.success();
            return;
        }

        AnsibleRunner runner = new AnsibleRunner();
        dstPathsMap.forEach((srcPathInMN, dstPathInHost) -> {
            SshFileMd5Checker checker = new SshFileMd5Checker();
            checker.setTargetIp(kvm.getManagementIp());
            checker.setUsername(kvm.getUsername());
            checker.setPassword(kvm.getPassword());
            checker.setSshPort(kvm.getPort());
            checker.addSrcDestPair(srcAbsolutePathMap.get(srcPathInMN), dstPathInHost);
            runner.installChecker(checker);
        });

        List<Map.Entry<String, String>> entries = new ArrayList<>(dstPathsMap.entrySet());
        List<String> srcPathsInMN = entries.stream().map(entry -> srcAbsolutePathMap.get(entry.getKey())).collect(Collectors.toList());
        List<String> dstPathsInHost = entries.stream().map(Map.Entry::getValue).collect(Collectors.toList());

        runner.setPlayBookName(ANSIBLE_PLAYBOOK_NAME);
        runner.setTargetIp(kvm.getManagementIp());
        runner.setUsername(kvm.getUsername());
        runner.setPassword(kvm.getPassword());
        runner.setSshPort(kvm.getPort());
        runner.putArgument("src_paths_in_mn", StringUtils.join(srcPathsInMN, ','));
        runner.putArgument("dst_paths_in_host", StringUtils.join(dstPathsInHost, ','));

        runner.run(new ReturnValueCompletion<Boolean>(completion) {
            @Override
            public void success(Boolean sendFlag) {
                logger.info(String.format("successfully send %s to kvm host[uuid:%s]", srcPathsInMN, kvm.getUuid()));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to send %s to kvm host[uuid:%s]", srcPathsInMN, kvm.getUuid()));
                completion.fail(errorCode);
            }
        });
    }

    public void setKvm(KVMHostVO kvm) {
        this.kvm = kvm;
    }

    public void setDstPathsMap(Map<String, String> dstPathsMap) {
        this.dstPathsMap = dstPathsMap;
    }

    public void setExpectDriverMd5s(Map<String, String> expectDriverMd5s) {
        this.expectDriverMd5s = expectDriverMd5s;
    }

    @SuppressWarnings("unchecked")
    public void configWithVFDDriverPath() {
        String vfdAmd64Path = Paths.get(VIRTIO_DRIVER_DIRECTORY_ON_MN_CLASS_PATH, VIRTIO_DRIVER_VFD_AMD64_FILENAME).toString();
        String vfdX86Path = Paths.get(VIRTIO_DRIVER_DIRECTORY_ON_MN_CLASS_PATH, VIRTIO_DRIVER_VFD_X86_FILENAME).toString();
        this.setDstPathsMap(map(e(vfdAmd64Path, VIRTIO_DRIVER_VFD_AMD64_PATH_ON_HOST),
                e(vfdX86Path, VIRTIO_DRIVER_VFD_X86_PATH_ON_HOST)));
        this.setExpectDriverMd5s(map(e(vfdAmd64Path, MD5_VIRTIO_DRIVER_VFD_AMD64),
                e(vfdX86Path, MD5_VIRTIO_DRIVER_VFD_X86)));
    }
}
