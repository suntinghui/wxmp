package com.emgot.wxmp.util;

import javax.servlet.http.HttpServletRequest;

public class AppUtils {

    public static String getRootURL(HttpServletRequest request) {
        //return request.getScheme()+"://"+request.getServerName();
        return "https://"+request.getServerName();
    }

    public static String genServerURL(HttpServletRequest request, String path) {
        String url = getRootURL(request) + "/wx" + path;
        return url;
    }
}
