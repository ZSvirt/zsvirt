package org.zstack.testlib;

import org.apache.logging.log4j.ThreadContext;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.Constants;
import org.zstack.header.message.AbstractBeforeSendMessageInterceptor;
import org.zstack.header.message.CarrierMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.BeforeAsyncJsonPostInterceptor;
import org.zstack.header.rest.RESTFacade;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2017/3/24.
 */
public class ApiPathTracker {
    private CloudBus bus;
    private RESTFacade restf;

    final List<Path> paths = new ArrayList<>();

    private enum Type {
        Message,
        HttpRPC,
    }

    private static class Path {
        Type type;
        String path;
    }

    public List<String> getApiPath() {
        synchronized (paths) {
            return paths.stream().map(p -> String.format("(%s) %s", p.type, p.path)).collect(Collectors.toList());
        }
    }

    public ApiPathTracker(String apiId) {
        bus = Platform.getComponentLoader().getComponent(CloudBus.class);
        restf = Platform.getComponentLoader().getComponent(RESTFacade.class);

        bus.installBeforeSendMessageInterceptor(new AbstractBeforeSendMessageInterceptor() {
            @Override
            public void beforeSendMessage(Message msg) {
                if (msg instanceof MessageReply || msg instanceof CarrierMessage) {
                    return;
                }

                String id = ThreadContext.get(Constants.THREAD_CONTEXT_API);

                if (id == null || !id.equals(apiId)) {
                    return;
                }

                Path p = new Path();
                p.type = Type.Message;
                p.path = msg.getClass().getName();
                paths.add(p);
            }
        });

        restf.installBeforeAsyncJsonPostInterceptor(new BeforeAsyncJsonPostInterceptor() {
            @Override
            public void beforeAsyncJsonPost(String url, Object body, TimeUnit unit, long timeout) {
                String id = ThreadContext.get(Constants.THREAD_CONTEXT_API);

                if (id == null || !id.equals(apiId)) {
                    return;
                }

                Path p = new Path();
                p.type = Type.HttpRPC;
                p.path = String.format("[url:%s, cmd: %s]", url, body.getClass().getName());
                paths.add(p);
            }

            @Override
            public void beforeAsyncJsonPost(String url, String body, TimeUnit unit, long timeout) {
                String id = ThreadContext.get(Constants.THREAD_CONTEXT_API);

                if (id == null || !id.equals(apiId)) {
                    return;
                }

                Path p = new Path();
                p.type = Type.HttpRPC;
                p.path = String.format("[url:%s, cmd body: %s]", url, body);
                paths.add(p);
            }
        });
    }
}
