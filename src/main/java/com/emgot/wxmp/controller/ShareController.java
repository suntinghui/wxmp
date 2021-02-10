package com.emgot.wxmp.controller;

import cn.hutool.core.text.StrFormatter;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.StaticLog;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.emgot.wxmp.util.Util;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

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
@RequestMapping("/share")
public class ShareController {

    @Autowired
    private WxMpService mpService;

    @RequestMapping(value="/queryDetail", method={RequestMethod.GET, RequestMethod.POST })
    public ModelAndView queryDetail(HttpServletRequest request, HttpServletResponse response) {
        String activityNbr = request.getParameter("activityNbr");
        String shareCustomerNbr = request.getParameter("shareCustomerNbr");
        StaticLog.info("activityNbr:{}, shareCustomerNbr:{}", activityNbr, shareCustomerNbr);

        String respStr = HttpRequest.post("https://qbhb.emgot.com/qbhbcustomerapi/share/activeDetail")
                .header("content-type", "application/x-www-form-urlencoded")
                .form("activityNbr", activityNbr)
                .execute().body();
        StaticLog.info(respStr);

        ModelAndView modelAndView = new ModelAndView();

        JSONObject rootObject = JSON.parseObject(respStr);
        if (rootObject.get("code").equals("B0000")) {
            try {
                modelAndView.setViewName("activity_detail");
                JSONObject A7100 = (JSONObject) rootObject.getJSONObject("data").getJSONArray("A7100").get(0);
                modelAndView.addObject("title", A7100.getString("subjectName"));
                modelAndView.addObject("img", StrFormatter.format("https://qbhb.emgot.com/{}", A7100.getString("attachmentPath")));
                String joinUrl = StrFormatter.format("{}?activityNbr={}&shareCustomerNbr={}", Util.genServerURL(request, "/share/wxUserInfo"), activityNbr, shareCustomerNbr);
                modelAndView.addObject("join_url", joinUrl);
                modelAndView.addObject("activityNbr", activityNbr);

            } catch (Exception e) {
                modelAndView.setViewName("error");
            }

        } else {
            modelAndView.setViewName("error");
        }

        return modelAndView;
    }

    /*
     用户点击立即参与后执行
     查询用户openid

     */
    @RequestMapping(value = "/wxUserInfo", method={RequestMethod.GET, RequestMethod.POST })
    public void wxUserInfo(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String activityNbr = request.getParameter("activityNbr");
        String shareCustomerNbr = request.getParameter("shareCustomerNbr");

        String redirectUri = Util.genServerURL(request, "/share/wxUserInfo2");
        redirectUri = StrFormatter.format("{}?activityNbr={}&shareCustomerNbr={}", redirectUri, activityNbr, shareCustomerNbr);
        StaticLog.info(redirectUri);

        //String url = StrFormatter.format("https://open.weixin.qq.com/connect/oauth2/authorize?appid={}&redirect_uri={}&response_type=code&scope=snsapi_base&state=123#wechat_redirect", AppID, URLEncoder.encode(redirectUri, "UTF-8"));
        String url = this.mpService.getOAuth2Service().buildAuthorizationUrl(redirectUri, WxConsts.OAuth2Scope.SNSAPI_BASE, null);
        StaticLog.info(url);
        response.sendRedirect(url);
    }

    @RequestMapping(value="/wxUserInfo2", method={RequestMethod.GET, RequestMethod.POST })
    public void wxUserInfo2(HttpServletRequest request, HttpServletResponse response) throws WxErrorException, IOException {
        String code = request.getParameter("code");
        StaticLog.info("code:{}",code);

        WxOAuth2AccessToken wxOAuth2AccessToken = this.mpService.getOAuth2Service().getAccessToken(request.getParameter("code"));
        HashMap<String, String> map = new HashMap<>();
        map.put("openid", wxOAuth2AccessToken.getOpenId());

        /*
        String wxUrl = StrFormatter.format("https://api.weixin.qq.com/sns/oauth2/access_token?appid={}&secret={}&code={}&grant_type=authorization_code", AppID, AppSecret, code);
        String wxRespStr = HttpUtil.get(wxUrl);
        StaticLog.info(wxRespStr);
        JSONObject obj = JSON.parseObject(wxRespStr);
         */

        String activityNbr = request.getParameter("activityNbr");
        String shareCustomerNbr = request.getParameter("shareCustomerNbr");
        String url = "https://qbhb.emgot.com/qbhbcustomerapi/share/toRegist";
        url = StrFormatter.format("{}?activityNbr={}&shareCustomerNbr={}&info={}", url, activityNbr, shareCustomerNbr, URLEncoder.encode(JSON.toJSONString(map), "UTF-8"));
        StaticLog.info(url);
        response.sendRedirect(url);
    }

}
