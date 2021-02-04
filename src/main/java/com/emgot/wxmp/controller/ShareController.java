package com.emgot.wxmp.controller;

import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.log.StaticLog;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.emgot.wxmp.util.Util;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;

/*
本类用于惠众宝分享使用
1、查询商品详情
 */
@Controller
@RequestMapping("/wxmp")
public class ShareController {

    private static final String AppID = "wx8e460eeebd076f72";
    private static final String AppSecret = "e9c1ec28dd64d5e2c7a3a369f4a9abb8";

    @Autowired
    private WxMpService mpService;

    @RequestMapping(value="/queryDetail", method={RequestMethod.GET, RequestMethod.POST })
    @ResponseBody
    public String queryDetail(HttpServletRequest request, HttpServletResponse response) {
        String activityNbr = request.getParameter("activityNbr"); // 分享的商品
        StaticLog.info("activityNbr:{}", activityNbr);

        String respStr = HttpRequest.post("https://qbhb.emgot.com/qbhbcustomerapi/share/activeDetail")
                .header("content-type", "application/x-www-form-urlencoded")
                .form("activityNbr", activityNbr)
                .execute().body();

        StaticLog.info(respStr);
        return respStr;
    }

    /*
     用户点击立即参与后执行
     查询用户openid

     */
    @RequestMapping(value = "/wxUserInfo", method={RequestMethod.GET, RequestMethod.POST })
    public void wxUserInfo(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String activityNbr = request.getParameter("activityNbr");
        String shareCustomerNbr = request.getParameter("shareCustomerNbr");

        String redirectUri = Util.genServerURL(request, "/wxmp/yaoyao");
        redirectUri = StrFormatter.format("{}?activityNbr={}&shareCustomerNbr={}", redirectUri, activityNbr, shareCustomerNbr);
        StaticLog.info(redirectUri);

        String url = StrFormatter.format("https://open.weixin.qq.com/connect/oauth2/authorize?appid={}&redirect_uri={}&response_type=code&scope=snsapi_base&state=123#wechat_redirect", AppID, URLEncoder.encode(redirectUri, "UTF-8"));
        //String url = this.mpService.getOAuth2Service().buildAuthorizationUrl(redirectUri, WxConsts.OAuth2Scope.SNSAPI_BASE, null);
        StaticLog.info(url);
        response.sendRedirect(url);
    }

    @RequestMapping(value="/yaoyao", method={RequestMethod.GET, RequestMethod.POST })
    public String yaoyao(HttpServletRequest request, HttpServletResponse response) throws WxErrorException, IOException {
        String code = request.getParameter("code");
        StaticLog.info("code:{}",code);

        /*
        WxOAuth2AccessToken wxOAuth2AccessToken = this.mpService.getOAuth2Service().getAccessToken(request.getParameter("code"));
        HashMap<String, String> map = new HashMap<>();
        map.put("openid", wxOAuth2AccessToken.getOpenId());
        */

        String wxUrl = StrFormatter.format("https://api.weixin.qq.com/sns/oauth2/access_token?appid={}&secret={}&code={}&grant_type=authorization_code", AppID, AppSecret, code);
        String wxRespStr = HttpUtil.get(wxUrl);
        StaticLog.info(wxRespStr);
        JSONObject obj = JSON.parseObject(wxRespStr);

        String activityNbr = request.getParameter("activityNbr");
        String shareCustomerNbr = request.getParameter("shareCustomerNbr");
        String url = "https://qbhb.emgot.com/qbhbcustomerapi/share/toRegist";
        url = StrFormatter.format("{}?activityNbr={}&shareCustomerNbr={}&info={}", url, activityNbr, shareCustomerNbr, wxRespStr);

        String respStr = HttpUtil.get(url);
        StaticLog.info("\n{}\n",respStr);

        return respStr;
    }

}
