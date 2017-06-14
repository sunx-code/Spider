package com.sunx.entity;

import com.sunx.storage.annotation.Row;
import com.sunx.storage.annotation.Table;

/**
 * 任务实体类
 *
 */
@Table(table = "task")
public class TaskEntity {
    //自增id
    private Long id;
    //渠道id
    @Row(field = "cid")
    private Long channelId;
    //渠道名称
    @Row(field = "sname")
    private String channelName;
    //地区
    private String region;
    //地区参数
    @Row(field = "region_param")
    private String regionParam;
    //入住日期
    @Row(field = "check_in_date")
    private String checkInDate;
    //离开时间
    @Row(field = "check_out_date")
    private String checkOutDate;
    //入住晚数
    private Integer sleep;
    //成人数
    @Row(field = "adult_num")
    private Integer adultNum;
    //儿童数
    @Row(field = "child_num")
    private Integer childNum;
    //人员类型
    @Row(field = "people_type")
    private String peopleType;
    //商品链接地址
    private String url;
    //商品title
    private String title;
    //店铺名称
    @Row(field = "shop_name")
    private String shopName;
    //店铺链接地址
    @Row(field = "shop_url")
    private String shopUrl;
    //任务状态
    private Integer status;
    //创建时间
    private String createAt;
    //更新时间
    private String updateAt;
    //重试次数
    @Row(field = "try_time")
    private Integer tryTime;
    //儿童生日
    private String birthday;
    //异常原因,在处理数据的时候进行更新
    private String exception;
    //类型
    @Row(field = "package_type")
    private String type;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getChannelId() {
        return channelId;
    }

    public void setChannelId(long channelId) {
        this.channelId = channelId;
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

    public String getRegionParam() {
        return regionParam;
    }

    public void setRegionParam(String regionParam) {
        this.regionParam = regionParam;
    }

    public String getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }

    public int getSleep() {
        return sleep;
    }

    public void setSleep(int sleep) {
        this.sleep = sleep;
    }

    public int getAdultNum() {
        return adultNum;
    }

    public void setAdultNum(int adultNum) {
        this.adultNum = adultNum;
    }

    public int getChildNum() {
        return childNum;
    }

    public void setChildNum(int childNum) {
        this.childNum = childNum;
    }

    public String getPeopleType() {
        return peopleType;
    }

    public void setPeopleType(String peopleType) {
        this.peopleType = peopleType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getShopUrl() {
        return shopUrl;
    }

    public void setShopUrl(String shopUrl) {
        this.shopUrl = shopUrl;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCreateAt() {
        return createAt;
    }

    public void setCreateAt(String createAt) {
        this.createAt = createAt;
    }

    public String getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(String updateAt) {
        this.updateAt = updateAt;
    }

    public int getTryTime() {
        return tryTime;
    }

    public void setTryTime(int tryTime) {
        this.tryTime = tryTime;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public String getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(String checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public void setSleep(Integer sleep) {
        this.sleep = sleep;
    }

    public void setAdultNum(Integer adultNum) {
        this.adultNum = adultNum;
    }

    public void setChildNum(Integer childNum) {
        this.childNum = childNum;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setTryTime(Integer tryTime) {
        this.tryTime = tryTime;
    }
}
