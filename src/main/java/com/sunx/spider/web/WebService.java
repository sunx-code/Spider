package com.sunx.spider.web;

import com.sunx.constant.Configuration;
import com.sunx.constant.Constant;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

/**
 * web服务
 */
public class WebService {


    public static void main(String[] args) throws Exception {
        new WebService().start();
    }

    public void start() throws Exception{
        Server server = new Server(8080);

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setResourceBase(Configuration.me().getString(Constant.DEFAULT_IMG_SAVE));

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { resourceHandler, new DefaultHandler() });
        server.setHandler(handlers);

        server.start();
        server.join();
    }
}