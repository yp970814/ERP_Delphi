/**
 * 
 */
package com.bz.cy.app.wechat;

//import cn.hutool.core.util.StrUtil;
//import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bz.cy.app.BaseApp;
import com.bz.cy.basedao.HibernateSessionFactory;
import com.bz.cy.basedao.Item;
import com.bz.cy.basedao.UtilsDAO;
//import com.bz.cy.itf.dao.HttpClientUtil;
import com.bz.cy.services.UserScene;
import com.bz.cy.sys.job.Sysmain;
import com.bz.cy.util.BzBase64;
import com.bz.cy.util.BzErrors;
import com.bz.cy.util.CYException;
import com.bz.cy.util.Errors;
import com.bz.cy.util.HttpClientUtil;
import com.bz.cy.util.UserAttribute;
import com.bz.cy.wdi.dao.Wdii004DAO.CompanyAll;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.AlgorithmParameters;
import java.security.Security;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.beanutils.BeanUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hibernate.Session;
/**
 * @author 作者：yuanping
 * @version 创建时间：2022-2-14下午04:48:52
 * @description:
 */
public class WechatApi {

	/**
	 * 用admin账号模拟登录
	 * @return
	 * @throws CYException
	 */
	public static String login() throws CYException {
		String userKey = "";
		String userAcc = "admin";
		String pwd = "";
		UserScene userScene = null;
		boolean flag = false;
		Session session = null;
		UtilsDAO dao = new UtilsDAO();
		try {
			session = dao.getNilSession();
			dao.setDbName(HibernateSessionFactory.DefaultDbName);
			String sql = "select sysdd002 from tb_sysdd where sysdd001 = (select pubcd_id from tb_pubcd where pubcd001='admin')";
			pwd = session.createSQLQuery(sql).uniqueResult().toString();
			UserAttribute user = dao.findValidateUser(userAcc, pwd, session);
			if (user != null) {
				flag = true;
			}
			if (flag) {
				userScene = new UserScene();
				try {
					BeanUtils.copyProperties(userScene.userAttr, user);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					flag = false;
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					flag = false;
				}
			}

			if (flag == false) {
				throw new CYException(BzErrors.errIllegalUser);
			}

			if (userScene == null)
				throw new CYException(BzErrors.errInvalidUserScene);

			//登录用户获取数据过滤权限
			userScene.userAttr.setDataFilterRoles(dao.getUserDataFilterRoles(user.getUserId(), session)); 
			if (!userScene.register())
				throw new CYException(BzErrors.errUserSceneRegisterFalse);

			userKey = userScene.getKey().toString();
			if (!UtilsDAO.addUseScene(userKey, userScene, userScene.userAttr)){
				throw new CYException(BzErrors.errUserSceneRegisterFalse);
			}			

		} finally {
			if(session!=null){
				session.close();
			}
		}
		return userKey;
	}

	//将byte[]转String
	static String byteToStr(byte[] byteArray) {
		String strDigest = "";
		for (int i = 0; i < byteArray.length; i++) {
			strDigest += byteToHexStr(byteArray[i]);
		}
		return strDigest;
	}

	private static String byteToHexStr(byte mByte) {
		char[] Digit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
		char[] tempArr = new char[2];
		tempArr[0] = Digit[(mByte >>> 4) & 0X0F];
		tempArr[1] = Digit[mByte & 0X0F];
		String s = new String(tempArr);
		return s;
	}

	/**
	 * 发送短信
	 * @param phone
	 * @param checkCode
	 */
	static void sendSms(String phone, String checkCode) {
		//短信下发
		String sendUrl = WechatConfig.SEND_URL;
		Map<String, String> map = new HashMap<String, String>();
		map.put("account",WechatConfig.ACCOUNT);//API账号
		map.put("password",WechatConfig.PASSWORD);//API密码
		map.put("msg",checkCode+"是您的验证码，非本人操作请忽略");//短信内容
		map.put("phone",phone);//手机号
		map.put("report","true");//是否需要状态报告
		map.put("extend","123");//自定义扩展码
		JSONObject js = (JSONObject) JSONObject.toJSON(map);
		System.out.println("sendSmsByPost："+sendSmsByPost(sendUrl,js.toString()));
		//查询余额
		//		String balanceUrl = "https://xxx/msg/balance/json";
		//		Map map1 = new HashMap();
		//		map1.put("account","N*******");
		//		map1.put("password","************");
		//		JSONObject js1 = (JSONObject) JSONObject.toJSON(map1);
		//		System.out.println(sendSmsByPost(balanceUrl,js1.toString()));
	}

