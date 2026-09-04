package org.zstack.sso.oauth2.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.zstack.sso.oauth2.OAuth2Constants;
import org.zstack.sso.oauth2.service.OAuth2ManagerImpl;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @Author: DaoDao
 * @Date: 2022/8/23
 */
@Controller
public class OAuth2Controller {

    private static final CLogger logger = Utils.getLogger(OAuth2Controller.class);

    @Autowired
    private OAuth2ManagerImpl oauth2;

    @RequestMapping(
            value = OAuth2Constants.OAUTH2_REDIRECT,
            method = {
                    RequestMethod.POST, RequestMethod.GET
            })
    public void oauth2Redirect(HttpServletRequest request, HttpServletResponse response) throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        oauth2.oauth2Redirect(request, response);
    }
}

