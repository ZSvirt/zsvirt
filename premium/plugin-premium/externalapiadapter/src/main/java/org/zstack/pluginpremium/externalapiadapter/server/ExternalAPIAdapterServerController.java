package org.zstack.pluginpremium.externalapiadapter.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by lining on 2018/4/19.
 */
@Controller
public class ExternalAPIAdapterServerController {

    @Autowired
    private ExternalAPIAdapterServer server;

    @RequestMapping(
            value = ExternalAPIAdapterConstants.ALL_PATH,
            method = {
                    RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.GET,
                    RequestMethod.HEAD, RequestMethod.OPTIONS, RequestMethod.PATCH, RequestMethod.TRACE
            }
    )
    public void api(HttpServletRequest request, HttpServletResponse response) throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        server.handle(request, response);
    }
}
