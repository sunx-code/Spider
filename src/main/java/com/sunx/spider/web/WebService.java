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
		// -------------------------------
//		WebAppContext webContext = new WebAppContext();
//		webContext.setExtractWAR(false);
//		webContext.setParentLoaderPriority(true);
//		webContext.setConfigurationDiscovered(true);
////		webContext.setContextPath(Configuration.me().getString("web.path"));
//		webContext.setClassLoader(Thread.currentThread().getContextClassLoader());
//		webContext.setResourceBase(Configuration.me().getString("web.path"));
//		webContext.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");
//		ArrayList<DispatcherType> list = Lists.newArrayList(DispatcherType.values());
//		webContext.addFilter(WebFilter.class, "/*",
//				EnumSet.of(list.remove(0), list.toArray(new DispatcherType[list.size()])));
//		webContext.addServlet(WebDefaultServlet.class, "/*");

		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setDirectoriesListed(true);
		resourceHandler.setResourceBase("/");//Configuration.me().getString(Constant.DEFAULT_IMG_SAVE)

		MimeTypes mimeTypes = new MimeTypes();
		resourceHandler.setMimeTypes(mimeTypes);
		mimeTypes.addMimeMapping("*", MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
		mimeTypes.addMimeMapping("md", MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
		mimeTypes.addMimeMapping("txt", MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
		mimeTypes.addMimeMapping(ImageType.BASE64.getValue(), MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());

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

	public static class WebFilter implements Filter {
		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			chain.doFilter(request, response);
		}
		@Override
		public void destroy() {
		}

	}

	public static class WebDefaultServlet extends DefaultServlet {
		private static final long serialVersionUID = 1L;

		public WebDefaultServlet() {
			super(new WebResourceService());
		}
	}

	public static class WebResourceService extends ResourceService {

		@Override
		public void doGet(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			String servletPath = request.getServletPath();
			String pathInfo = request.getPathInfo();
			String pathInContext = URIUtil.addPaths(servletPath, pathInfo);
			if(pathInContext != null && !pathInContext.contains("/pending") && pathInContext.toLowerCase().endsWith(".base64")){
				pathInContext = "/pending" + pathInContext;
			}
			LOGGER.info(pathInContext);
			if (StringUtils.endsWithIgnoreCase(pathInContext, ImageType.PNG.getValue())
					|| StringUtils.endsWithIgnoreCase(pathInContext, ImageType.TXT.getValue())) {
				HttpContent content_tmp = getContentFactory().getContent(pathInContext, response.getBufferSize());
				if (content_tmp == null
						&& !StringUtils.startsWithIgnoreCase(pathInContext, Configuration.me().getString("web.pending"))) {
					servletPath = Configuration.me().getString("web.pending") + pathInfo;
					request.getRequestDispatcher(servletPath).forward(request, response);
					return;
				}
			} else if(StringUtils.endsWithIgnoreCase(pathInContext, ImageType.BASE64.getValue())){
				//获取文件名称,通过base64的路径,创建png文件,并返回png文件路径地址
				String png = create(request.getRealPath("/"),pathInContext,response.getBufferSize());
				if(png != null && png.length() > 0){
					String path = URLEncoder.encode(png,"UTF-8").replaceAll("%2F","/");
					response.sendRedirect(path.replaceAll(" ","%20").replaceAll("\\+","%20"));
					return;
				}
			} else {
				request.getSession(true);
			}
			super.doGet(request, response);
		}

		@Override
		protected boolean sendData(HttpServletRequest request, HttpServletResponse response, boolean include,
				HttpContent content, Enumeration<String> reqRanges) throws IOException {
			 return super.sendData(request, response, include, content, reqRanges);
		}

		/**
		 * 构建png文件
		 * @param path
		 */
		private String create(String base,String path,int bufferSize){
			//png图片地址
			String fileName = path.substring(0,path.lastIndexOf(".")) + ".png";
			//如果文件已经存在了
			if(exits(base + fileName))return fileName;
			//获取base64源码
			String baseStr = reader(path,bufferSize);
			//对数据进行判定
			if(baseStr == null || baseStr.length() <= 0)return null;
			//构建文件
			return newFile(base,fileName,baseStr);
		}

		/**
		 * 读取base64文件内容
		 * @param path
		 * @param bufferSize
         * @return
         */
		private String reader(String path,int bufferSize){
			try{
				HttpContent content = getContentFactory().getContent(path, bufferSize);
				return new String(IOUtils.toByteArray(content.getInputStream()));
			}catch (Exception e){
				e.printStackTrace();
			}
			return null;
		}

		/**
		 * 将base64字节码转成图片
		 * @param fileName
		 * @param imgStr
         * @return
         */
		private String newFile(String base,String fileName,String imgStr){
			//对字节数组字符串进行Base64解码并生成图片
			if (imgStr == null)return fileName;
			try {
				//Base64解码
				byte[] b = Base64.getMimeDecoder().decode(imgStr);
				for(int i=0;i<b.length;++i) {
					if(b[i]<0) {//调整异常数据
						b[i]+=256;
					}
				}
				//生成png图片
				IOUtils.write(b,new FileOutputStream(base + fileName));
			} catch (Exception e) {
				e.printStackTrace();
			}
			return fileName;
		}

		/**
		 * 判断文件是否存在
		 * @param path
         * @return
         */
		public boolean exits(String path){
			try{
				File file = new File(path);
				return file.exists();
			}catch (Exception e){
				e.printStackTrace();
			}
			return false;
		}
	}
}
