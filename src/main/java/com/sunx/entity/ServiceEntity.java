package com.sunx.entity;

import com.sunx.storage.annotation.Row;
import com.sunx.storage.annotation.Table;

/**
 * 服务实体类
 *
 * 对应数据库表中的名称为:service
 */
@Table(table = "service")
public class ServiceEntity {
    //自增唯一主键
    private Long id;
    //服务名称
    @Row(field = "sname")
    private String name;
    //服务地址
    private String service;
    //服务状态
    private Integer status;
    //服务创建时间
    private String createAt;
    //服务最近更新时间
    private String updateAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
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
}
