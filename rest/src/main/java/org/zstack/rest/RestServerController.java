package org.zstack.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by xing5 on 2016/12/7.
 */
@Controller
public class RestServerController {
    @Autowired
    private RestServer server;

    @RequestMapping(
            value = RestConstants.ALL_PATH,
            method = {
                    RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE,
                    RequestMethod.GET, RequestMethod.HEAD,
            }
    )
    public void api(HttpServletRequest request, HttpServletResponse response) throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        server.handle(request, response);
    }
}