	private static String sendSmsByPost(String path, String postContent) {
		URL url = null;
		HttpURLConnection httpURLConnection = null;
		try {
			url = new URL(path);
			httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setRequestMethod("POST");
			httpURLConnection.setConnectTimeout(10000);
			httpURLConnection.setReadTimeout(10000);
			httpURLConnection.setDoOutput(true);
			httpURLConnection.setDoInput(true);
			httpURLConnection.setRequestProperty("Charset", "UTF-8");
			httpURLConnection.setRequestProperty("Content-Type", "application/json");
			httpURLConnection.connect();
			OutputStream os=httpURLConnection.getOutputStream();
			os.write(postContent.getBytes("UTF-8"));
			os.flush();
			StringBuilder sb = new StringBuilder();
			int httpRspCode = httpURLConnection.getResponseCode();
			if (httpRspCode == HttpURLConnection.HTTP_OK) {
				BufferedReader br = new BufferedReader(
						new InputStreamReader(httpURLConnection.getInputStream(), "utf-8"));
				String line = null;
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
				br.close();
				return sb.toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			if(httpURLConnection!=null){
				httpURLConnection.disconnect();
			}
		}
		return null;
	}

	/**
	 * 生成6位随机数字验证码
	 * @return
	 */
	static String generateCode() {
		Random random = new Random();
		int randomNum = random.nextInt(1000000);
		String randomCode = String.format("%06d", randomNum);
		return randomCode;
	}

	/**
	 * 解密手机号
	 * @param iv
	 * @param encrypteddata
	 * @param sessionkey
	 * @return
	 */
	static String getPhoneNumber(String iv, String encrypteddata, String sessionkey){
		String result = "";
		//	    Res res = new Res();
		byte[] dataByte = new byte[0];
		byte[] keyByte = new byte[0];
		byte[] ivByte = new byte[0];
		try {
			//草泥马，傻逼前端，记得替换特殊字符。。。。
			//这个替换字符特别重要，不写就报错。
			String replace = URLEncoder.encode(encrypteddata, "UTF-8").replace("%3D", "=").replace("%2F", "/").replace("%2B","+");
			dataByte = BzBase64.decode(replace);
			String replace2 = URLEncoder.encode(sessionkey, "UTF-8").replace("%3D", "=").replace("%2F", "/").replace("%2B","+");
			keyByte = BzBase64.decode(replace2);
			String replace1 = URLEncoder.encode(iv, "UTF-8").replace("%3D", "=").replace("%2F", "/").replace("%2B","+");
			ivByte = BzBase64.decode(replace1);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		try {
			// 如果密钥不足16位，那么就补足. 这个if 中的内容很重要
			int base = 16;
			if (keyByte.length % base != 0) {
				int groups = keyByte.length / base + (keyByte.length % base != 0 ? 1 : 0);
				byte[] temp = new byte[groups * base];
				Arrays.fill(temp, (byte) 0);
				System.arraycopy(keyByte, 0, temp, 0, keyByte.length);
				keyByte = temp;
			}
			// 初始化
			Security.addProvider(new BouncyCastleProvider());

			Cipher cipher = Cipher.getInstance(WechatConfig.AES_CBC_PADDING, "BC");
			SecretKeySpec spec = new SecretKeySpec(keyByte, WechatConfig.AES);
			AlgorithmParameters parameters = AlgorithmParameters.getInstance(WechatConfig.AES);
			parameters.init(new IvParameterSpec(ivByte));
			// 初始化
			cipher.init(Cipher.DECRYPT_MODE, spec, parameters);
			byte[] resultByte = cipher.doFinal(dataByte);
			if (null != resultByte && resultByte.length > 0) {
				String s = resultByte.toString();
				result = new String(resultByte, "UTF-8");
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 调用微信接口，根据code 获取openId
	 * @param js_code
	 * @return
	 * @throws Exception 
	 * @throws CYException 
	 */
	static String getOpenIdByJSCode(String js_code) throws Exception {
		String apiUrl = "https://api.weixin.qq.com/sns/jscode2session"
			+ "?appid=" + WechatConfig.XCX_AppID
			+ "&secret=" + WechatConfig.XCX_AppSecret
			+ "&js_code=" + js_code
			+ "&grant_type=authorization_code";
		//		return HttpClientUtil.getInstance(Charset.forName("UTF-8")).doGet(apiUrl);
		return HttpClientUtil.get(apiUrl, null);
	}

	/**
	 * 返回错误json
	 * @param message
	 * @return
	 */
	static String getFailureResult(String message){
		JSONObject result = BaseApp.getFailureResult();
		if(!"".equals(message)) {
			//			JSONObject rpdata = new JSONObject();
			//			rpdata.put("code", "-1");
			//			rpdata.put("info", message);
			result.put("message", message);
		}
		return result.toString();
	}

	//	/**
	//	 * 返回错误值
	//	 * @param body
	//	 * @return
	//	 */
	//	private static String throwErrorMessageIfExists(String body) {
	////		String callMethodName = (new Throwable()).getStackTrace()[1].getMethodName();
	////      log.info("#0820 {} body={}", callMethodName, body);
	//		JSONObject jsonObject = JSON.parseObject(body);
	//		if (jsonObject.containsKey("errcode") && jsonObject.getIntValue("errcode") > 0) {
	////		throw new RuntimeException("#WechatApi["+callMethodName+"] call error: "+body);
	//			return getFailureResult(body);
	//		}
	//		return body;
	//	}

	/**
	 * 缓存
	 */
	private final static LoadingCache<String, String> mAccessTokenCache =
		CacheBuilder.newBuilder()
		.expireAfterWrite(7200, TimeUnit.SECONDS)//设置2小时，对象没有被写访问则对象从内存中删除
		.build(new CacheLoader<String, String>() {//CacheLoader类 实现自动加载
			@Override
			public String load(String key) {
				// key: appId#appSecret
				// 从SQL或者NoSql 获取对象
				String[] array = key.split("#");
				if (null == array || array.length != 2) {
					throw new IllegalArgumentException("load access_token error, key = " + key);
				}
				return getAccessToken(array[0], array[1]);
			}
		});

	/**
	 * 获取从缓存中，获取AccessToken
	 * @return
	 */
	public static String getAccessToken() {
		String cacheKey = WechatConfig.XCX_AppID + "#" + WechatConfig.XCX_AppSecret;
		try {
			return mAccessTokenCache.get(cacheKey);
		} catch (ExecutionException e) {
			e.printStackTrace();
			System.out.println("#getAccessToken error, cacheKey=" + cacheKey);
			//          log.error("#getAccessToken error, cacheKey=" + cacheKey, e);
		}
		return null;
	}

	/**
	 * 调用微信接口，获取AccessToken
	 * @param appId
	 * @param appSecret
	 * @return
	 */
	private static String getAccessToken(String appId, String appSecret) {
		String apiUrl = "https://api.weixin.qq.com/cgi-bin/token"
			+ "?appid=" + appId
			+ "&secret=" + appSecret
			+ "&grant_type=client_credential";
		String body = "";
		try {
			//			body = HttpClientUtil.getInstance(Charset.forName("UTF-8")).doGet(apiUrl);
			body = HttpClientUtil.get(apiUrl, null);
		} catch (Exception e) {
			e.printStackTrace();
			//			throw new CYException(-1, "获取AccessToken失败！");
		}

		JSONObject jsonObject = JSON.parseObject(body);
		if (jsonObject.containsKey("errcode") && jsonObject.getIntValue("errcode") > 0) {
			return getFailureResult(body);
		}
		System.out.println("有效时长expires_in："+jsonObject.getString("expires_in"));
		System.out.println("access_token："+jsonObject.getString("access_token"));
		return jsonObject.getString("access_token");
	}

	/**
	 * 调用微信接口，发送模板消息
	 * @param accessToken
	 * @param template
	 */
	public static String templateSend(String accessToken, WechatTemplate template) {
		String apiUrl = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + (accessToken == null ? getAccessToken() : accessToken);

		String templateStr = JSONObject.toJSONString(template);  // 转换为json字符串
		//        JSONObject templateObject = JSONObject.parseObject(templateStr);  // 转换为json对象

		System.out.println("apiUrl："+apiUrl);
		System.out.println("templateSend_accessToken："+accessToken);
		System.out.println("templateSend_templateStr："+templateStr);

		//		return com.bz.cy.itf.dao.HttpClientUtil.getInstance(Charset.forName("UTF-8")).doPostJsonParam(apiUrl, JSONObject.parseObject(templateStr) ,"UTF-8");
		return HttpClientUtil.post(apiUrl, null, templateStr);
	}
	
	public static String templateSendGZH(String accessToken, HashMap<String, Object> map) {
		String apiUrl = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/uniform_send?access_token=" + (accessToken == null ? getAccessToken() : accessToken);
		
		String templateStr = JSONObject.toJSONString(map);  // 转换为json字符串
		System.out.println("templateSendGZH："+templateStr);
		System.out.println("apiUrl："+apiUrl);
		//        JSONObject templateObject = JSONObject.parseObject(templateStr);  // 转换为json对象
		
		//		return com.bz.cy.itf.dao.HttpClientUtil.getInstance(Charset.forName("UTF-8")).doPostJsonParam(apiUrl, JSONObject.parseObject(templateStr) ,"UTF-8");
		return HttpClientUtil.post(apiUrl, null, templateStr);
	}

	public static WechatTemplate getWechatTemplate(String openid, 
			Map<String, WechatTemplateItem> map,
			String type,
			String salhaid,
			String pubcaid){
		//填充模板数据 （测试代码，写死）
		WechatTemplate wechatTemplate = new WechatTemplate();
		wechatTemplate.setTouser(openid);
		wechatTemplate.setTemplate_id(WechatConfig.XCX_TEMPLATE_ID);
		//表单提交场景下为formid，支付场景下为prepay_id
		//wechatTemplate.setForm_id(body.getString("formId"));
		//跳转页面
		wechatTemplate.setMiniprogram_state("trial");
		wechatTemplate.setLang("zh_CN");
		/**
		 * 模板内容填充：随机字符
		 * 购买地点 {{keyword1.DATA}}
		 * 购买时间 {{keyword2.DATA}}
		 * 物品名称 {{keyword3.DATA}}
		 * 交易单号 {{keyword4.DATA}}
		 * -> {"keyword1": {"value":"xxx"}, "keyword2": ...}
		 */
		if("".equals(type)){
			wechatTemplate.setData(map);
			wechatTemplate.setPage(WechatConfig.Jump_Page);
		}else{
			wechatTemplate.setData(map);
			String page = WechatConfig.Jump_Page + "?billid=" + salhaid + "&pubcaid=" + pubcaid;
			wechatTemplate.setPage(page);
		}
		return wechatTemplate;
	}

	public static HashMap<String, Object> getWechatTemplateGZH(String openid, 
			Map<String, String> miniprogram,
			Map<String, WechatTemplateItem> data){
		//填充模板数据 （测试代码，写死）
		WechatTemplateGZH wechatTemplateGZH = new WechatTemplateGZH();
		wechatTemplateGZH.setAppid(WechatConfig.GZH_AppID);
		wechatTemplateGZH.setTemplate_id(WechatConfig.GZH_TEMPLATE_ID);
		wechatTemplateGZH.setUrl("http://weixin.qq.com/download");
		wechatTemplateGZH.setMiniprogram(miniprogram);
		wechatTemplateGZH.setData(data);
		
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("touser", openid);
		map.put("mp_template_msg", wechatTemplateGZH);
		
		return map;
	}

}