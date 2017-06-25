package com.sunx.entity;

import com.sunx.constant.Configuration;
import com.sunx.storage.annotation.Row;
import com.sunx.storage.annotation.Table;

/**
 * 结果存储对象
 * 1
 */
@Table(table = "clubmed_pricemonitor.result")
public class ResultEntity {
    //自增id
    @Row(field = "auto_id")
    private long autoId;
    //id md5加密结果
    private String id;
    //抓取日期
    @Row(field = "crawling_date")
    private String vday;
    //任务id
    private Long tid;
    //渠道名称
    @Row(field = "channel")
    private String channelName;
    //地区
    private String region;
    //成人数
    @Row(field = "adult_num")
    private Integer adultNum;
    //儿童数
    @Row(field = "child_num")
    private Integer childNum;
    //人员类型
    @Row(field = "people_type")
    private String peopleType;
    //房型
    @Row(field = "house_type")
    private String houseType;
    //入住日期
    @Row(field = "check_in_date")
    private String checkInDate;
    //链接地址
    private String url;
    //path
    private String path;
    //入住晚数
    private int sleep;
    //类型
    @Row(field = "package_type")
    private String type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVday() {
        return vday;
    }

    public void setVday(String vday) {
        this.vday = vday;
    }

    public Long getTid() {
        return tid;
    }

    public void setTid(Long tid) {
        this.tid = tid;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Integer getAdultNum() {
        return adultNum;
    }

    public void setAdultNum(Integer adultNum) {
        this.adultNum = adultNum;
    }

    public Integer getChildNum() {
        return childNum;
    }

    public void setChildNum(Integer childNum) {
        this.childNum = childNum;
    }

    public String getPeopleType() {
        return peopleType;
    }

    public void setPeopleType(String peopleType) {
        this.peopleType = peopleType;
    }

    public String getHouseType() {
        return houseType;
    }

    public void setHouseType(String houseType) {
        this.houseType = houseType;
    }

    public String getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        path = path.replaceAll(Configuration.me().getString("img.save.path"),
                "http://" + Configuration.me().host() + ":" + Configuration.me().getString("web.port"));
        this.path = path;
    }

    public int getSleep() {
        return sleep;
    }

    public void setSleep(int sleep) {
        this.sleep = sleep;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getAutoId() {
        return autoId;
    }

    public void setAutoId(long autoId) {
        this.autoId = autoId;
    }
}
