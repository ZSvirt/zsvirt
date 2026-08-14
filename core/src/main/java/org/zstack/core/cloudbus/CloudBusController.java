package org.zstack.core.cloudbus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

@Controller
public class CloudBusController {
    private static final CLogger logger = Utils.getLogger(CloudBusController.class);

    @Autowired
    private CloudBus bus;

    private HttpEntity<String> toHttpEntity(HttpServletRequest req) {
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = req.getReader().readLine()) != null) {
                sb.append(line);
            }
            req.getReader().close();

            HttpHeaders header = new HttpHeaders();
            for (Enumeration e = req.getHeaderNames(); e.hasMoreElements() ;) {
                String name = e.nextElement().toString();
                header.add(name, req.getHeader(name));
            }

            return new HttpEntity<>(sb.toString(), header);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            throw new CloudRuntimeException(e);
        }
    }

    @RequestMapping(value = CloudBusImpl3.HTTP_BASE_URL, method = RequestMethod.POST)
    public void handle(HttpServletRequest request, HttpServletResponse response) {
        HttpEntity<String> entity = toHttpEntity(request);
        //Platform.getComponentLoader().getComponent(CloudBusImpl3.class).handleHttpRequest(entity, response);
        bus.handleHttpRequest(entity, response);
    }
}
