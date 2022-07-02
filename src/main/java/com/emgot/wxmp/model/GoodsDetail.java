package com.emgot.wxmp.model;

import cn.hutool.core.util.NumberUtil;

public class GoodsDetail {

    private String title;
    private String pict_url;
    private String coupon_amount;
    private String zk_final_price;
    private String volume;
    private String platform_type;
    private String shop_title;
    private String shop_type;
    private String shop_logo;
    private String reserve_price;

    private String imgs; // 轮播图
    private String detail_pics; // 商品详情图

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPict_url() {
        return pict_url;
    }

    public void setPict_url(String pict_url) {
        this.pict_url = pict_url;
    }

    public String getCoupon_amount() {
        return coupon_amount;
    }

    public void setCoupon_amount(String coupon_amount) {
        this.coupon_amount = coupon_amount;
    }

    public String getZk_final_price() {
        return zk_final_price;
    }

    public void setZk_final_price(String zk_final_price) {
        this.zk_final_price = zk_final_price;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        try {
            Long v = Long.parseLong(volume);
            if (v < 10000) {
                this.volume = volume;
            } else {
                this.volume = NumberUtil.decimalFormat("#.##万", v/10000.0);
            }
        } catch (Exception e) {
            this.volume = volume;
        }
    }

    public String getPlatform_type() {
        return platform_type;
    }

    public void setPlatform_type(String platform_type) {
        this.platform_type = platform_type;
    }

    public String getShop_title() {
        return shop_title;
    }

    public void setShop_title(String shop_title) {
        this.shop_title = shop_title;
    }

    public String getShop_type() {
        return shop_type;
    }

    public void setShop_type(String shop_type) {
        this.shop_type = shop_type;
        this.setShop_logo();
    }

    public String getShop_logo() {
        return shop_logo;
    }

    public void setShop_logo(String shop_logo) {
        this.shop_logo = shop_logo;
    }

    public void setShop_logo() {
        if (this.shop_type.equals("tb")) {
            shop_logo = "/wx/static/image/pf-tb.png";
        } else if (this.shop_type.equals("tm")) {
            shop_logo = "/wx/static/image/pf-tm.png";
        } else if (this.shop_type.equals("pdd")) {
            shop_logo = "/wx/static/image/pf-pdd.png";
        } else if (this.shop_type.equals("jd")) {
            shop_logo = "/wx/static/image/pf-jd.png";
        } else if (this.shop_type.equals("wph")) {
            shop_logo = "/wx/static/image/pf-wph.png";
        } else {
            shop_logo = "/wx/static/image/pf-tb.png";
        }
    }

    public String getReserve_price() {
        return reserve_price;
    }

    public void setReserve_price(String reserve_price) {
        this.reserve_price = reserve_price;
    }

    public String getImgs() {
        return imgs;
    }

    public void setImgs(String imgs) {
        this.imgs = imgs;
    }

    public String getDetail_pics() {
        return detail_pics;
    }

    public void setDetail_pics(String detail_pics) {
        this.detail_pics = detail_pics;
    }
}
