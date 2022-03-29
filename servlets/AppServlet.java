package com.bz.cy.app.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;

import com.alibaba.fastjson.JSONObject;
import com.bz.cy.app.BaseApp;
import com.bz.cy.basedao.HibernateSessionFactory;
import com.bz.cy.basedao.UtilsDAO;
import com.bz.cy.services.UserScene;
import com.bz.cy.util.CYException;
import com.bz.cy.util.GlobalInstanceFactory;

/**
 * APP接口的入口
 * 
 * 读取app_interface.properties文件中定义的URL和实现类的对应关系
 * 根据URL获取实现类和方法名，返回执行结果
 * 
 * @author zsuny
 */
public class AppServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	protected static final Log log = LogFactory.getLog(AppServlet.class.getName());

	private static Map<String, Class<?>> actionMap = new HashMap<String, Class<?>>();

	public void init() throws ServletException {
		System.out.println(this.getClass().getName()+" is created");

		//读取配置文件，初始化接口调用配置
		Properties prop = new Properties();
		try {
			prop.load(new Object(){}.getClass().getResourceAsStream("/app_interface.properties"));
			Enumeration<?> propertyNames = prop.propertyNames();
			while (propertyNames.hasMoreElements()) {
				String key = (String) propertyNames.nextElement();
				String value = prop.getProperty(key);
				if (null != value) {
					try {
						Class<?> cls = Class.forName(value);
						actionMap.put(key, cls);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		}catch (IOException e){
			e.printStackTrace();
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		JSONObject params = new JSONObject();
		Enumeration<?> names = request.getParameterNames();
		while(names.hasMoreElements()) {
			String name = (String) names.nextElement();
			String value = request.getParameter(name);
			params.put(name, value);
		}

		execute(request, response, params.toJSONString());
	}

//		public void doPost(HttpServletRequest request, HttpServletResponse response)
//				throws ServletException, IOException {
//			request.setCharacterEncoding("utf-8");  
//	    	response.setContentType("text/html;charset=utf-8"); 
//			StringBuffer requestBody = new StringBuffer();
//	    	BufferedReader reader = request.getReader();
//	    	String line = null;
//	    	while((line = reader.readLine()) != null) {
//	    		requestBody.append(line);
//	    	}
//			Enumeration<?> names = request.getParameterNames();
//			if (names.hasMoreElements()) {
//				JSONObject params = JSONObject.parseObject(requestBody.toString());
//				if (null == params) {
//					params = new JSONObject();
//				}
//				while(names.hasMoreElements()) {
//					String name = (String) names.nextElement();
//					String value = request.getParameter(name);
//					params.put(name, value);
//				}
//				execute(request, response, params.toJSONString());
//			} else {
//				execute(request, response, requestBody.toString());
//			}
//		}
		
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		request.setCharacterEncoding("utf-8");  
		response.setContentType("text/html;charset=utf-8"); 
		StringBuffer requestBody = new StringBuffer();
		String uri = request.getRequestURI();
		String[] uris = uri.split("/");
		String name1 = "",name2 = "",name3 = "";
		if (uri != null && uris.length >= 6) {
			name3 = uris[uris.length-1];
			name2 = uris[uris.length-2];
			name1 = uris[uris.length-3];
		}
		if("bpm".equals(name1) && "bill".equals(name2) && "uploadFileTwo".equals(name3)){
			
			String paramJson = "";
			try {
				paramJson = URLDecoder.decode(request.getParameter("sessionToken").trim(), "utf-8");
			} catch (Exception e) {
				//e.printStackTrace();
			}
			System.out.println("小程序上传Params:" + paramJson);
			JSONObject json = new JSONObject();
			json.put("sessionToken", paramJson);
			
			execute(request, response, json.toJSONString());
		}else{
			BufferedReader reader = request.getReader();
			String line = null;
			while((line = reader.readLine()) != null) {
				requestBody.append(line);
			}
			Enumeration<?> names = request.getParameterNames();
			if (names.hasMoreElements()) {
				JSONObject params = JSONObject.parseObject(requestBody.toString());
				if (null == params) {
					params = new JSONObject();
				}
				while(names.hasMoreElements()) {
					String name = (String) names.nextElement();
					String value = request.getParameter(name);
					params.put(name, value);
				}
				execute(request, response, params.toJSONString());
			} else {
				execute(request, response, requestBody.toString());
			}
		}
	}

	private void execute(HttpServletRequest request, HttpServletResponse response, String params) throws IOException {
		String uri = request.getRequestURI();
		try {
			params = params.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
			params = URLDecoder.decode(params.trim(), "utf-8");

			System.out.println("URI:" + request.getMethod() + " " + uri);
			System.out.println("Params:" + params);
		} catch (Exception e) {
			//e.printStackTrace();
		}

		/*
		 /bigzone_new_cy_shake/v1/api/sys/lg/login

		 截掉 /bigzone_new_cy_shake/v1/api
		 login 为方法名
		 sys.lg 为模块名
		 */
		String[] uris = uri.split("/");
		if (uri != null && uris.length >= 6) {
			String methodName = uris[uris.length-1];
			StringBuilder model = new StringBuilder();
			for (int i=4; i<=uris.length-2; i++) {
				model.append(uris[i]).append(".");
			}
			model.deleteCharAt(model.length()-1);

			System.err.println("[model]"+model.toString());
			System.err.println("[methodName]"+methodName);

			Class<?> cls = actionMap.get(model.toString());

			System.err.println("[cls]"+cls.getName());

			if (null != cls) {
				Method method = null;
				try {
					method = cls.getMethod(methodName, new Class<?>[]{HttpServletRequest.class, HttpServletResponse.class, String.class});
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}

				if (null != method) {
					Object returnValue = null;
					try {
						if (Void.class == method.getReturnType()) {
							method.invoke(cls, new Object[] {request, response, params});
							return;
						} else {
							returnValue = method.invoke(cls, new Object[] {request, response, params});
						}
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
						Throwable targetException = e.getTargetException();
						if (targetException instanceof CYException) {
							int errCode = ((CYException)targetException).getError();
							String errInfo = ((CYException)targetException).getMessage();
							if (null == errInfo || "".equals(errInfo)) {
								errInfo = getErrInfo(errCode);
							}
							returnValue = "{\"message\":{\"code\":\"" + errCode + "\",\"info\":\"" + errInfo + "\"},\"status\":\"failure\"}";
						}
					}

					if (null != returnValue && !"".equals(returnValue)) {
						if (returnValue instanceof String) {
							System.out.println("Result:" + (String) returnValue);
							output(request, response, (String) returnValue, params);	
						} else {
							System.out.println("Result:" + returnValue.toString());
							output(request, response, returnValue.toString(), params);	
						}
					} else {
						String responseContent = "{\"message\":{\"code\":\"4\",\"info\":\"接口返回值不正确\"},\"status\":\"failure\"}";
						output(request, response, responseContent, params);
					}
				} else {
					String responseContent = "{\"message\":{\"code\":\"3\",\"info\":\"接口未实现\"},\"status\":\"failure\"}";
					output(request, response, responseContent, params);
				}
			} else {
				String responseContent = "{\"message\":{\"code\":\"2\",\"info\":\"接口配置不正确\"},\"status\":\"failure\"}";
				output(request, response, responseContent, params);
			}
		} else {
			String responseContent = "{\"message\":{\"code\":\"1\",\"info\":\"接口未定义\"},\"status\":\"failure\"}";
			output(request, response, responseContent, params);
		}
	}

	private void output(HttpServletRequest request, HttpServletResponse response, String content, String params) throws IOException {
        
		JSONObject paramJson = JSONObject.parseObject(params);
		String sessionToken = paramJson.getString("sessionToken");

		if (null == sessionToken || "".equals(sessionToken)) {

		}else{
			try{
	            logout_new(params);
	        }catch(Exception e){
	            e.printStackTrace();
	        }
		}

		response.setContentType("text/plain;charset=utf-8");
		PrintWriter out = response.getWriter();
		out.write(content);
		out.flush();
		out.close();
	}
	
	public static void logout_new(String paramJson) throws CYException {
		JSONObject params = JSONObject.parseObject(paramJson);
	    try {
	    	UserScene usc = BaseApp.checkSessionToken(params);
	    	System.out.println("登出-->"+usc.getUUIDStr());
	    	HibernateSessionFactory.freeUserSession(usc.getUUIDStr());
	    	GlobalInstanceFactory.freeUserInstance(usc.getUUIDStr());
	    	UserScene.unregister(usc.getUUIDStr());
//	    	UtilsDAO.delUseScene(usc.getUUIDStr());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getErrInfo(int errCode) {
		UtilsDAO dao= new UtilsDAO();
		Session session = dao.getNilSession();
		try {
			return dao.getErrorMessage(errCode, session);
		} finally {
			if(session!=null){
				session.close();
			}
		}
	}
}
