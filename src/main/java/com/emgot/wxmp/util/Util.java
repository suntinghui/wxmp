package com.emgot.wxmp.util;

import javax.servlet.http.HttpServletRequest;

public class Util {

    public static String getRootURL(HttpServletRequest request) {
        //return request.getScheme()+"://"+request.getServerName();
        return "http://"+request.getServerName();
    }

    public static String genServerURL(HttpServletRequest request, String path) {
        String url = getRootURL(request) + path;
        System.out.println(url);
        return url;
    }
}
