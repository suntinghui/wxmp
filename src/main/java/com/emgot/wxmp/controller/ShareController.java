package com.emgot.wxmp.controller;

import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.log.StaticLog;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.emgot.wxmp.model.GoodsDetail;
import com.emgot.wxmp.util.AppUtils;
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
    public void queryDetail(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String activityNbr = request.getParameter("activityNbr");
        String shareCustomerNbr = request.getParameter("shareCustomerNbr");
        String merchantNbr = request.getParameter("merchantNbr");
        String platformType = request.getParameter("platformType");

        StaticLog.info("activityNbr:{}, shareCustomerNbr:{}, merchantNbr:{}, platformType:{}", activityNbr, shareCustomerNbr, merchantNbr, platformType);

        String wxUrl = StrFormatter.format("{}?activityNbr={}&shareCustomerNbr={}&merchantNbr={}&platformType={}", AppUtils.genServerURL(request, "/share/wxUserInfo"), activityNbr, shareCustomerNbr, merchantNbr, platformType);
        response.sendRedirect(wxUrl);
    }

    /*
     用户点击立即参与后执行
     查询用户openid
     */
    @RequestMapping(value = "/wxUserInfo", method={RequestMethod.GET, RequestMethod.POST })
    public void wxUserInfo(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String activityNbr = request.getParameter("activityNbr");
        String shareCustomerNbr = request.getParameter("shareCustomerNbr");
        String merchantNbr = request.getParameter("merchantNbr");
        String platformType = request.getParameter("platformType");

        String redirectUri = AppUtils.genServerURL(request, "/share/wxUserInfo2");
        redirectUri = StrFormatter.format("{}?activityNbr={}&shareCustomerNbr={}&merchantNbr={}&platformType={}", redirectUri, activityNbr, shareCustomerNbr,merchantNbr, platformType);
        StaticLog.info(redirectUri);

        //String url = StrFormatter.format("https://open.weixin.qq.com/connect/oauth2/authorize?appid={}&redirect_uri={}&response_type=code&scope=snsapi_base&state=123#wechat_redirect", AppID, URLEncoder.encode(redirectUri, "UTF-8"));
        String url = this.mpService.getOAuth2Service().buildAuthorizationUrl(redirectUri, WxConsts.OAuth2Scope.SNSAPI_BASE, null);
        StaticLog.info(url);
        response.sendRedirect(url);
    }

    // 通过上面的wxUserInfo方法发起调用
    @RequestMapping(value="/wxUserInfo2", method={RequestMethod.GET, RequestMethod.POST })
    public ModelAndView wxUserInfo2(HttpServletRequest request, HttpServletResponse response) throws WxErrorException, IOException {
        // 取得code
        String code = request.getParameter("code");
        StaticLog.info("code:{}",code);
        // 使用code换得openId
        WxOAuth2AccessToken wxOAuth2AccessToken = this.mpService.getOAuth2Service().getAccessToken(request.getParameter("code"));
        String openId = wxOAuth2AccessToken.getOpenId();
        StaticLog.info("openId:{}",openId);
        // 初始的两个基础参数
        String activityNbr = request.getParameter("activityNbr");
        String shareCustomerNbr = request.getParameter("shareCustomerNbr");
        String merchantNbr = request.getParameter("merchantNbr");
        String platformType = request.getParameter("platformType");

        StaticLog.info("activityNbr:{}, platformType: {}", activityNbr, platformType);

        // 取得其他信息，标题和图片
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("activityNbr", activityNbr);
        paramMap.put("platformType", platformType);

        String respStr = HttpRequest.post("https://su.emgot.com/qbhbcustomerapi/share/activeDetail")
                .header("content-type", "application/x-www-form-urlencoded")
                .form(paramMap)
                .execute().body();

        StaticLog.info(respStr);

        ModelAndView modelAndView = new ModelAndView();

        JSONObject rootObject = JSON.parseObject(respStr);
        if (rootObject.get("code").equals("B0000")) {
            try {
                modelAndView.setViewName("goods_detail");

                JSONObject A7100 = (JSONObject) rootObject.getJSONObject("data").getJSONArray("A7100").get(0);

                GoodsDetail goodsDetail = null;
                // 包含unionInfo的为联盟电商的标准数据
                if (A7100.containsKey("unionInfo")) {
                    goodsDetail = A7100.getObject ("unionInfo", GoodsDetail.class);

                } else {
                    goodsDetail = new GoodsDetail();
                    goodsDetail.setTitle(A7100.getString("subjectName"));
                    goodsDetail.setPict_url(StrFormatter.format("https://su.emgot.com/{}", A7100.getString("attachmentPath")));

                    // 以下是为了配合自上架商品能显示
                    goodsDetail.setShop_title(null);
                    goodsDetail.setZk_final_price("0.00");
                    goodsDetail.setCoupon_amount("0");
                }
                modelAndView.addObject("detail", goodsDetail);

                modelAndView.addObject("activityNbr", activityNbr);
                modelAndView.addObject("customerNbr", shareCustomerNbr);
                modelAndView.addObject("openId", openId);
                modelAndView.addObject("merchantNbr", merchantNbr);
                modelAndView.addObject("platformType", platformType);

                String shareLink = StrUtil.format("emgot.com#{}#{}#{}#{}#{}", activityNbr, shareCustomerNbr, openId, merchantNbr, platformType);
                StaticLog.info(shareLink);
                modelAndView.addObject("shareLink", shareLink);

            } catch (Exception e) {
                e.printStackTrace();
                modelAndView.setViewName("error");
            }

        } else {
            modelAndView.setViewName("error");
        }

        return modelAndView;
    }


    @RequestMapping(value="/activityTip", method={RequestMethod.GET, RequestMethod.POST })
    public ModelAndView activityTip(HttpServletRequest request, HttpServletResponse response) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("activity_tip");
        return modelAndView;
    }

    @RequestMapping(value="/merchantShare", method={RequestMethod.GET, RequestMethod.POST })
    public ModelAndView merchantShare(HttpServletRequest request, HttpServletResponse response) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("merchant_share");

        String code = request.getParameter("code");
        String shareCode = StrUtil.format("*#*{}*#*", code);
        StaticLog.info("shareCode: {}", shareCode);
        modelAndView.addObject("shareCode", shareCode);
        return modelAndView;
    }

}
