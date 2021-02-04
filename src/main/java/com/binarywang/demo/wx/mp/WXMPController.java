package com.binarywang.demo.wx.mp;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import me.chanjar.weixin.mp.bean.result.WxMpCurrentAutoReplyInfo;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.apache.http.client.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;

@Controller
@RequestMapping("/wxmp")
public class WXMPController {

    @Autowired
    private WxMpService mpService;

    @RequestMapping("/reg")
    public String reg() throws WxErrorException {
        return "index";
    }

    @RequestMapping("/bind")
    public String bind() throws WxErrorException {
        return "binduser";
    }

    /*
    https://github.com/Wechat-Group/WxJava/wiki/MP_%E4%B8%BB%E5%8A%A8%E5%8F%91%E9%80%81%E6%B6%88%E6%81%AF%EF%BC%88%E5%AE%A2%E6%9C%8D%E6%B6%88%E6%81%AF%EF%BC%89
     */
    @GetMapping("/nmsg")
    public String normarlMessage() throws WxErrorException {
        WxMpKefuMessage message = WxMpKefuMessage
                .TEXT()
                //.toUser("oVXxg6oI1ayqnvMdV39HSXCFyUvE") // dxp
                .toUser("oVXxg6kOUugk_A5qsHYCJ0sIoOBo") // sth
                .content("用户下单成功，订单号为111，请您及时处理。")
                .build();

        // 设置消息的内容等信息
        this.mpService.getKefuService().sendKefuMessage(message);
        return "info";
    }

    /*
    https://github.com/Wechat-Group/WxJava/wiki/MP_%E5%8F%91%E9%80%81%E6%A8%A1%E6%9D%BF%E6%B6%88%E6%81%AF
     */
    @GetMapping("/tmsg")
    public void templateMessage(HttpServletRequest request) throws WxErrorException {
        JSONObject jsonObject  = JSON.parseObject(request.getParameter("message"));

        WxMpTemplateMessage templateMessage = WxMpTemplateMessage.builder()
                .toUser((String)jsonObject .get("openId"))
                .templateId("W78fcPVwHaZC1S7nlCBS7E-NwVJKbyFdLxmRuShhW8k")
                .build();

        String url = (String) jsonObject.get("url");
        if (!StrUtil.isBlank(url)) {
            templateMessage.setUrl(url);
        }

        // https://github.com/Wechat-Group/WxJava/blob/develop/weixin-java-mp/src/test/java/me/chanjar/weixin/mp/api/impl/WxMpTemplateMsgServiceImplTest.java

        templateMessage.addData(new WxMpTemplateData("first", StrUtil.nullToDefault((String) jsonObject.get("first"), ""), "#000000"));

        // 审核内容
        templateMessage.addData(new WxMpTemplateData("keyword1", (String) jsonObject.get("info"), "#000000"));
        // 时间
        templateMessage.addData(new WxMpTemplateData("keyword2", DateUtil.formatDateTime(DateUtil.date())));

        templateMessage.addData(new WxMpTemplateData("remark", StrUtil.nullToDefault((String) jsonObject.get("remark"), ""), "#000000"));

        this.mpService.getTemplateMsgService().sendTemplateMsg(templateMessage);
    }

    @RequestMapping("/r")
    public void regBind(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String referrerNbr = request.getParameter("c");
        String redirectUri = Util.genServerURL(request, "/wxmp/baseOpenId")+"?referrerNbr="+referrerNbr;
        String url = this.mpService.getOAuth2Service().buildAuthorizationUrl(redirectUri, WxConsts.OAuth2Scope.SNSAPI_BASE, null);
        response.sendRedirect(url);
    }

    @RequestMapping("/baseOpenId")
    public void baseOpenId(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String code = request.getParameter("code");
        String referrerNbr = request.getParameter("referrerNbr");

        WxOAuth2AccessToken wxOAuth2AccessToken = this.mpService.getOAuth2Service().getAccessToken(code);
        System.out.println(JSON.toJSONString(wxOAuth2AccessToken));

        /*
        // 重新发送post请求
        RedirectWithPost redirectWithPost = new RedirectWithPost(response);
        String redirectUrl = regUrl;
        redirectWithPost.setParameter("openId", wxOAuth2AccessToken.getOpenId());
        redirectWithPost.sendByPost(redirectUrl);
         */

        HashMap<String, String> map = new HashMap<>();
        map.put("openid", wxOAuth2AccessToken.getOpenId());

        StringBuilder urlSb =  new StringBuilder("https://qbhb.emgot.com/qbhbcustomerapi");
        urlSb.append("?c=").append(referrerNbr);
        urlSb.append("&info=").append(URLEncoder.encode(JSON.toJSONString(map), "UTF-8"));
        System.out.println(urlSb.toString());
        response.sendRedirect(urlSb.toString());
    }

