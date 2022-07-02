package com.emgot.wxmp.controller;

// 微信开放平台网站应用

import cn.hutool.core.text.StrFormatter;
import cn.hutool.log.StaticLog;
import com.emgot.wxmp.util.AppUtils;
import com.emgot.wxmp.util.JsUtils;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/auth")
public class AuthController {

    /*

    https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/Wechat_webpage_authorization.html

     */

    @Autowired
    private WxMpService mpService;

    @RequestMapping(value = "/bind", method={RequestMethod.GET, RequestMethod.POST })
    public void bind(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String merCode = request.getParameter("merCode");
        StaticLog.info("merCode: {}", merCode);

        String redirectUri = AppUtils.genServerURL(request, "/auth/bind2");
        redirectUri = StrFormatter.format("{}?merCode={}", redirectUri, merCode);
        StaticLog.info(redirectUri);

        String url = this.mpService.getOAuth2Service().buildAuthorizationUrl(redirectUri, WxConsts.OAuth2Scope.SNSAPI_USERINFO, null);
        StaticLog.info(url);
        response.sendRedirect(url);
    }

    // 通过上面的wxUserInfo方法发起调用
    @RequestMapping(value="/bind2", method={RequestMethod.GET, RequestMethod.POST })
    public ModelAndView bind2(HttpServletRequest request, HttpServletResponse response) throws WxErrorException, IOException {
        // 取得code
        String code = request.getParameter("code");
        StaticLog.info("code:{}",code);
        // 使用code换得openId
        WxOAuth2AccessToken wxOAuth2AccessToken = this.mpService.getOAuth2Service().getAccessToken(request.getParameter("code"));
        String openId = wxOAuth2AccessToken.getOpenId();
        String unionId = wxOAuth2AccessToken.getUnionId();
        StaticLog.info("openId:{}, unionId:{}",openId, unionId);

        String merCode = request.getParameter("merCode");

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("auth");
        modelAndView.addObject("merCode", merCode);
        modelAndView.addObject("openId", openId);
        modelAndView.addObject("unionId", unionId);

        return modelAndView;
    }

    /*
    https://www.jianshu.com/p/7d2a163df8d1

    1、开发工具调试：
    开发工具的调试，不要在开发工具中直接操作，这样即便代码写的没问题也会报错；用手机扫描该页面的二维码进行调试就行；

    2、真机调试
    在真机微信端测试时，要通过点击卡片、扫码等形式进入，不能直接通过url地址进入网页，否则也是会一直报莫名其妙的错误的！

    3、开放标签的动态隐藏
    对开放标签做v-if或v-show的隐藏要非常注意。因为开放标签和config的验证是同步进行的，不能先config验证是否成功，
    再判断是否显示开放标签，这样做，即便config验证通过，开放标签页不能显示；

    4、在uniapp或者vue相关框架中报警告
    开放标签属于自定义标签，Vue会给予未知标签的警告，可通过配置Vue.config.ignoredElements来忽略Vue对开放标签的检查。
     */

    @RequestMapping(value = "/getSign")
    @ResponseBody
    public Map getSign(HttpServletRequest request, String url) throws Exception {
        String Url=request.getRequestURL().toString();
        StaticLog.info("请求的url={}",Url);
        Map resMap = new HashMap();
        resMap = JsUtils.sign(url);
        StaticLog.info("map参数:{}",resMap);
        return resMap;

    }

}
