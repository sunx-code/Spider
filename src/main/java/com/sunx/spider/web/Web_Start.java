//package com.fosun.fonova.spider.server;
//
//import java.io.*;
//import java.lang.management.ManagementFactory;
//import java.net.URLEncoder;
//import java.util.ArrayList;
//import java.util.EnumSet;
//import java.util.Enumeration;
//
//import javax.management.InstanceAlreadyExistsException;
//import javax.management.MBeanRegistrationException;
//import javax.management.MBeanServer;
//import javax.management.MalformedObjectNameException;
//import javax.management.NotCompliantMBeanException;
//import javax.management.ObjectName;
//import javax.management.StandardMBean;
//import javax.servlet.DispatcherType;
//import javax.servlet.Filter;
//import javax.servlet.FilterChain;
//import javax.servlet.FilterConfig;
//import javax.servlet.ServletException;
//import javax.servlet.ServletRequest;
//import javax.servlet.ServletResponse;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import org.apache.commons.io.IOUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.commons.lang3.exception.ExceptionUtils;
//import org.apache.log4j.PropertyConfigurator;
//import org.eclipse.jetty.http.HttpContent;
//import org.eclipse.jetty.http.MimeTypes;
//import org.eclipse.jetty.server.Connector;
//import org.eclipse.jetty.server.ResourceService;
//import org.eclipse.jetty.server.Server;
//import org.eclipse.jetty.server.ServerConnector;
//import org.eclipse.jetty.servlet.DefaultServlet;
//import org.eclipse.jetty.util.URIUtil;
//import org.eclipse.jetty.webapp.WebAppContext;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.fosun.fonova.utils.Base64Util;
//import com.fosun.fonova.utils.ConfUtil;
//import com.fosun.fonova.utils.FileUtil;
//import com.fosun.fonova.utils.ImageUtil;
//import com.fosun.fonova.utils.ImageUtil.ImageType;
//import com.google.common.collect.Lists;
//
//public class Web_Start extends StandardMBean implements AppMBean {
//
//	static {
//		PropertyConfigurator.configureAndWatch(
//				Thread.currentThread().getContextClassLoader().getResource("web_log4j.properties").getFile(), 5000);
//	}
//	private static final Logger LOGGER = LoggerFactory.getLogger(AppMBean.class);
//
//	static final String OBJECT_NAME = "com.fosun.fonova.hawk:name=web";
//
//	public static void main(String[] args) {
//
//		try {
//			Web_Start web = new Web_Start();
//
//			MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
//			ObjectName objectName = new ObjectName(OBJECT_NAME);
//			mBeanServer.registerMBean(web, objectName);
////			JMXUtil.registerToProxyServerByMBeanServer(ConfUtil.getWebJMXPort(), mBeanServer, "web");
//			web.start();
//		} catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException
//				| NotCompliantMBeanException e) {
//			LOGGER.error(ExceptionUtils.getStackTrace(e));
//		}
//
//	}
//
//	protected Web_Start() {
//		super(AppMBean.class, true);
//	}
//
//	private Server server = new Server();
//
//	@Override
//	public void start() {
//
//		synchronized (server) {
//			if (server.isStarted() || server.isStarting()) {
//				LOGGER.info("Web Server is already started.");
//				return;
//			}
//		}
//		// -------------------------------
//
//		server.setStopAtShutdown(true);
//		// -------------------------------
//		ServerConnector connector = new ServerConnector(server);
//		connector.setPort(Integer.parseInt(ConfUtil.getWebPort()));
//		connector.setReuseAddress(false);
//		server.setConnectors(new Connector[] { connector });
//		// -------------------------------
//		WebAppContext webContext = new WebAppContext();
//		webContext.setExtractWAR(false);
//		webContext.setParentLoaderPriority(true);
//		webContext.setConfigurationDiscovered(true);
//		webContext.setContextPath(ConfUtil.getWebAppPath());
//		webContext.setClassLoader(Thread.currentThread().getContextClassLoader());
//		webContext.setResourceBase("webapp");
//		{
//			webContext.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");
//		}
//		{
//			ArrayList<DispatcherType> list = Lists.newArrayList(DispatcherType.values());
//			webContext.addFilter(WebFilter.class, "/*",
//					EnumSet.of(list.remove(0), list.toArray(new DispatcherType[list.size()])));
//			webContext.addServlet(WebDefaultServlet.class, "/*");
//		}
//		{
//			MimeTypes mimeTypes = new MimeTypes();
//			webContext.setMimeTypes(mimeTypes);
//			mimeTypes.addMimeMapping("*", MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
//			mimeTypes.addMimeMapping("md", MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
//			mimeTypes.addMimeMapping("txt", MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
//			mimeTypes.addMimeMapping(ImageType.BASE64.getValue(), MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
//		}
//		server.setHandler(webContext);
//
//		try {
//			server.start();
//			// server.join();
//		} catch (Exception e) {
//			LOGGER.error(ExceptionUtils.getStackTrace(e));
//			System.exit(-1);
//		}
//		LOGGER.info("Web Server Start.");
//	}
//
//	@Override
//	public void stop() {
//		synchronized (server) {
//			if (server.isStopped() || server.isStopping())
//				return;
//			try {
//				server.stop();
//			} catch (Exception e) {
//				LOGGER.error(ExceptionUtils.getStackTrace(e));
//			}
//			LOGGER.info("Web Server Stop.");
//		}
//		new Thread(() -> {
//			synchronized (server) {
//				try {
//					server.wait(100l);
//				} catch (Exception e) {
//					LOGGER.error(ExceptionUtils.getStackTrace(e));
//				}
//				System.exit(0);
//			}
//		}).start();
//	}
//
//	public static class WebFilter implements Filter {
//
//		@Override
//		public void init(FilterConfig filterConfig) throws ServletException {
//		}
//
//		@Override
//		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//				throws IOException, ServletException {
//			// default
//			// {
//			// response.setCharacterEncoding("UTF-8");
//			// response.setContentType("text/html;charset=UTF-8");
//			// }
//			chain.doFilter(request, response);
//		}
//
//		@Override
//		public void destroy() {
//		}
//
//	}
//
//	public static class WebDefaultServlet extends DefaultServlet {
//		private static final long serialVersionUID = 1L;
//
//		public WebDefaultServlet() {
//			super(new WebResourceService());
//		}
//	}
//
//	public static class WebResourceService extends ResourceService {
//
//		@Override
//		public void doGet(HttpServletRequest request, HttpServletResponse response)
//				throws ServletException, IOException {
//			String servletPath = request.getServletPath();
//			String pathInfo = request.getPathInfo();
//			String pathInContext = URIUtil.addPaths(servletPath, pathInfo);
//			if(pathInContext != null && !pathInContext.contains("/pending") && pathInContext.toLowerCase().endsWith(".base64")){
//				pathInContext = "/pending" + pathInContext;
//			}
//			LOGGER.info(pathInContext);
//			if (StringUtils.endsWithIgnoreCase(pathInContext, ImageType.PNG.getValue())
//					|| StringUtils.endsWithIgnoreCase(pathInContext, ImageUtil.ImageType.TXT.getValue())) {
//				HttpContent content_tmp = getContentFactory().getContent(pathInContext, response.getBufferSize());
//				if (content_tmp == null
//						&& !StringUtils.startsWithIgnoreCase(pathInContext, "/" + FileUtil.pagesource_pending_dir)) {
//					servletPath = "/" + FileUtil.pagesource_pending_dir + pathInfo;
//					request.getRequestDispatcher(servletPath).forward(request, response);
//					return;
//				}
//			} else if(StringUtils.endsWithIgnoreCase(pathInContext, ImageType.BASE64.getValue())){
//				//获取文件名称,通过base64的路径,创建png文件,并返回png文件路径地址
//				String png = create(request.getRealPath("/"),pathInContext,response.getBufferSize());
//				if(png != null && png.length() > 0){
//					String path = URLEncoder.encode(png,"UTF-8").replaceAll("%2F","/");
//					response.sendRedirect(path);
//					return;
//				}
//			} else {
//				request.getSession(true);
//			}
//			super.doGet(request, response);
//		}
//
//		@Override
//		protected boolean sendData(HttpServletRequest request, HttpServletResponse response, boolean include,
//				HttpContent content, Enumeration<String> reqRanges) throws IOException {
//			 return super.sendData(request, response, include, content, reqRanges);
////			 return true;
//
////			if (request.getSession(false) != null
////					&& StringUtils.endsWithIgnoreCase(request.getRequestURI(), ImageType.BASE64.getValue())) {
////
////				LOGGER.info("Base64Decoder-{}", request.getRequestURI());
////
////				OutputStream out = response.getOutputStream();
////
////				// write the headers
////				putHeaders(response, content, -1);
////
////				// write the content asynchronously if supported
////				if (request.isAsyncSupported() && content.getContentLengthValue() > response.getBufferSize()) {
////					final AsyncContext context = request.startAsync();
////					context.setTimeout(0);
////
////					((HttpOutput) out).sendContent(Base64Util.base64Decoder
////							.decode(ByteBuffer.wrap(IOUtils.toByteArray(content.getInputStream()))), new Callback() {
////								@Override
////								public void succeeded() {
////									context.complete();
////									content.release();
////								}
////
////								@Override
////								public void failed(Throwable x) {
////									if (x instanceof IOException)
////										LOGGER.debug("", x);
////									else
////										LOGGER.warn("", x);
////									context.complete();
////									content.release();
////								}
////
////								@Override
////								public String toString() {
////									return String.format("ResourceService@%x$CB", WebResourceService.this.hashCode());
////								}
////							});
////					return false;
////				}
////				// otherwise write content blocking
////				((HttpOutput) out).sendContent(content);
////			}
////
////			return super.sendData(request, response, include, content, reqRanges);
//		}
//
//		/**
//		 * 构建png文件
//		 * @param path
//		 */
//		private String create(String base,String path,int bufferSize){
//			//png图片地址
//			String fileName = path.substring(0,path.lastIndexOf(".")) + ".png";
//			//如果文件已经存在了
//			if(exits(base + fileName))return fileName;
//			//获取base64源码
//			String baseStr = reader(path,bufferSize);
//			//对数据进行判定
//			if(baseStr == null || baseStr.length() <= 0)return null;
//			//构建文件
//			return newFile(base,fileName,baseStr);
//		}
//
//		/**
//		 * 读取base64文件内容
//		 * @param path
//		 * @param bufferSize
//         * @return
//         */
//		private String reader(String path,int bufferSize){
//			try{
//				HttpContent content = getContentFactory().getContent(path, bufferSize);
//				return new String(IOUtils.toByteArray(content.getInputStream()));
//			}catch (Exception e){
//				e.printStackTrace();
//			}
//			return null;
//		}
//
//		/**
//		 * 将base64字节码转成图片
//		 * @param fileName
//		 * @param imgStr
//         * @return
//         */
//		private String newFile(String base,String fileName,String imgStr){
//			//对字节数组字符串进行Base64解码并生成图片
//			if (imgStr == null)return fileName;
//			try {
//				//Base64解码
//				byte[] b = Base64Util.base64Decoder.decode(imgStr);
//				for(int i=0;i<b.length;++i) {
//					if(b[i]<0) {//调整异常数据
//						b[i]+=256;
//					}
//				}
//				//生成png图片
//				IOUtils.write(b,new FileOutputStream(base + fileName));
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			return fileName;
//		}
//
//		/**
//		 * 判断文件是否存在
//		 * @param path
//         * @return
//         */
//		public boolean exits(String path){
//			try{
//				File file = new File(path);
//				return file.exists();
//			}catch (Exception e){
//				e.printStackTrace();
//			}
//			return false;
//		}
//	}
//}
