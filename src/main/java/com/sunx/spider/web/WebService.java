package com.sunx.spider.web;

import com.sunx.constant.Configuration;
import com.sunx.moudle.enums.ImageType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.http.HttpContent;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.URIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Enumeration;

public class WebService {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebService.class);
	private Server server = new Server();

	public static void main(String[] args) {
		WebService web = new WebService();
		web.start();
	}

	public void start() {
		server.setStopAtShutdown(true);
		// -------------------------------
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(Configuration.me().getInt("web.port"));
		connector.setReuseAddress(false);
		server.setConnectors(new Connector[] { connector });

		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setDirectoriesListed(true);
		resourceHandler.setResourceBase(Configuration.me().getString("img.save.path"));

		MimeTypes mimeTypes = new MimeTypes();
		mimeTypes.addMimeMapping("*", MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
		mimeTypes.addMimeMapping("md", MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
		mimeTypes.addMimeMapping("txt", MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
		mimeTypes.addMimeMapping(ImageType.BASE64.getValue(), MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());

		resourceHandler.setMimeTypes(mimeTypes);

		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] { resourceHandler, new DefaultHandler() });
		server.setHandler(handlers);
		try {
			server.start();
		} catch (Exception e) {
			LOGGER.error(ExceptionUtils.getStackTrace(e));
			System.exit(-1);
		}
		LOGGER.info("Web Server Start.");
	}
}
