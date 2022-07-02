package com.emgot.wxmp.controller;

import cn.hutool.log.StaticLog;
import com.alibaba.fastjson.JSON;
import com.emgot.wxmp.util.AppUtils;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.bean.menu.WxMenu;
import me.chanjar.weixin.common.bean.menu.WxMenuButton;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.material.WxMpMaterialFileBatchGetResult;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutNewsMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wxmp")
public class InitMpController {
    @Autowired
    private WxMpService mpService;

    protected WxMpMessageRouter wxMpMessageRouter;

    @GetMapping("/test")
    public String test() throws WxErrorException {

        StaticLog.info("test……");

        try {
            String accessToken = this.mpService.getAccessToken();
            StaticLog.info("accessToken is: {}", accessToken);
            return accessToken;
        } catch (Exception e){
            return "没有取到accessToken";
        }
    }

    // 创建菜单
    @GetMapping("/menu")
    public String menu(HttpServletRequest request) throws WxErrorException {
        WxMenu wxMenu = new WxMenu();

        WxMenuButton btn1 = new WxMenuButton();
        btn1.setKey("1");
        btn1.setName("账户");
        btn1.setUrl(AppUtils.genServerURL(request, "/wxmp/bindUser"));
        btn1.setType("view");

        /////////////////////////////
        WxMenuButton btn2 = new WxMenuButton();
        btn2.setKey("2");
        btn2.setName("下载应用");

        WxMenuButton btn21 = new WxMenuButton();
        btn21.setKey("21");
        btn21.setName("下载惠众宝");
        /*
        btn21.setMediaId("7Ds_vWDdW2Qrk6Mql4DJ1vbWaLBLoloSSL56OH2X_Og");
        btn21.setType("media_id");
         */
        btn21.setUrl("https://a.app.qq.com/o/simple.jsp?pkgname=com.cfastech.yonghu");
        btn21.setType("view");

        WxMenuButton btn22 = new WxMenuButton();
        btn22.setKey("22");
        btn22.setName("下载易商小店");
        /*
        btn22.setMediaId("7Ds_vWDdW2Qrk6Mql4DJ1gHXX_RVXgPHlzQ9RLECUm4");
        btn22.setType("media_id");
         */
        btn22.setUrl("https://a.app.qq.com/o/simple.jsp?pkgname=com.cfastech.mendian");
        btn22.setType("view");

        btn2.setSubButtons(Arrays.asList(btn21, btn22) );

        /////////////////////////////

        WxMenuButton btn3 = new WxMenuButton();
        btn3.setKey("3");
        btn3.setName("口令红包");

        WxMenuButton btn31 = new WxMenuButton();
        btn31.setKey("31");
        btn31.setName("领取红包");
        btn31.setUrl("https://su.emgot.com/getBonus.html");
        btn31.setType("view");

        WxMenuButton btn32 = new WxMenuButton();
        btn32.setKey("32");
        btn32.setName("领取步骤");
        btn32.setUrl("https://mp.weixin.qq.com/s/WFkboW1tlATFW1LaMcrK_Q");
        btn32.setType("view");

        btn3.setSubButtons(Arrays.asList(btn31, btn32) );

        /////////////////////////////


        wxMenu.setButtons(Arrays.asList(btn1, btn2, btn3));
        // 设置菜单
        this.mpService.getMenuService().menuCreate(wxMenu);

        StaticLog.info("菜单创建完成");

        return "菜单创建完成";
    }

    // 查询素材 https://github.com/Wechat-Group/WxJava/blob/develop/weixin-java-mp/src/test/java/me/chanjar/weixin/mp/api/impl/WxMpMaterialServiceImplTest.java
    @RequestMapping("/getMediaList")
    public void getMediaList() throws WxErrorException {
        WxMpMaterialFileBatchGetResult wxMpMaterialImageBatchGetResult = this.mpService.getMaterialService().materialFileBatchGet(WxConsts.MaterialType.IMAGE, 0, 20);
        List<WxMpMaterialFileBatchGetResult.WxMaterialFileBatchGetNewsItem> list = wxMpMaterialImageBatchGetResult.getItems();
        StaticLog.info(JSON.toJSONString(list));
    }

