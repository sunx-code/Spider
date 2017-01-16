package com.sunx.moudle.enums;

/**
 * 图片类型
 */
public enum ImageType {
    PNG("png"), TXT("txt"), BASE64("base64"),;

    private String name;

    ImageType(String name) {
        this.name = name;
    }

    public String getValue() {
        return name;
    }
}
