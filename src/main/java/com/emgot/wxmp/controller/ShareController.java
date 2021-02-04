package com.emgot.wxmp.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.log.StaticLog;
import com.alibaba.fastjson.JSON;
import com.emgot.wxmp.util.Util;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/share")
public class ShareController {

    @Autowired
    private WxMpService mpService;

    public void queryDetail() {

    }

    /**
     * 该服务用于支持惠众宝中的分享功能
     *
     */
    @GetMapping("/auth2")
    public void auth2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        StringBuilder redirectUri = new StringBuilder(Util.genServerURL(request, "/wxmp/callback2?"));
        redirectUri.append("activityNbr=").append(request.getParameter("activityNbr")).append("&")
                .append("shareCustomerNbr=").append(request.getParameter("shareCustomerNbr")).append("&")
                .append("mobileNbr=").append(request.getParameter("mobileNbr"));
        String url = this.mpService.getOAuth2Service().buildAuthorizationUrl(redirectUri.toString(), WxConsts.OAuth2Scope.SNSAPI_BASE, null);
        // https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx7a24fcce32609ee0&redirect_uri=http%3A%2F%2Fqbhb.emgot.com%2Fcallback&response_type=code&scope=snsapi_userinfo&state=&connect_redirect=1#wechat_redirect
        StaticLog.info(url);
        response.sendRedirect(url);
    }

    @GetMapping("/callback2")
    public void callback2(HttpServletRequest request, HttpServletResponse response) throws WxErrorException, IOException {
        String code = request.getParameter("code");
        StaticLog.info("获得Code：" + code);
        WxOAuth2AccessToken wxOAuth2AccessToken = this.mpService.getOAuth2Service().getAccessToken(code);
        // 获得用户基本信息
        WxOAuth2UserInfo userInfo = this.mpService.getOAuth2Service().getUserInfo(wxOAuth2AccessToken, null);

        StaticLog.info(JSON.toJSONString(userInfo));

        // 验证token
        boolean valid = this.mpService.getOAuth2Service().validateAccessToken(wxOAuth2AccessToken);
        if (valid) {
            StaticLog.info("Token验证通过");
        } else {
            StaticLog.info("Token验证失败");
        }

        StringBuilder url = new StringBuilder("https://qbhb.emgot.com/emgot/index.html?");
        url.append("activityNbr=").append(request.getParameter("activityNbr")).append("&")
                .append("shareCustomerNbr=").append(request.getParameter("shareCustomerNbr")).append("&")
                .append("mobileNbr=").append(request.getParameter("mobileNbr")).append("&")
                .append("info=").append(JSON.toJSONString(userInfo));

        response.sendRedirect(url.toString());
    }

}
