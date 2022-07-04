package com.emgot.wxmp.util;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.StaticLog;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JsUtils {
    public static String accessToken = null;
    public static String ticket = null;
    private static final String appid = "wx7a24fcce32609ee0";
    private static final String appsecret = "e7d190b98f5cd8d12e6d6c956256644f";

    public static Map sign(String url) {
        // https://developers.weixin.qq.com/miniprogram/dev/api-backend/open-api/access-token/auth.getAccessToken.html
        if (accessToken == null) {
            String sendUrl = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appid + "&secret=" + appsecret;
            String result = getHttpResult(sendUrl);
            JSONObject tokenJson = JSONObject.parseObject(result);
            accessToken = tokenJson.get("access_token").toString();
            StaticLog.info("toke={}", accessToken);
        }

        // https://developers.weixin.qq.com/doc/offiaccount/WeChat_Invoice/E_Invoice/Vendor_API_List.html
        if (ticket == null) {
            String signUrl = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=" + accessToken + "&type=jsapi";
            String resultSign = getHttpResult(signUrl);
            JSONObject tokenJson = JSONObject.parseObject(resultSign);
            ticket = tokenJson.get("ticket").toString();
        }
        Map<String, Object> ret = new HashMap();
        String nonce_str = create_nonce_str();
        String timestamp = create_timestamp();
        String signature = "";
        //注意这里参数名必须全部小写，且必须有序
        String source = StrUtil.format("jsapi_ticket={}&noncestr={}&timestamp={}&url={}", ticket, nonce_str, timestamp, url);
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(source.getBytes("UTF-8"));
            signature = HexUtil.encodeHexStr(crypt.digest(), true);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        ret.put("url", url);
        ret.put("nonceStr", nonce_str);
        ret.put("timestamp", Long.parseLong(timestamp));
        ret.put("signature", signature);
        ret.put("jsapi_ticket", ticket);
        ret.put("appId", appid);
        return ret;
    }

    /**
     * 获取访问地址链接返回值
     */
    private static String getHttpResult(String url) {
        String result = "";
        HttpGet httpRequest = new HttpGet(url);
        try {
            HttpResponse httpResponse = HttpClients.createDefault().execute(httpRequest);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                result = EntityUtils.toString(httpResponse.getEntity());
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            result = e.getMessage().toString();
        } catch (IOException e) {
            e.printStackTrace();
            result = e.getMessage().toString();
        }
        return result;
    }

    /**
     * 产生随机串--由程序自己随机产生
     *
     * @return
     */

    private static String create_nonce_str() {
        return UUID.randomUUID().toString();
    }

    /**
     * 由程序自己获取当前时间
     *
     * @return
     */
    private static String create_timestamp() {
        return Long.toString(System.currentTimeMillis() / 1000);
    }

}