    // 微信管理后台设置时验证基本信息
    @RequestMapping("/verify")
    public void verify(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 消息验证
        String msgSignature = request.getParameter("signature");
        String msgTimestamp = request.getParameter("timestamp");
        String msgNonce = request.getParameter("nonce");

        StaticLog.info("msgSignature: {}", msgSignature);
        StaticLog.info("msgTimestamp: {}", msgTimestamp);
        StaticLog.info("msgNonce: {}", msgNonce);

        if (!this.mpService.checkSignature(msgTimestamp,msgNonce,msgSignature)) {
            response.getWriter().println("非法请求");
            return;
        }

        String echostr = request.getParameter("echostr");
        if (StringUtils.isNotBlank(echostr)){
            // 说明是一个仅仅用来验证的请求，回显echostr
            response.getWriter().println(echostr);
            return;
        }

        // 创建Router
        this.buildRouter();

        String encryptType = StringUtils.isBlank(request.getParameter("encrypt_type")) ? "raw" : request.getParameter("encrypt_type");

        if ("raw".equals(encryptType)) {
            // 明文传输的消息
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(request.getInputStream());
            WxMpXmlOutMessage outMessage = wxMpMessageRouter.route(inMessage);
            if(outMessage == null) {
                //为null，说明路由配置有问题，需要注意
                response.getWriter().write("");
            }
            response.getWriter().write(outMessage.toXml());
            StaticLog.info(outMessage.toXml());
            return;
        }

        if ("aes".equals(encryptType)) {

            // 是aes加密的消息
            msgSignature = request.getParameter("msg_signature");
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromEncryptedXml(request.getInputStream(), this.mpService.getWxMpConfigStorage(), msgTimestamp, msgNonce, msgSignature);
            WxMpXmlOutMessage outMessage = wxMpMessageRouter.route(inMessage);
            if(outMessage == null) {
                //为null，说明路由配置有问题，需要注意
                response.getWriter().write("");
                return;
            }

            String exml = outMessage.toEncryptedXml(this.mpService.getWxMpConfigStorage());
            response.getWriter().write(exml);
            return;
        }

        response.getWriter().println("不可识别的加密类型");

    }

    /*
    配置路由规则时要按照从细到粗的原则，否则可能消息可能会被提前处理
    规则的结束必须用Rule.end()或者Rule.next()，否则不会生效
    msgType  event  eventKey  content
     */
    private void buildRouter() {
        wxMpMessageRouter = new WxMpMessageRouter(this.mpService);
        wxMpMessageRouter
                // 用户关注公众号的响应
                .rule()
                .async(false)
                .msgType(WxConsts.XmlMsgType.EVENT)
                .event(WxConsts.EventType.SUBSCRIBE)
                .handler(this.createSubscribeHandler())
                .end()

                // 发送关键词"省钱"的响应
                .rule()
                .msgType(WxConsts.XmlMsgType.TEXT)
                .async(false)
                .content("省钱")
                .handler(this.createHandler1())
                .end()

                // 其他动作忽略
                .rule()
                .async(false)
                .handler(this.createDefaultHandler()) // 匹配任意内容
                .end();
    }

    // 用户关注公众号的响应
    private WxMpMessageHandler createSubscribeHandler() {
        String content = "欢迎关注易商惠众\n\n" +
                "网购下单[礼物]单单省[强]\n\n" +
                "分享好物[庆祝]笔笔赚[强]\n\n" +
                "<a href=\"https://su.emgot.com/wx/wxmp/bindUser/ \">>>>戳此绑定手机号，及时接收【收益&红包】发放通知。</a>";

        return new WxMpMessageHandler() {
            @Override
            public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage, Map<String, Object> context, WxMpService wxMpService, WxSessionManager sessionManager) throws WxErrorException {
                return WxMpXmlOutMessage
                        .TEXT()
                        .content(content)
                        .fromUser(wxMessage.getToUser())
                        .toUser(wxMessage.getFromUser())
                        .build();
            }
        };
    }

    // 用户发送关键词"省钱"的响应
    private WxMpMessageHandler createHandler1() {
        WxMpXmlOutNewsMessage.Item item = new WxMpXmlOutNewsMessage.Item();
        item.setDescription("学会这一招，一年网购少花3600元！");
        item.setPicUrl("https://mmbiz.qpic.cn/mmbiz_png/fujSZJBmrictFjnNFL7kBatNFOtKvyN1lXEhxd89hlibAXibFnkscpicqINAJZSKAL2DYvsiaJXzM5eXlSZkkFwnI6A/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1");
        item.setTitle("一年网购花不少，手把手教你网购更省钱！");
        item.setUrl("https://mp.weixin.qq.com/s/Z0WpI70fm2qwoHUAE-Ys_g");

        return new WxMpMessageHandler() {
            @Override
            public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage, Map<String, Object> context, WxMpService wxMpService, WxSessionManager sessionManager) throws WxErrorException {
                return WxMpXmlOutMessage.NEWS()
                        .addArticle(item)
                        .fromUser(wxMessage.getToUser())
                        .toUser(wxMessage.getFromUser())
                        .build();
            }
        };
    }

    private WxMpMessageHandler createDefaultHandler() {
        // 不回复内容
        return null;
    }


}
