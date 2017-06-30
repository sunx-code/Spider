package com.sunx.moudle.proxy;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class IProxy {
    private String host;
    private int port;

    private long createAt;
    private int cnt;
    private boolean flag = true;

    private Map<Long,Long> use = new HashMap<>();

    public IProxy(){}

    public IProxy(String host,int port){
        this.host = host;
        this.port = port;
    }

    public IProxy(String host,int port,long createAt,int cnt){
        this.host = host;
        this.port = port;
        this.createAt = createAt;
        this.cnt = cnt;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getCnt() {
        return cnt;
    }

    public long getCreateAt() {
        return createAt;
    }

    public void setCnt(int cnt) {
        this.cnt = cnt;
    }

    public void setCreateAt(long createAt) {
        this.createAt = createAt;
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public void put(long key,long time){
        use.put(key,time);
    }

    public long get(long key){
        if(!use.containsKey(key)){
            put(key,0l);
        }
        return use.get(key);
    }

    public boolean contains(long cid){
        if(cid < 0)return false;
        if(use.containsKey(cid))return true;
        return false;
    }
}
