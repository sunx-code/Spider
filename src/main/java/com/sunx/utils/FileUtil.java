package com.sunx.utils;

import com.sunx.constant.Constant;
import com.sunx.moudle.enums.ImageType;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.IOException;

public class FileUtil {
	/**
	 * 创建文件
	 * @param vday
	 * @param channel
	 * @param region
	 * @param checkInDate
	 * @param md5
	 * @param imageType
     * @return
     */
	public static String createPageFile(String vday, String channel, String region,String checkInDate, String md5, ImageType imageType) {
		File parent_path = new File(Constant.DEFAULT_IMG_SAVE + File.separator
				+ vday + File.separator + channel + File.separator + region);
		if (!parent_path.exists())
			parent_path.mkdirs();

		String file_path = checkInDate + "_" + md5 + "." + imageType.getValue();
		File file = new File(parent_path, file_path);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return file.getPath();
	}

	/**
	 * 获取截图对应的byte数组
	 * @param driver
	 * @return
     */
	public static byte[] getScreenshot(RemoteWebDriver driver) {
		return driver.getScreenshotAs(OutputType.BYTES);
	}
}