    /*
    https://github.com/Wechat-Group/WxJava/wiki/MP_OAuth2%E7%BD%91%E9%A1%B5%E6%8E%88%E6%9D%83
    通过网页授权拿到openid，然后打开注册页面，让用户输入手机号
    在后台将手机号与openid进行绑定
    后面的操作都需要通过手机号得到openid，以识别用户进行各种操作
     */
    @GetMapping("/auth")
    public void auth(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String redirectUri = Util.genServerURL(request, "/wxmp/callback");
        String url = this.mpService.getOAuth2Service().buildAuthorizationUrl(redirectUri, WxConsts.OAuth2Scope.SNSAPI_USERINFO, null);
        // https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx7a24fcce32609ee0&redirect_uri=http%3A%2F%2Fqbhb.emgot.com%2Fcallback&response_type=code&scope=snsapi_userinfo&state=&connect_redirect=1#wechat_redirect
        System.out.println(url);
        response.sendRedirect(url);
    }

    @GetMapping("/callback")
    public void callback(HttpServletRequest request, HttpServletResponse response) throws WxErrorException, IOException {
        String code = request.getParameter("code");
        System.out.println("获得Code：" + code);
        WxOAuth2AccessToken wxOAuth2AccessToken = this.mpService.getOAuth2Service().getAccessToken(code);
        // 获得用户基本信息
        WxOAuth2UserInfo userInfo = this.mpService.getOAuth2Service().getUserInfo(wxOAuth2AccessToken, null);

        System.out.println(JSON.toJSONString(userInfo));

        // 刷新token
        wxOAuth2AccessToken = this.mpService.getOAuth2Service().refreshAccessToken(wxOAuth2AccessToken.getRefreshToken());
        // 验证token
        boolean valid = this.mpService.getOAuth2Service().validateAccessToken(wxOAuth2AccessToken);
        if (valid) {
            System.out.println("Token验证通过");
        } else {
            System.out.println("Token验证失败");
        }
        String info = URLEncoder.encode(JSON.toJSONString(userInfo), "UTF-8");
        System.out.println(info);
        //String url = "http://gotit.xiaomiqiu.com/qbhbcustomerapi/3?info="+info;
        String url = "https://qbhb.emgot.com/qbhbcustomerapi/3?info="+info;
        response.sendRedirect(url);
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
        System.out.println(url);
        response.sendRedirect(url);
    }

    @GetMapping("/callback2")
    public void callback2(HttpServletRequest request, HttpServletResponse response) throws WxErrorException, IOException {
        String code = request.getParameter("code");
        System.out.println("获得Code：" + code);
        WxOAuth2AccessToken wxOAuth2AccessToken = this.mpService.getOAuth2Service().getAccessToken(code);
        // 获得用户基本信息
        WxOAuth2UserInfo userInfo = this.mpService.getOAuth2Service().getUserInfo(wxOAuth2AccessToken, null);

        System.out.println(JSON.toJSONString(userInfo));

        // 验证token
        boolean valid = this.mpService.getOAuth2Service().validateAccessToken(wxOAuth2AccessToken);
        if (valid) {
            System.out.println("Token验证通过");
        } else {
            System.out.println("Token验证失败");
        }

        StringBuilder url = new StringBuilder("https://qbhb.emgot.com/emgot/index.html?");
        url.append("activityNbr=").append(request.getParameter("activityNbr")).append("&")
                .append("shareCustomerNbr=").append(request.getParameter("shareCustomerNbr")).append("&")
                .append("mobileNbr=").append(request.getParameter("mobileNbr")).append("&")
                .append("info=").append(JSON.toJSONString(userInfo));

        response.sendRedirect(url.toString());
    }


}