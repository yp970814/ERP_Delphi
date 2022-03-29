package com.bz.cy.app.wechat;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bz.cy.app.BaseApp;
import com.bz.cy.basedao.HibernateSessionFactory;
import com.bz.cy.basedao.Item;
import com.bz.cy.basedao.UtilsDAO;
import com.bz.cy.sal.dao.Sali151DAO;
import com.bz.cy.services.UserScene;
import com.bz.cy.util.AppIOFlowUtil;
import com.bz.cy.util.CYException;
import com.bz.cy.util.Errors;
import com.bz.cy.util.Information;
import com.bz.cy.util.SetTime;

/**
 * @author 作者：yuanping
 * @version 创建时间：2021-11-26上午11:05:41
 * @description:
 */
public class WechatController extends BaseApp{
	
	/**
	 * 接收反馈交期
	 * @param request
	 * @param response
	 * @param paramJson
	 * @return
	 */
	public static String wechatUpdateDate(HttpServletRequest request, HttpServletResponse response, String paramJson){
		JSONObject params = JSONObject.parseObject(paramJson);
		String pubcaid = params.getString("pubcaid");
		String billid = params.getString("billid");
		String date = params.getString("date");
		if(null==pubcaid || "".equals(pubcaid)) return WechatApi.getFailureResult("pubcaid不存在！");
		if(null==billid || "".equals(billid)) return WechatApi.getFailureResult("billid不存在！");
		if(null==date || "".equals(date)) return WechatApi.getFailureResult("date不存在！");
		
		Session session = null;
		Transaction tx = null;
		UserScene scence = null;
		String userKey = "";
//		Item item = null;
		boolean condition = false;
		try {
			UtilsDAO dao = new UtilsDAO();
			session = dao.getNilSession();
			dao.setDbName(HibernateSessionFactory.DefaultDbName);

			String sql = " select pubca030 "
				+" from tb_pubca "
				+" where pubca_id = " + pubcaid;
			String dbname = session.createSQLQuery(sql).uniqueResult().toString()+".";
			
			try {
				
				if(!"".equals(date)){
					
					sql = " select salha055 "
						+" from "+dbname+"tb_salha "
						+" where salha_id = " + billid;
					String salha055 = session.createSQLQuery(sql).uniqueResult().toString();
					Date oldDate = SetTime.parseDate(salha055);
					Date newDate = SetTime.parseDate(date);
					if (oldDate.compareTo(newDate) > 0) {
						return WechatApi.getFailureResult("新交期不能大于原交期！");
					}
					
					tx = session.beginTransaction();
					sql = " update "+dbname+"tb_salha "
					+ " set "
					+ " salha055 = to_date('"+date+"','YYYY-MM-DD'), "
					+ " salha196 = to_date('"+date+"','YYYY-MM-DD') + salha195 "
					+ " where salha_id = "+billid;
					int x = session.createSQLQuery(sql).executeUpdate();
					
					sql = " update "+dbname+"tb_salhb "
					+ " set "
					+ " salhb038 = to_date('"+date+"','YYYY-MM-DD'), "
					+ " salhb072 = to_date('"+date+"','YYYY-MM-DD') "
					+ " where salhb001 = "+billid;
					int xx = session.createSQLQuery(sql).executeUpdate();
					
					if(x>0 && xx>0){
						tx.commit();
						tx = null;
						condition = true;
					}else{
						tx.rollback();
						tx = null;
						return WechatApi.getFailureResult("修改交期失败！");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
				tx = null;
			}
			
			if(condition){
				sql = " select nvl(salha191,0) "
					+" from "+dbname+"tb_salha "
					+" where salha_id = "+billid;
				String salha191 = session.createSQLQuery(sql).uniqueResult().toString();
				if("1".equals(salha191)){
					/*2.用admin账号模拟登录*/
					userKey = WechatApi.login();
					scence = UserScene.fromKey(userKey);
					
					Sali151DAO sali151dao = new Sali151DAO();
					sali151dao.setUserKey(userKey);
					sali151dao.pushCarryBagsOrderInfo(new Information(),"", Long.valueOf(billid), userKey, session);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			return getFailureResult().toString();
		}finally {
			if (session != null) session.close();
			if (!"".equals(userKey)){
				HibernateSessionFactory.freeUserSession(userKey);
				scence.unregister(userKey);
			}
		}
		return getSuccessResult().toJSONString();
	}

	/**
	 * 统一服务消息推送
	 * @return
	 */
	public static String wechatPushMessageGZH(HttpServletRequest request, HttpServletResponse response, String paramJson){
		JSONObject params = JSONObject.parseObject(paramJson);
		String openid = params.getString("openid");
		if(null==openid || "".equals(openid)) return WechatApi.getFailureResult("openid不存在！");

		Map<String, String> miniprogram = new HashMap<String, String>();
		miniprogram.put("appid", WechatConfig.XCX_AppID);
		miniprogram.put("page", WechatConfig.Jump_Page);
		
		Map<String, WechatTemplateItem> data = new HashMap<String, WechatTemplateItem>();
		data.put("first", new WechatTemplateItem("嘿嘿"));
		data.put("keyword1", new WechatTemplateItem("今晚"));
		data.put("keyword2", new WechatTemplateItem("去你家"));
		data.put("keyword3", new WechatTemplateItem("吃大西瓜"));
		data.put("keyword4", new WechatTemplateItem("皮"));
		data.put("keyword5", new WechatTemplateItem("呸"));
		data.put("remark", new WechatTemplateItem("真难吃"));
		
		String accessToken = WechatApi.getAccessToken();
		String body = WechatApi.templateSendGZH(accessToken, WechatApi.getWechatTemplateGZH(openid, miniprogram, data));
		JSONObject jsonObject = JSON.parseObject(body);
		if (jsonObject.containsKey("errcode") && jsonObject.getIntValue("errcode") > 0) {
			System.out.println("订阅消息发送失败:" + jsonObject.getIntValue("errcode") + "," + jsonObject.getIntValue("errmsg"));
			return getFailureResult().toString();
		}
		System.out.println("Send Success");
		JSONObject result = getSuccessResult();
		result.put("accessToken", accessToken);
		return result.toJSONString();
	}	
	
	/**
	 * 一次性订阅消息推送
	 * @return
	 */
	public static String wechatPushMessage(HttpServletRequest request, HttpServletResponse response, String paramJson){
		JSONObject params = JSONObject.parseObject(paramJson);
		String openid = params.getString("openid");
		if(null==openid || "".equals(openid)) return WechatApi.getFailureResult("openid不存在！");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
		Map<String, WechatTemplateItem> map = new HashMap<String, WechatTemplateItem>();
		map.put("character_string1", new WechatTemplateItem("123456789"));
		map.put("thing2", new WechatTemplateItem("请你吃大西瓜"));
		map.put("time3", new WechatTemplateItem(sdf.format(new Date())));
		map.put("thing4", new WechatTemplateItem("请你吃大西瓜皮"));
		
		WechatTemplate wechatTemplate = WechatApi.getWechatTemplate(openid, map, "", "", "");
		
		String accessToken = WechatApi.getAccessToken();
		String body = WechatApi.templateSend(accessToken, wechatTemplate);
		JSONObject jsonObject = JSON.parseObject(body);
		if (jsonObject.containsKey("errcode") && jsonObject.getIntValue("errcode") > 0) {
			System.out.println("订阅消息发送失败:" + jsonObject.getIntValue("errcode") + "," + jsonObject.getIntValue("errmsg"));
			return getFailureResult().toString();
		}
		System.out.println("Send Success");
		JSONObject result = getSuccessResult();
		result.put("accessToken", accessToken);
		return result.toJSONString();
	}	
	
	/**
	 * 确认消息推送，客户订阅之后
	 * @param request
	 * @param response
	 * @param paramJson
	 * @return
	 */
	public static String wechatPushConfirm(HttpServletRequest request, HttpServletResponse response, String paramJson) {
		JSONObject params = JSONObject.parseObject(paramJson);
		String openid = params.getString("openid");
		if(null==openid || "".equals(openid)) return WechatApi.getFailureResult("openid不存在！");
		
		Session session = null;
		String sql = "";
		Transaction tx = null;
		try {
			UtilsDAO dao = new UtilsDAO();
			session = dao.getNilSession();
			dao.setDbName(HibernateSessionFactory.DefaultDbName);

			sql = "select count(1) from TB_WECHAT where openid = '" + openid+"'";
			Double con = ((BigDecimal)session.createSQLQuery(sql).uniqueResult()).doubleValue();

			if(con > 0D){
				tx = session.beginTransaction();
				sql = " update tb_wechat set "
					+ " push = '1' "
					+ " where openid = '"+openid+"' ";
				if(session.createSQLQuery(sql).executeUpdate()<=0) throw new CYException(-1,"修改记录失败！");
			}else{
				return WechatApi.getFailureResult("没有找到openid记录！");
			}
			
			if (tx != null) tx.commit();
		}catch(CYException e){
			e.printStackTrace();
			if (tx != null) tx.rollback();
		}catch(Exception e){
			e.printStackTrace();
			if (tx != null) tx.rollback();
		}finally{
			if (session != null) session.close();
		}
		return getSuccessResult().toJSONString();
	}

	/**
	 * 微信授权认证,需要在微信平台配置token
	 * @param signature
	 * @param timestamp
	 * @param nonce
	 * @param echostr
	 * @return
	 */
	public static String wechatAuth(HttpServletRequest request, HttpServletResponse response, String paramJson) {
		JSONObject params = JSONObject.parseObject(paramJson);

		String signature = params.getString("signature");
		String timestamp = params.getString("timestamp");
		String nonce = params.getString("nonce");
		String echostr = params.getString("echostr");

		if(null==signature || "".equals(signature)) return WechatApi.getFailureResult("signature不存在！");
		if(null==timestamp || "".equals(timestamp)) return WechatApi.getFailureResult("timestamp不存在！");
		if(null==nonce || "".equals(nonce)) return WechatApi.getFailureResult("nonce不存在！");
		if(null==echostr || "".equals(echostr)) return WechatApi.getFailureResult("echostr不存在！");

		//排序
		String[] arr = {WechatConfig.TOKEN, timestamp, nonce};
		Arrays.sort(arr);

		StringBuilder content = new StringBuilder();
		for (int i = 0; i < arr.length; i++) {
			content.append(arr[i]);
		}

		//sha1Hex 加密
		MessageDigest md = null;
		String temp = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(content.toString().getBytes());
			temp = WechatApi.byteToStr(digest);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		if ((temp.toLowerCase()).equals(signature)) {
			return echostr;
		}
		return null;
	}

	/**
	 * 收款单详情
	 * @param request
	 * @param response
	 * @param paramJson
	 * @return
	 * @throws CYException
	 */
	public static String wechatGetDepositdetail(HttpServletRequest request, HttpServletResponse response, String paramJson) 
	throws CYException {
		JSONObject params = JSONObject.parseObject(paramJson);
		String pubcaid = params.getString("pubcaid");
		String billid = params.getString("billid");

		if(null==billid || "".equals(billid)) return WechatApi.getFailureResult("billid不存在！");
		if(null==pubcaid || "".equals(pubcaid)) return WechatApi.getFailureResult("pubcaid不存在！");

		Session session = null;
		Item item = null;
		try {
			UtilsDAO dao = new UtilsDAO();
			session = dao.getNilSession();
			dao.setDbName(HibernateSessionFactory.DefaultDbName);

			String sqlx = " select pubca030 "
				+" from tb_pubca "
				+" where pubca_id = " + pubcaid + "";
			String dbname = session.createSQLQuery(sqlx).uniqueResult().toString()+".";

			StringBuilder sql = new StringBuilder();
			sql.setLength(0);
			sql.append("select salda_id billid,")
			.append(" salda002 billno,")
			.append(" to_char(salda003,'YYYY-MM-DD') billdate,")
			.append(" salda042 type,")//收款类型1.自营 2.联营
			.append(" salda043 source,")//收款来源 1.卡内 2.卡外
			.append(" pubkc_id paytypeid,")
			.append(" pubkc002 paytype,")//收款方式
			.append(" saldb006 bankno,")//银行卡号
			.append(" SALDA015 customername,")
			.append(" salda016 phoneno,")
			.append(" salda034 address,")
			.append(" cd1.pubcd_id guideid,")
			.append(" cd1.pubcd003 guider,")
			.append(" db003 depositmoney,")//收款金额
			.append(" dc2.dd003 returndepositmoney,")//退款金额
			.append(" pubcb_id storeid,")
			.append(" pubcb002 store,")
			.append(" salda019 memo,")
			.append(" salda024 statue,")
			.append(" db015 setamount,")//预计冲抵金额
			.append(" db016 unoffsetamount,")//未冲抵金额
			.append(" db016 canreturnmoney,")//可退款金额
			.append(" case when salda024='Y' and db016>0 then '1' else '0' end returnmoney,")//是否显示退定金按钮，1表示显示，0表示不显示
			.append("salda036,")       /*SALHA018   导购员一         */
			.append("SALDA046,")       /*SALHA040   导购员1分摊比率         */
			.append("cd1.pubcd001 salda036c1,")
			.append("cd1.pubcd003 salda036c2,")
			.append("SALDA047,")       /*SALDA047   导购员二     */
			.append("SALDA048,")       /*SALDA048   导购员2分摊比率      */
			.append("cd2.pubcd001 SALDA047c1,")
			.append("cd2.pubcd003 SALDA047c2,")
			.append("SALDA049,")       /*SALDA049   导购员三     */
			.append("SALDA050,")       /*SALDA050   导购员3分摊比率      */
			.append("cd3.pubcd001 SALDA049c1,")
			.append("cd3.pubcd003 SALDA049c2,")
			.append("SALDA051,")       /*SALDA051   导购员四     */
			.append("SALDA052,")       /*SALDA052   导购员4分摊比率      */
			.append("cd4.pubcd001 SALDA051c1,")
			.append("cd4.pubcd003 SALDA051c2,")
			.append("SALDA053,")       /*SALDA053   导购员五     */
			.append("SALDA054,")       /*SALDA054   导购员5分摊比率      */
			.append("SALDA059,")       /*SALDA059 POS机凭证号      */
			.append("SALDA060,")       /*SALDA060 POS机终端号      */
			.append("cd5.pubcd001 SALDA053c1,")
			.append("cd5.pubcd003 SALDA053c2,")
			.append("SALDA064 cashiertype, ")
			.append("SALDA082 ")
			.append(" from "+dbname+"tb_salda")
			.append(" left join tb_pubcd cd1 on cd1.pubcd_id = salda036")
			.append(" left join tb_pubcd cd2 on cd2.pubcd_id = SALDA047")
			.append(" left join tb_pubcd cd3 on cd3.pubcd_id = SALDA049")
			.append(" left join tb_pubcd cd4 on cd4.pubcd_id = SALDA051")
			.append(" left join tb_pubcd cd5 on cd5.pubcd_id = SALDA053")
			.append(" left join tb_pubcb on pubcb_id = salda005")
			.append(" left join (select sum(saldb003) db003,sum(saldb016) db016,sum(saldb015) db015, saldb001,saldb002,saldb006")
			.append("      from "+dbname+"tb_saldb group by saldb001,saldb002,saldb006) db on salda_id=saldb001")
			.append(" left join tb_pubkc kc on db.saldb002=kc.pubkc_id")
			.append(" left join (select sum(saldd003) dd003, saldc_id,saldc004")
			.append("      		  from "+dbname+"tb_saldc")
			.append("              left join "+dbname+"tb_saldd on saldd001 = saldc_id")
			.append("            group by saldc_id,saldc004) dc2 on dc2.saldc004=salda_id")
			.append(" left join (select sum(saldd003) dd003, saldd001")
			.append("      from "+dbname+"tb_saldd group by saldd001) dd on dc2.saldc_id=saldd001")
			.append(" where salda_id = "+billid);
			System.out.println("sql.toString()======>"+sql.toString());

			JSONObject rpdata = new JSONObject();
			try {
				item = dao.findBySql(sql.toString(), session);
				ResultSet rs = item.getRs();
				while(rs.next()){

					rpdata.put("customername", nullToEmptyStr(rs.getString("customername")));
					rpdata.put("phoneno", nullToEmptyStr(rs.getString("phoneno")));
					rpdata.put("address", nullToEmptyStr(rs.getString("address")));
					rpdata.put("statue", nullToEmptyStr(rs.getString("statue")));
					//					JSONObject deposit = new JSONObject();
					rpdata.put("billid", nullToEmptyStr(rs.getString("billid")));
					rpdata.put("billno", rs.getString("billno"));
					rpdata.put("billdate", rs.getString("billdate"));
					rpdata.put("type", nullToEmptyStr(rs.getString("type")));
					rpdata.put("source", nullToEmptyStr(rs.getString("source")));
					rpdata.put("paytypeid", nullToEmptyStr(rs.getString("paytypeid")));
					rpdata.put("paytype", nullToEmptyStr(rs.getString("paytype")));
					rpdata.put("bankno", nullToEmptyStr(rs.getString("bankno")));
					rpdata.put("depositmoney", nullToEmptyStr(rs.getString("depositmoney")));
					rpdata.put("returndepositmoney", nullToEmptyStr(rs.getString("returndepositmoney")));
					rpdata.put("guideid", nullToEmptyStr(rs.getString("guideid")));
					rpdata.put("guider", nullToEmptyStr(rs.getString("guider")));
					rpdata.put("storeid", nullToEmptyStr(rs.getString("storeid")));
					rpdata.put("store", nullToEmptyStr(rs.getString("store")));
					rpdata.put("memo", nullToEmptyStr(rs.getString("memo")));
					rpdata.put("setamount", nullToZero(rs.getString("setamount")));
					rpdata.put("unoffsetamount", nullToZero(rs.getString("unoffsetamount")));
					rpdata.put("canreturnmoney", nullToZero(rs.getString("canreturnmoney")));
					rpdata.put("returnmoney", nullToZero(rs.getString("returnmoney")));
					rpdata.put("pospinno", nullToEmptyStr(rs.getString("SALDA059")));
					rpdata.put("posmacno", nullToEmptyStr(rs.getString("SALDA060")));
					rpdata.put("cashiertype", nullToEmptyStr(rs.getString("cashiertype")));
					rpdata.put("salda082", nullToEmptyStr(rs.getString("SALDA082")));
					JSONArray guidelist = new JSONArray();
					JSONObject one = new JSONObject();
					one.put("id", rs.getLong("salda036"));
					one.put("code", rs.getString("salda036c1"));
					one.put("name", rs.getString("salda036c2"));
					one.put("ratios", rs.getDouble("SALDA046"));
					guidelist.add(one);
					if (rs.getLong("SALDA047")>0){
						JSONObject two = new JSONObject();
						two.put("id", rs.getLong("SALDA047"));
						two.put("code", rs.getString("SALDA047c1"));
						two.put("name", rs.getString("SALDA047c2"));
						two.put("ratios", rs.getDouble("SALDA048"));
						guidelist.add(two);
					}
					if (rs.getLong("SALDA049")>0){
						JSONObject three = new JSONObject();
						three.put("id", rs.getLong("SALDA049"));
						three.put("code", rs.getString("SALDA049c1"));
						three.put("name", rs.getString("SALDA049c2"));
						three.put("ratios", rs.getDouble("SALDA050"));
						guidelist.add(three);
					}
					if (rs.getLong("SALDA051")>0){
						JSONObject four = new JSONObject();
						four.put("id", rs.getLong("SALDA051"));
						four.put("code", rs.getString("SALDA051c1"));
						four.put("name", rs.getString("SALDA051c2"));
						four.put("ratios", rs.getDouble("SALDA052"));
						guidelist.add(four);
					}
					if (rs.getLong("SALDA053")>0){
						JSONObject five = new JSONObject();
						five.put("id", rs.getLong("SALDA053"));
						five.put("code", rs.getString("SALDA053c1"));
						five.put("name", rs.getString("SALDA053c2"));
						five.put("ratios", rs.getDouble("SALDA054"));
						guidelist.add(five);
					}
					rpdata.put("guidelist", guidelist);
					Item item1 = null;
					try {
						//↓下面是产品列表
						sql.setLength(0);
						sql.append( " select saldc_id billid,to_char(saldc003,'YYYY-MM-DD') billdate")
						.append( " from tb_saldc")
						.append( " where saldc004=").append(rs.getString("billid"));
						item1 = dao.findBySql(sql.toString(), session);
						String[][] fieldMap = new String[][] {
								{"BILLID", "billid"},
								{"BILLDATE", "billdate"}
						};
						rpdata.put("returndeposit", itemToJson(item1,fieldMap));
					} catch (Exception e) {
						throw e;
					} finally {
						if (item1!=null) item1.close();
					}
				}
			} catch (Exception e) {
				throw e;
			} finally {
				if (item!=null) item.close();
			}

			sql.setLength(0);
			sql.append( " select SALDU002 name,SALDU003 sysname,SALDU003 url,pubcd003 upper,to_char(SALDU005,'YYYY-MM-DD') uptime")
			.append( " from TB_SALDU ")
			.append( " left join tb_pubcd on pubcd_id = SALDU004")
			.append( " where SALDU001 = ").append(billid);

			System.out.println("查询附件SQL: " + sql.toString());
			String[][] fieldMap = new String[][] {
					{"NAME", "name"},
					{"SYSNAME", "sysname"},
					{"URL", "url"},
					{"UPPER", "upper"},
					{"UPTIME", "uptime"}
			};

			try {
				item = dao.findBySql(sql.toString(), session);
				rpdata.put("enclosure", itemToJson(item,fieldMap,null,null,"url"));
			} catch (Exception e) {
				//				throw e;
			} finally {
				if (item!=null) item.close();
			}
			JSONObject result = getSuccessResult();
			result.put("rpdata", rpdata);
			return result.toJSONString();
		}catch(Exception e){
			e.printStackTrace();
			return getFailureResult().toString();
		}finally {
			if (session != null) session.close();
			if (item != null) item.close();
		}
	}

	/**
	 * 收款列表
	 * @param request
	 * @param response
	 * @param paramJson
	 * @return
	 * @throws CYException
	 */
	public static String wechatGetDepositList(HttpServletRequest request, HttpServletResponse response, String paramJson) 
	throws CYException {
		JSONObject params = JSONObject.parseObject(paramJson);
		String openid = params.getString("openid");
		String begin = params.getString("begin");
		String pagesize = params.getString("pagesize");

		Session session = null;
		UtilsDAO dao = null;
		Item it = null;
		ResultSet rs = null;
		StringBuilder sb = null;
		StringBuilder bigsb = null;
		String where = "";
		try {
			dao = new UtilsDAO();
			session = dao.getNilSession();
			dao.setDbName(HibernateSessionFactory.DefaultDbName);
			String sql = " select phone "
				+" from tb_wechat "
				+" where rownum = 1 "
				+" and openid = '" + openid +"'";
			String phone = session.createSQLQuery(sql).uniqueResult().toString();

			if(null==phone || "".equals(phone)){
				return WechatApi.getFailureResult("当前openId没有绑定手机号");
			}
			
			where+=" and salda016 = '"+phone+"'";

			String dbname = "", pubca_id = "";
			String sqlCheck = " select pubca_id,pubca030 from tb_pubca where pubca030 <> 'nywms' order by pubca_id ";
			it = dao.findBySql(sqlCheck, session);
			rs = it.getRs();

			sb = new StringBuilder();
			bigsb = new StringBuilder();
			while(rs.next()){
				dbname = rs.getString("pubca030")+".";
				pubca_id = rs.getString("pubca_id");

				String[] urls = getFolderUrl("");
				String[] apks = GetApkUrl();
				sb.setLength(0);
				sb.append("select salda_id billid,")
				.append(pubca_id+" as pubcaid,")
				.append(" salda002 billno,")
				.append(" to_char(salda003,'YYYY-MM-DD')||'  ('||decode(nvl(salda029,'0'),'1','已确认','未确认')||')' billdate,")
				.append(" SALDA015 customername,")
				.append(" salda016 phoneno,")
				.append(" pubcd003 guider,")
				.append(" db003 depositmoney,")//定金金额
				.append(" db014 returndepositmoney,")
				.append(" salda024 statue,")
				.append(" db015 setamount,")//预计冲抵金额
				.append(" db016 unoffsetamount,")//未冲抵金额
				.append(" case when salda024='Y' and db016>0 then '1' else '0' end returnmoney,")//是否显示退定金按钮，1表示显示，0表示不显示
				.append(" '0' qrcode")
				//			   .append(" ,case when salda024='Y' ")
				//			   .append("		and nvl(salda029,0)='1' ")
				//			   .append("		and ((nvl(salda079,0) <> '12' and nvl(salda083,0) = 0) ")
				//			   .append("			or ")
				//			   .append("			 (nvl(salda079,0) <> '8' and nvl(salda083,0) = 1)) ")
				//			   .append("		and instr(xx.sysdb007, '开具收据') > 0 ")
				//			   .append("	   then '1' else '0' end ispaycontract ")
				//			   .append(" ,case when ((nvl(salda079,0) = '12' and nvl(salda083,0) = 0) ")
				//			   .append("			or ")
				//			   .append("			 (nvl(salda079,0) = '8' and nvl(salda083,0) = 1)) ")
				//			   .append("		and instr(xx.sysdb007, '作废签章') > 0 ")
				//			   .append("	   then '1' else '0' end isnullify ")
				.append(" ,case when ((nvl(salda079,0) = '12' and nvl(salda083,0) = 0) ")
				.append("			or ")
				.append("			 (nvl(salda079,0) = '8' and nvl(salda083,0) = 1)) ")
				.append("	   then ")
				.append(" replace('").append(urls[0]).append("'||").append("salda077").append("||'.pdf'").append(",")
				.append(" '").append(urls[1]).append("',")
//				.append(" '").append("http://" + apks[0] + ":" + apks[1]).append("')")
				.append(" '").append("https://cls.bgycx.com").append("')")
				.append(" else '0' end isdownload")
				.append(" ,pubcb003 ")
				.append(" from "+dbname+"tb_salda")
				.append(" left join tb_pubcb on pubcb_id = SALDA005")
				.append(" left join tb_pubcd on pubcd_id = salda036")
				.append(" left join (select sum(saldb003) db003,sum(saldb014) db014,sum(saldb015) db015,sum(saldb016) db016, saldb001")
				.append("      from "+dbname+"tb_saldb group by saldb001) on salda_id=saldb001")
				.append(" left join (select sum(saldd003) dd003, saldc004")
				.append("      		  from "+dbname+"tb_saldc")
				.append("              left join "+dbname+"tb_saldd on saldd001 = saldc_id")
				.append("            group by saldc004) on saldc004=salda_id")
				.append(" where nvl(salda065,'0')<>'1' ").append(where);
				if(!rs.isLast()){
					sb.append("  union all ");
				}
				bigsb.append(sb);
			}
		}catch(Exception e){
			e.printStackTrace();			
		} finally {
			//			if(session!=null) session.close();
			if (it!=null) it.close();
		}

		System.out.println("=========>"+bigsb.toString());
		try{
			//			session = dao.getNilSession();
			//			dao.setDbName(HibernateSessionFactory.DefaultDbName);
			String[][] fieldMap = new String[][] {
					{"BILLID", "billid"},//销售单ID
					{"PUBCAID", "pubcaid"},//销售单ID
					{"BILLNO", "billno"},//单号
					{"BILLDATE", "billdate"},//日期
					{"CUSTOMERNAME", "customername"},//顾客姓名
					{"PHONENO", "phoneno"},//电话
					{"GUIDER", "guider"},//导购
					{"DEPOSITMONEY", "depositmoney"},//定金金额
					{"RETURNDEPOSITMONEY", "returndepositmoney"},//退定金金额
					{"STATUE", "statue"},//审核状态
					{"SETAMOUNT", "setamount"},//预计冲抵金额
					{"UNOFFSETAMOUNT", "unoffsetamount"},//未冲抵金额
					{"RETURNMONEY", "returnmoney"},//是否显示退定金按钮，1表示显示，0表示不显示
					{"QRCODE", "qrcode"},//是否显示获取二维码按钮
//					{"ISPAYCONTRACT", "ispaycontract"},//是否显示获取二维码按钮
//					{"ISNULLIFY", "isnullify"},//是否显示获取二维码按钮
					{"ISDOWNLOAD", "isdownload"},//是否显示获取二维码按钮
					{"PUBCB003", "pubcb003"}//是否显示获取二维码按钮
			};
			int count = queryCount(bigsb, dao, session);
			it = dao.findBySql(bigsb.toString(), session);
			JSONObject result = getSuccessResult();
			JSONObject rpdata = new JSONObject();
			JSONArray list = itemToJson(it,fieldMap,begin,pagesize);
			rpdata.put("list", list);
			rpdata.put("count", count);
			result.put("rpdata", rpdata);
			return result.toJSONString();
		}catch(Exception e){
			e.printStackTrace();
			return getFailureResult().toString();
		}finally {
			if (session != null) session.close();
			if (it != null) it.close();
		}
	}

	/**
	 * 销售订单详情
	 * @param request
	 * @param response
	 * @param paramJson
	 * @return
	 * @throws CYException
	 */
	public static String wechatGetOrderDetail(HttpServletRequest request, HttpServletResponse response, String paramJson) 
	throws CYException {
		JSONObject params = JSONObject.parseObject(paramJson);
		String pubcaid = params.getString("pubcaid");
		String billid = params.getString("billid");

		if(null==billid || "".equals(billid)) return WechatApi.getFailureResult("billid不存在！");
		if(null==pubcaid || "".equals(pubcaid)) return WechatApi.getFailureResult("pubcaid不存在！");

		Session session = null;
		Item item = null;
		try {
			UtilsDAO dao = new UtilsDAO();
			session = dao.getNilSession();
			dao.setDbName(HibernateSessionFactory.DefaultDbName);

			String sqlx = " select pubca030 "
				+" from tb_pubca "
				+" where pubca_id = " + pubcaid + "";
			String dbname = session.createSQLQuery(sqlx).uniqueResult().toString()+".";
			//		String oaid = "";
			//		try {
			//			WorkSySAppDAO wsa = new WorkSySAppDAO();
			//			oaid = wsa.getoaid(usc.userAttr, billid,"sali151");
			//		} catch (Exception e) {
			//			e.printStackTrace();
			//		}
			StringBuilder sql = new StringBuilder();
			sql.setLength(0);
			sql.append( " select salha_id,")
			.append( " salha002,") //单号
			.append( " to_char(salha003,'YYYY-MM-DD HH24:MI:SS') salha003,") //日期
			.append( " cb.pubcb002 as salha005c2,") //门店名称
			.append( " salha010,") //顾客姓名
			//	       .append( " cd1.pubcd003 as salhb018c2,")//导购员姓名
			//	       .append( " cd2.pubcd003 as salhb034c2,")//导购员二姓名
			.append( " (select pubgo057 from tb_pubgo where pubgo_id = salha008) pubgo057,")//邮箱地址
			.append( " pubbf001,")
			.append( " pubbf002,")
			.append( " salha012,")//电话
			.append( " salha014,")
			.append( " salha015,") //送货地址
			.append( " salha016,")
			.append( " salha017,")
			.append( " salha025,")//审核状态
			.append( " salha026,")//结案状态
			.append( " nvl(salha038,0) salha038,")//是否暂定
			.append( " salha027,")//备注
			.append( " to_char(salha055,'YYYY-MM-DD') salha055,")
			.append( " salha090,")//销售凭证号
			.append( " salha161,")//导购签名
			.append( " salha162,")//客户签名
			.append( " (select sum(nvl(salhb020,0)) from tb_salhb where salhb001 = salha_id) sumhb020,")//商品总金额
			.append( " (select sum(nvl(salhb033,0)) from tb_salhb where salhb001 = salha_id) sumhb033,")//整单折扣金额
			.append( " salha028,")//应收金额
			.append( " (select sum(nvl(salhg013,0)) from tb_salhg where salhg005 = salha_id) salhg013,")//已收金额
			.append( " (select sum(nvl(salhm003,0)) from tb_salhm join tb_pubaa on 1=1 where salhm001 = salha_id) customcost,")//定制费用
			.append( " SALHA099||'%' SALHA099,")
			.append( " cd1.pubcd_id salesoneid,")
			.append( " cd1.pubcd003 salesonename,")
			.append( " cd2.pubcd_id salestwoid,")
			.append( " cd2.pubcd003 salestwoname,")
			.append( " cb.pubcb_id,")
			.append( " cb.pubcb001,")
			.append( " cb.pubcb002,")
			.append("SALHA018,")       /*SALHA018   导购员一         */
			.append("SALHA040,")       /*SALHA040   导购员1分摊比率         */
			.append("cd1.pubcd001 salha018c1,")
			.append("cd1.pubcd003 salha018c2,")
			.append("SALHA034,")       /*SALHA034   导购员二     */
			.append("SALHA041,")       /*SALHA041   导购员2分摊比率      */
			.append("cd2.pubcd001 salha034c1,")
			.append("cd2.pubcd003 salha034c2,")
			.append("SALHA051,")       /*SALHA051   导购员三     */
			.append("SALHA052,")       /*SALHA052   导购员3分摊比率      */
			.append("cd3.pubcd001 salha051c1,")
			.append("cd3.pubcd003 salha051c2,")
			.append("SALHA069,")       /*SALHA069   导购员四     */
			.append("SALHA070,")       /*SALHA070   导购员4分摊比率      */
			.append("cd4.pubcd001 salha069c1,")
			.append("cd4.pubcd003 salha069c2,")
			.append("SALHA071,")       /*SALHA071   导购员五     */
			.append("SALHA072,")       /*SALHA072   导购员5分摊比率      */
			.append("cd5.pubcd001 salha071c1,")
			.append("cd5.pubcd003 salha071c2,")
			.append("salha178,")
			.append("salha179,")
			.append("decode(salha179,'1','零售上样','2','零售私宅','3','卡内货量','4','卡外货量') salha179c,")
			.append("decode(salha180,'1','家具卡','2','投资度假','3','刚需','4','改善','5','高定') salha180c,")
			.append("decode(salha181,'1','公寓','2','洋房','3','叠墅/复式','4','别墅') salha181c,")
			.append("salha180,")
			.append("salha181,")
			.append("salha182,salha193,")
			.append("decode(salha193,'1','样板房销售','2','设计师见面会','3','社区店','4','集装箱','5','电商','6','门店','') salha193C,")
			.append("salha194,")
			.append("salha195,")
			.append("to_char(salha196,'YYYY-MM-DD') salha196")
			.append(" from "+dbname+"tb_salha ha")
			.append(" join tb_pubcb cb on cb.pubcb_id = salha005")
			.append(" left join tb_pubbf bf on salha036=pubbf_id")
			.append(" left join tb_pubbe be on pubbf003=pubbe_id")
			.append(" left join tb_pubbd bd on pubbe003=pubbd_id")
			.append(" join tb_pubcd cd1 on cd1.pubcd_id = salha018")
			.append(" left join tb_pubcd cd2 on cd2.pubcd_id = salha034")
			.append(" left join tb_pubcd cd3 on cd3.pubcd_id = salha051")
			.append(" left join tb_pubcd cd4 on cd4.pubcd_id = salha069")
			.append(" left join tb_pubcd cd5 on cd5.pubcd_id = salha071")
			.append(" where salha_id = ").append(billid);

			System.out.println("sql.toString()======>"+sql.toString());

			JSONObject tbsalha = new JSONObject();
			try {
				item = dao.findBySql(sql.toString(), session);
				ResultSet rs = item.getRs();
				while(rs.next()){
					tbsalha.put("salha_id", rs.getString("salha_id"));
					tbsalha.put("salha002", rs.getString("salha002"));
					tbsalha.put("salha003", rs.getString("salha003"));
					tbsalha.put("salha005c2", rs.getString("salha005c2"));
					tbsalha.put("salha010", nullToEmptyStr(rs.getString("salha010")));
					tbsalha.put("salhb018c2", rs.getString("salha018c2"));
					tbsalha.put("salhb034c2", rs.getString("salha034c2"));
					tbsalha.put("pubgo057", nullToEmptyStr(rs.getString("pubgo057")));
					tbsalha.put("salha012", nullToEmptyStr(rs.getString("salha012")));
					tbsalha.put("salha014", nullToEmptyStr(rs.getString("salha014")));
					tbsalha.put("salha015", nullToEmptyStr(rs.getString("salha015")));
					tbsalha.put("salha025", nullToEmptyStr(rs.getString("salha025")));
					tbsalha.put("salha026", nullToEmptyStr(rs.getString("salha026")));
					tbsalha.put("salha027", nullToEmptyStr(rs.getString("salha027")));
					tbsalha.put("salha038", nullToEmptyStr(rs.getString("salha038")));
					tbsalha.put("salha090", nullToEmptyStr(rs.getString("salha090")));
					tbsalha.put("sumhb020", nullToZero(rs.getString("sumhb020")));
					tbsalha.put("sumhb033", nullToZero(rs.getString("sumhb033")));
					tbsalha.put("salha028", nullToZero(rs.getString("salha028")));
					tbsalha.put("salhg013", nullToZero(rs.getString("salhg013")));
					tbsalha.put("customcost", nullToZero(rs.getString("customcost")));
					tbsalha.put("SALHA099", nullToZero(rs.getString("salha099")));
					tbsalha.put("pubbf001", nullToEmptyStr(rs.getString("pubbf001")));
					tbsalha.put("pubbf002", nullToEmptyStr(rs.getString("pubbf002")));
					tbsalha.put("salha016", nullToEmptyStr(rs.getString("salha016")));//楼层
					tbsalha.put("salha017", nullToEmptyStr(rs.getString("salha017")));//是否有电梯
					tbsalha.put("salha055", nullToEmptyStr(rs.getString("salha055")));//预约送货日期
					tbsalha.put("salesoneid", nullToEmptyStr(rs.getString("salesoneid")));
					tbsalha.put("salesonename", nullToEmptyStr(rs.getString("salesonename")));
					tbsalha.put("salestwoid", nullToEmptyStr(rs.getString("salestwoid")));
					tbsalha.put("salestwoname", nullToEmptyStr(rs.getString("salestwoname")));
					tbsalha.put("pubcb_id", nullToEmptyStr(rs.getString("pubcb_id")));
					tbsalha.put("pubcb001", nullToEmptyStr(rs.getString("pubcb001")));
					tbsalha.put("pubcb002", nullToEmptyStr(rs.getString("pubcb002")));
					tbsalha.put("salha178", nullToEmptyStr(rs.getString("salha178")));
					tbsalha.put("salha179", nullToEmptyStr(rs.getString("salha179")));
					tbsalha.put("salha180", nullToEmptyStr(rs.getString("salha180")));
					tbsalha.put("salha181", nullToEmptyStr(rs.getString("salha181")));
					tbsalha.put("salha179c", nullToEmptyStr(rs.getString("salha179c")));
					tbsalha.put("salha180c", nullToEmptyStr(rs.getString("salha180c")));
					tbsalha.put("salha181c", nullToEmptyStr(rs.getString("salha181c")));
					tbsalha.put("salha182", nullToEmptyStr(rs.getString("salha182")));
					tbsalha.put("typeid", nullToEmptyStr(rs.getString("salha193")));
					tbsalha.put("typename", nullToEmptyStr(rs.getString("salha193C")));

					tbsalha.put("typehouse", nullToEmptyStr(rs.getString("salha194")));
					tbsalha.put("typeday", nullToEmptyStr(rs.getString("salha195")));
					tbsalha.put("finishday", nullToEmptyStr(rs.getString("salha196")));
					//					tbsalha.put("oaid", nullToEmptyStr(oaid));
					JSONArray guidelist = new JSONArray();
					JSONObject one = new JSONObject();
					one.put("id", rs.getLong("salha018"));
					one.put("code", rs.getString("salha018c1"));
					one.put("name", rs.getString("salha018c2"));
					one.put("ratios", rs.getDouble("SALHA040"));
					guidelist.add(one);
					if (rs.getLong("salha034")>0){
						JSONObject two = new JSONObject();
						two.put("id", rs.getLong("salha034"));
						two.put("code", rs.getString("salha034c1"));
						two.put("name", rs.getString("salha034c2"));
						two.put("ratios", rs.getDouble("SALHA041"));
						guidelist.add(two);
					}
					if (rs.getLong("SALHA051")>0){
						JSONObject three = new JSONObject();
						three.put("id", rs.getLong("SALHA051"));
						three.put("code", rs.getString("salha051c1"));
						three.put("name", rs.getString("salha051c2"));
						three.put("ratios", rs.getDouble("SALHA052"));
						guidelist.add(three);
					}
					if (rs.getLong("SALHA069")>0){
						JSONObject four = new JSONObject();
						four.put("id", rs.getLong("SALHA069"));
						four.put("code", rs.getString("salha069c1"));
						four.put("name", rs.getString("salha069c2"));
						four.put("ratios", rs.getDouble("SALHA070"));
						guidelist.add(four);
					}
					if (rs.getLong("SALHA071")>0){
						JSONObject five = new JSONObject();
						five.put("id", rs.getLong("SALHA071"));
						five.put("code", rs.getString("salha071c1"));
						five.put("name", rs.getString("salha071c2"));
						five.put("ratios", rs.getDouble("SALHA072"));
						guidelist.add(five);
					}
					tbsalha.put("guidelist", guidelist);
					try {
						String[] args = AppIOFlowUtil.connectServer();
						String[] apks = GetApkUrl();
						if ("".equals(nullToEmptyStr(rs.getString("salha161")))){
							tbsalha.put("salha161", "");
						}else{
							String url=AppIOFlowUtil.getImageUrl(args, apks[0], apks[1], "", rs.getString("salha161")); 
							tbsalha.put("salha161", nullToEmptyStr(url));
						}
						if ("".equals(nullToEmptyStr(rs.getString("salha162")))){
							tbsalha.put("salha162", "");
						}else{
							String url=AppIOFlowUtil.getImageUrl(args, apks[0], apks[1], "", rs.getString("salha162")); 
							tbsalha.put("salha162", nullToEmptyStr(url));
						}
					} catch (Exception e) {
						e.printStackTrace();
						//						throw e;
					}
				}
			} catch (Exception e) {
				throw e;
			} finally {
				if (item!=null) item.close();
			}

			try {
				//↓下面是产品列表
				sql.setLength(0);
				sql.append( " select salhb_id,nvl(pmtya001,pubhd001) pubhd001,")
				.append( " nvl(pmtya002,pubhd002) pubhd002,")
				.append( " decode(PUBHD016,'1',HH1.PUBHH004||':'||HH1.PUBHH003,'')||")
				.append( " decode(PUBHD017,'1',','||HH2.PUBHH004||':'||HH2.PUBHH003,'')||")
				.append( " decode(PUBHD018,'1',','||HH3.PUBHH004||':'||HH3.PUBHH003,'')||")
				.append( " decode(PUBHD019,'1',','||HH4.PUBHH004||':'||HH4.PUBHH003,'')||")
				.append( " decode(PUBHD020,'1',','||HH5.PUBHH004||':'||HH5.PUBHH003,'') as weidu,")
				.append( " nvl(hb1.jine,salhb019)salhb019,") //折后单价
				.append( " case when salhb076>0 then 1 else salhb014 end salhb014,") //订货数量
				.append( " nvl(pubhd006,(select pubim007 from tb_pubim where pubim001 = salhb002 and pubim009 = '1' and rownum = 1)) pubim007,")//图片
				.append( " to_char(trunc(salhb072,'DD'),'YYYY-MM-DD') salhb072,")//送货日期
				.append( " (case when salhb074 = '1' then '送货安装' ")
				.append( "       when salhb074 = '2' then '仓库自提安装'")
				.append( "       when salhb074 = '3' then '仓库自提不安装'")
				.append( "       when salhb074 = '4' then '现场自提不安装'")
				.append( "       when salhb074 = '5' then '送货不安装'")
				.append( "       when salhb074 = '6' then '现场自提安装'")
				.append( " end) as salhb074,")//提货方式
				.append( " (case when SALHB014-NVL(HE.HE034,0)-NVL(SALHB029,0)-nvl(SALHB068,0)-nvl(cc.CC067,0)-nvl(SALHB049,0)>0 then '待采购' ")
				.append( "       when nvl(SALHB049,0)>0 then '已采购'")
				.append( "       when nvl(cc.CC067,0)=SALHB014-NVL(HE.HE034,0)-NVL(SALHB029,0)-nvl(SALHB068,0) and nvl(SALHB043,0)=0 and NVL(SALHB029,0)<>SALHB014-NVL(HE.HE034,0) then '已到货'")
				.append( "       when SALHB043=SALHB014-NVL(HE.HE034,0)-NVL(SALHB029,0) and nvl(SALHB043,0)>0 then '已排车'")
				.append( "       when SALHB014=NVL(HE.HE034,0) then '已退货'")
				.append( "       when NVL(SALHB029,0)=SALHB014-NVL(HE.HE034,0) then '已签收'")
				.append( " end) as prostatus,")//订单状态
				.append( " case when nvl(SALHB058,0)>0 then '1' else '0' end sysbs007,")
				.append( " SALHB016||'%' SALHB016,")
				.append( " case when nvl(SALHB076,0)>0 then '1' else '0' end ismeal")
				.append( " from")
				.append( " (select hb.*,")
				.append( " row_number() over(partition by (case when salhb076>0 then salhb_id else salhb_id end) order by salhb_id asc) rn")
				.append( " from "+dbname+"tb_salhb hb")
				.append( " where salhb001 = ").append(billid)//导购员id
				.append( " ) hb")
				.append( " LEFT JOIN (SELECT SUM(SALHE034) HE034, SALHE002")
				.append( "			  FROM "+dbname+"TB_SALHD")
				.append( "			  JOIN "+dbname+"TB_SALHE ON SALHE001 = SALHD_ID")
				.append( "			 WHERE SALHD021 = 'Y' AND SALHD010 IS NOT NULL")
				.append( "			 GROUP BY SALHE002) HE ON HE.SALHE002 = HB.SALHB_ID")
				.append( " LEFT JOIN (SELECT SUM(BATCC006 - BATCC007) CC067, BATCC005")
				.append( "			  FROM "+dbname+"TB_BATCC")
				.append( "			 WHERE BATCC003 = '204' AND BATCC006 - BATCC007 > 0")
				.append( "			 GROUP BY BATCC005) CC ON CC.BATCC005 = HB.SALHB_ID")
				.append( " LEFT JOIN TB_PUBHH HH1 ON HB.SALHB006 = HH1.PUBHH_ID")
				.append( " LEFT JOIN TB_PUBHH HH2 ON HB.SALHB007 = HH2.PUBHH_ID")
				.append( " LEFT JOIN TB_PUBHH HH3 ON HB.SALHB008 = HH3.PUBHH_ID")
				.append( " LEFT JOIN TB_PUBHH HH4 ON HB.SALHB009 = HH4.PUBHH_ID")
				.append( " LEFT JOIN TB_PUBHH HH5 ON HB.SALHB010 = HH5.PUBHH_ID")
				.append( " left join "+dbname+"tb_pmtya ya on ya.pmtya_id = salhb076 and 1=2")
				.append( " left join (select sum(salhb014*salhb019) jine,salhb076 from "+dbname+"tb_salhb where salhb076>0 and salhb001 =")
				.append(billid).append(" group by salhb076) hb1 on 1=2")
				.append(" join tb_pubhd on pubhd_id = salhb002")
				.append( " where rn = 1")
				.append(" ORDER BY SALHB_ID");
				item = dao.findBySql(sql.toString(), session);
				String[][] fieldMap = new String[][] {
						{"SALHB_ID", "salhb_id"},
						{"PUBHD001", "pubhd001"},
						{"PUBHD002", "pubhd002"},
						{"WEIDU", "weidu"},
						{"SALHB019", "salhb019"},
						{"SALHB014", "salhb014"},
						{"PUBIM007", "pubim007"},
						{"SALHB072", "salhb072"},
						{"SALHB074", "salhb074"},
						{"PROSTATUS", "prostatus"},
						{"SYSBS007", "sysbs007"},
						{"SALHB016", "salhb016"},
						{"ISMEAL", "ismeal"}
				};
				tbsalha.put("tbsalhb", itemToJson(item,fieldMap,0,0,"pubim007"));
			} catch (Exception e) {
				throw e;
			} finally {
				if (item!=null) item.close();
			}

			//		}else{//商品列表调用
			//	    	sql.setLength(0);
			//			sql.append( " select pubqz002,pubqz003,pubqz003 url")
			//			.append( " from tb_pubqz ")
			//			.append( " where pubqz001 = "+pubhd_id);
			//	    }
			//		System.out.println("查询附件SQL: " + sql.toString());
			//		fieldMap = new String[][] {
			//				{"PUBQZ002", "name"},
			//				{"PUBQZ003", "sysname"},
			//				{"URL", "url"}
			//			};
			//		try {
			//			item = dao.findBySql(sql.toString(), session);
			//			list.put("enclosure", itemToJson(item,fieldMap,null,null,"url"));
			//		} catch (Exception e) {
			//			throw e;
			//		} finally {
			//			if (item!=null)
			//				item.close();
			//		}
			try {
				//↓下面是销售单头附件
				sql.setLength(0);
				sql.append( " select salku002,salku004,salku004 url,pubcd003 upper,to_char(salku006,'YYYY-MM-DD') uptime")
				.append( " from "+dbname+"tb_salku")
				.append( " left join tb_pubcd on pubcd_id = salku005")
				.append( " where salku001 = ").append(billid);
				item = dao.findBySql(sql.toString(), session);
				String[][] fieldMap = new String[][] {
						{"SALKU002", "salku002"},
						{"SALKU004", "salku004"},
						{"URL", "url"},
						{"UPPER", "upper"},
						{"UPTIME", "uptime"}
				};
				tbsalha.put("tbsalku", itemToJson(item,fieldMap,0,0,"url"));
			} catch (Exception e) {
				throw e;
			} finally {
				if (item!=null) item.close();
			}

			try {
				//↓下面是费用列表
				sql.setLength(0);
				sql.append( " select pubgk002,")
				.append( " salhm003")
				.append( " from "+dbname+"tb_salhm")
				.append( " join tb_pubgk on salhm002=pubgk_id")
				.append( " where salhm001 = ").append(billid);
				item = dao.findBySql(sql.toString(), session);
				String[][] fieldMap = new String[][] {
						{"PUBGK002", "salhm002"},
						{"SALHM003", "salhm003"}
				};
				tbsalha.put("tbsalhm", itemToJson(item,fieldMap));
			} catch (Exception e) {
				throw e;
			} finally {
				if (item!=null) item.close();
			}

			/*try {
				//↓下面是报修列表
				sql.setLength(0);
			    sql.append( " select serdg_id,serdg002,to_char(trunc(serdg004,'DD'),'yyyy-mm-dd') serdg004")
				   .append( " from tb_serdg")
				   .append( " where serdg003 = ").append(salha_id);
			    item = dao.findBySql(sql.toString(), session);
			    String[][] fieldMap = new String[][] {
					{"SERDG_ID", "serdg_id"},
					{"SERDG002", "serdg002"},
					{"SERDG004", "serdg004"}
				};
				tbsalha.put("tbserdg", itemToJson(item,fieldMap));
			} catch (Exception e) {
				throw e;
			} finally {
				if (item!=null)
					item.close();
			}*/

			try {
				//↓下面是退货列表
				sql.setLength(0);
				sql.append( " select salhd_id,salhd002,to_char(trunc(salhd003,'DD'),'yyyy-mm-dd') salhd003")
				.append( " from "+dbname+"tb_salhd")
				.append( " where SALHD010 = ").append(billid);
				item = dao.findBySql(sql.toString(), session);
				String[][] fieldMap = new String[][] {
						{"SALHD_ID", "salhd_id"},
						{"SALHD002", "salhd002"},
						{"SALHD003", "salhd003"}
				};
				tbsalha.put("tbsalhd", itemToJson(item,fieldMap));
			} catch (Exception e) {
				throw e;
			} finally {
				if (item!=null) item.close();
			}
			JSONObject result = getSuccessResult();
			result.put("rpdata", tbsalha);
			return result.toJSONString();
		}catch(Exception e){
			e.printStackTrace();
			return getFailureResult().toString();
		}finally {
			if (session != null) session.close();
			if (item != null) item.close();
		}
	}

	/**
	 * 根据openid，获取顾客全账套销售单
	 * @param request
	 * @param response
	 * @param paramJson
	 * @return
	 * @throws CYException
	 */
	public static String wechatGetOrderList(HttpServletRequest request, HttpServletResponse response, String paramJson) 
	throws CYException {
		JSONObject params = JSONObject.parseObject(paramJson);
		String openid = params.getString("openid");
		String begin = params.getString("begin");
		String pagesize = params.getString("pagesize");

		Session session = null;
		UtilsDAO dao = null;
		Item it = null;
		ResultSet rs = null;
		StringBuilder sb = null;
		StringBuilder bigsb = null;
		String where = "";
		try {
			dao = new UtilsDAO();
			session = dao.getNilSession();
			dao.setDbName(HibernateSessionFactory.DefaultDbName);
			String sql = " select phone "
				+" from tb_wechat "
				+" where rownum = 1 "
				+" and openid = '" + openid +"'";
			String phone = session.createSQLQuery(sql).uniqueResult().toString();

			if(null==phone || "".equals(phone)){
				return WechatApi.getFailureResult("当前openId没有绑定手机号");
			}
			
			where+=" and salha012 = '"+phone+"'";

			String dbname = "", pubca_id = "";
			String sqlCheck = " select pubca_id,pubca030 from tb_pubca where pubca030 <> 'nywms' order by pubca_id ";
			it = dao.findBySql(sqlCheck, session);
			rs = it.getRs();

			sb = new StringBuilder();
			bigsb = new StringBuilder();
			while(rs.next()){
				dbname = rs.getString("pubca030")+".";
				pubca_id = rs.getString("pubca_id");

				sb.setLength(0);
				sb.append( " select ha.salha_id as billid,")
				.append(pubca_id + " as pubcaid,") 
				.append( " ha.salha002,") 
				.append( " to_char(ha.salha003,'YYYY-MM-DD HH24:MI:SS') salha003,") 
				.append( " ha.salha010,") 
				.append( " ha.salha012,") 
				.append( " ha.salha015,")
				.append( " ha.salha025,")
				.append( " ha.salha026,")
				.append( " ha.salha028,") 
				.append( " hg.salhg013,")
				.append( " decode(nvl(SALHA038,0),'1','暂定单','正式单') billdesc,")
				.append( " hb.pubhd001,")
				.append( " hb.pubhd002,")
				.append( " hb.pubim007,")
				.append( " ismeal,")
				.append( " '1' salecontract,")
				.append( " case when salha025='Y' and SALHA058 <> '3' and salha028>0 and nvl(bd.con,0)=0 then '1' else '0' end ticketopen,")
				.append( " xx.hb014 total, ")
				//		           .append( " case when salha025='Q' then '0' when nvl(bb.salha_id,0)>0 then '1' else '0' end returngoods,")
				.append( " '0' returngoods,")
				//		           .append( " case when salha025='Q' then '0' when nvl(cc.salha_id,0)>0 then '1' else '0' end canrepair,")
				.append( " '0' canrepair,")
				//		           .append( " case when salha025='Q' then '0' when nvl(hg.salhg013,0)>=ha.salha028 then '1' when nvl(hd.con,0) > 0 then '1' when nvl(ha.salha030,'0') = '1' then '1' else '0' end ifpayment,")
				//		           .append( " case when salha025='Q' then '1' when nvl(hg.salhg013,0)>=ha.salha028 then '0' when nvl(hd.con,0) > 0 then '0' when nvl(ha.salha030,'0') = '1' then '0' else '1' end ifpayment,")
				.append(" case when (nvl(ha.salha028,0)-nvl(M.C,0)-nvl(cg.b,0)+nvl(nhj.salhi011,0)) > 0 then '1' else '0' end ifpayment,")//未交款金额=货款-已交款-退货款-退款金额
				.append( " case when salha025='Q' then '审批中'")
				.append( " when salha026='Y' then '已结案'")
				.append( " when salha025='N' then '未审核'")
				.append( " when salha025='Y' then '已审核'")
				.append( " end billstatus,")
				.append( " case when salha025='N' then '1' else '0' end modifybutton,")
				//				   .append( " case when nvl(salif.con,0)=0 then '1' else '0' end desreturn")
				.append( " '0' desreturn,")
				.append( " nvl(ha.salha174,'1') salesattr,")
				//				   .append( " case when "+banfang+"=1 then '0' when salha025='N' then '1' else '0' end isaudting,")
				//				   .append( " case when "+banfang+"=1 then '0' when salha025='N' and nvl(pubcb052,'0')='0' then '1' else '0' end isreason,")
				.append( " case when salha025='N' then '1' else '0' end isaudting,")
				.append( " case when salha025='N' and nvl(pubcb052,'0')='0' then '1' else '0' end isreason,")
				.append( " nvl(salha199,0) salha199, ")
				.append( " salha200, ")
				.append( " salha201, ")
				.append( " salha182 ")
				.append( " from  ").append(dbname).append("tb_salha ha")
				.append( " left join tb_pubfa fa on fa.pubfa_id = ha.salha001")
				.append( " left join tb_sysac ac on fa.pubfa003 = ac.sysac_id")
				.append( " left join tb_pubcb cb on cb.pubcb_id = ha.salha005")
				.append( " left join tb_pubcd cd1 on cd1.pubcd_id = ha.salha018")
				.append( " left join tb_pubcd cd2 on cd2.pubcd_id = ha.salha034")
				.append( " left join ( ")
				.append( " 	select salhg005,sum(nvl(salhg013,0)) salhg013 from  ").append(dbname).append("tb_salhg group by salhg005 ")
				.append( " ) hg on hg.salhg005 = ha.salha_id ")
				.append( " left join ( ")
				.append( " 	select count(1) con,TAXBD009 from  ").append(dbname).append("tb_taxbd group by TAXBD009 ")
				.append( " ) bd on bd.TAXBD009 = ha.salha_id ")
				.append( " left join (select count(1) con, SALHD010")
				.append( " 				from  ").append(dbname).append("tb_salhd where SALHD021 = 'N' ")
				.append( "           group by SALHD010) hd on hd.SALHD010 = ha.salha_id ")
				.append( " left join (select sum(salhb014) hb014,salhb001 from  ").append(dbname).append("tb_salhb group by salhb001) xx on xx.salhb001=ha.salha_id")
				.append( " join ( ")
				.append( " 	select * from ( ")
				.append( "     select salhb001,pubhd001,pubhd002, ")
				.append( " 	  ROW_NUMBER() OVER (PARTITION BY salhb001 ORDER BY pubhd001) rn, ")
				.append( " 	  pubhd006 pubim007, ")
				.append( "     case when nvl(SALHB076,0)>0 then '1' else '0' end ismeal")
				.append( " 	  from  ").append(dbname).append("tb_salhb  ")
				.append( " 	  join tb_pubhd on salhb002 = pubhd_id  ")
				.append( " 	) where rn = 1 ")
				.append( " ) hb on hb.salhb001 = ha.salha_id ")
				.append(" LEFT JOIN ( ")
				.append(" 	SELECT CA.SALHG005,SUM(nvl(CA.SALHG013,0)) C ")
				.append(" 	FROM  ").append(dbname).append("TB_SALHG CA ")
				.append(" 	GROUP BY CA.SALHG004,CA.SALHG005) M ON HA.SALHA_ID = M.SALHG005 ")
				.append(" left join ( ")
				.append("    select hd.salhd010,nvl(sum(he.salhe021),0) as b ")
				.append("    from  ").append(dbname).append("tb_salhd hd ")
				.append("    join  ").append(dbname).append("tb_salhe he on hd.salhd_id=he.salhe001 and he.salhe004='1' ")
				.append("    where hd.flag=0 and hd.salhd087='Y' ")
				.append("    group by hd.salhd010) cg on ha.salha_id = cg.salhd010 ")
				.append(" left join (select hd.salhd010,nvl(sum(hi.salhi011),0) as salhi011 ")
				.append("    from  ").append(dbname).append("tb_salhd hd ")
				.append("	left join  ").append(dbname).append("tb_salhi hi on hd.salhd_id=hi.salhi004 ")
				.append("    where hd.flag=0 and hd.salhd021='Y' and salhi017='Y' ") 
				.append("    group by hd.salhd010) nhj on ha.salha_id = nhj.salhd010 ")
				.append( " where 1=1 and salha175 = (select pubaa001 from tb_pubaa)")
				.append( " and upper(ac.sysac001) <> 'SALI151_2'")
				.append(where);
				//				.append( " order by SALHA003 desc,SALHA002 desc");
				if(!rs.isLast()){
					sb.append("  union all ");
				}
				bigsb.append(sb);
			}
		}catch(Exception e){
			e.printStackTrace();			
		} finally {
			//			if(session!=null) session.close();
			if (it!=null) it.close();
		}

		try{
			//			session = dao.getNilSession();
			//			dao.setDbName(HibernateSessionFactory.DefaultDbName);

			String[][] fieldMap = new String[][] {
					{"BILLID", "billid"},//销售单ID
					{"PUBCAID", "pubcaid"},//销售单ID
					{"SALHA002", "salha002"},//销售单号
					{"SALHA003", "salha003"},//日期
					{"SALHA010", "salha010"},//顾客姓名
					{"SALHA012", "salha012"},//电话
					{"SALHA015", "salha015"},//送货地址
					{"SALHA025", "salha025"},//审核状态
					{"SALHA026", "salha026"},//结案状态
					{"SALHA028", "salha028"},//折后金额
					{"SALHG013", "salhg013"},//已交款金额
					{"BILLDESC", "billdesc"},//暂定单/正式单
					{"PUBHD001", "pubhd001"},//商品编码
					{"PUBHD002", "pubhd002"},//商品名称
					{"PUBIM007", "pubim007"},//图片名称
					{"ISMEAL", "ismeal"},
					{"TOTAL", "total"},//产品数量
					{"RETURNGOODS", "returngoods"},//是否可退货
					{"SALECONTRACT", "salecontract"},//是否显示销售合同按钮
					{"TICKETOPEN", "ticketopen"},//是否显示开票申请按钮
					{"CANREPAIR", "canrepair"},//是否可报修
					{"IFPAYMENT", "ifpayment"},
					{"MODIFYBUTTON", "modifybutton"},//是否可修改
					{"BILLSTATUS", "billstatus"},
					{"DESRETURN", "desreturn"},//是否显示设计师返点按钮
					{"SALESATTR", "salesattr"},//1自营2联营
					{"ISAUDTING", "isaudting"},//1显示0不显示
					{"ISREASON", "isreason"},//1显示0不显示

					{"SALHA199", "salha199"},//1显示0不显示
					{"SALHA200", "salha200"},//1显示0不显示
					{"SALHA201", "salha201"},//1显示0不显示
					{"SALHA182", "SALHA182"}//1显示0不显示
			};
			System.out.println("SQL: " + bigsb.toString());
			int count = queryCount(bigsb, dao, session);
			it = dao.findBySql(bigsb.toString(), session);
			JSONObject result = getSuccessResult();
			JSONObject list = new JSONObject();
			JSONArray jsarr = itemToJson(it,fieldMap,begin,pagesize,"pubim007");
			JSONArray jsarrNew = new JSONArray();
			for (Iterator iterator = jsarr.iterator();iterator.hasNext();){
				JSONObject goods = (JSONObject) iterator.next();
				if ("N".equals(goods.getString("salha026"))&&"Y".equals(goods.getString("salha025"))){//已审核未结案
					//					goods.put("fillnew", getFillnew(goods.getString("salha_id"), dao, usc, session));
				}else{
				}
				goods.put("fillnew", "0");
				goods.put("ticketopen", "0");//隐藏开票申请按钮
				goods.put("returngoods", "0");//隐藏退货功能
				/*if ("1".equals(chadan)){
					goods.put("returngoods", "0");//{"RETURNGOODS", "returngoods"},//是否可退货
					goods.put("salecontract", "0");//{"SALECONTRACT", "salecontract"},//是否显示销售合同按钮
					goods.put("ticketopen", "0");//{"TICKETOPEN", "ticketopen"},//是否显示开票申请按钮
//					goods.put("canrepair", "0");//{"CANREPAIR", "canrepair"},//是否可报修
					goods.put("ifpayment", "1");//{"IFPAYMENT", "ifpayment"},是否交款，0表示未交款或部分交款，需显示交款按钮，1表示已交全款，隐藏交款按钮
					goods.put("modifybutton", "0");//{"MODIFYBUTTON", "modifybutton"},//是否可修改
					goods.put("desreturn", "0");//{"DESRETURN", "desreturn"}//是否显示设计师返点按钮
					goods.put("fillnew", "0");
				}*/
				jsarrNew.add(goods);
			}
			list.put("salha", jsarrNew);
			list.put("count", count);
			result.put("rpdata", list);
			return result.toJSONString();
		}catch(Exception e){
			e.printStackTrace();
			return getFailureResult().toString();
		}finally {
			if (session != null) session.close();
			if (it != null) it.close();
		}
	}

	/**
	 * 销售单状态栏
	 * @param request
	 * @param response
	 * @param paramJson
	 * @return
	 */
	public static String wechatGetOrderProgress(HttpServletRequest request, HttpServletResponse response, String paramJson){
		JSONObject params = JSONObject.parseObject(paramJson);
		String billid = params.getString("billid");
		String pubcaid = params.getString("pubcaid");
		if(null==billid || "".equals(billid)) return WechatApi.getFailureResult("billid不存在！");
		if(null==pubcaid || "".equals(pubcaid)) return WechatApi.getFailureResult("pubcaid不存在！");

		Session session = null;
		Item it = null;
		try {
			UtilsDAO dao = new UtilsDAO();
			session = dao.getNilSession();
			dao.setDbName(HibernateSessionFactory.DefaultDbName);
			String sql = " select pubca030 "
				+" from tb_pubca "
				+" where pubca_id = " + pubcaid + "";
			String dbname = session.createSQLQuery(sql).uniqueResult().toString()+".";

			sql = " select "
				+ "		'合同签署' as contract, "
				+ "		case " 
				+ "			when NVL(SALHB029,0)=SALHB014-NVL(HE034,0) then '全部发货' "
				+ "			when SALHB014-NVL(HE034,0)-NVL(SALHB029,0)>0 and NVL(SALHB029,0)>0 then '部分发货' "
				+ "			when SALHB014-NVL(HE034,0)>0 and NVL(SALHB029,0)=0 then '未发货' "
				+ "			when SALHB014=NVL(HE034,0) then '全部退货' "
				+ "		end as send,"
				+ "		case "
				+ "			when kb010=SALHB014-NVL(HE034,0) then '安装完成' "
				+ "			when NVL(SALHB029,0)=SALHB014-NVL(HE034,0) then '安装中' "
				+ "			when SALHB014-NVL(HE034,0)-NVL(SALHB029,0)>0 and NVL(SALHB029,0)>0 then '安装中' "
				+ "			when SALHB014-NVL(HE034,0)>0 and NVL(SALHB029,0)=0 then '未安装' "
				+ "			when SALHB014=NVL(HE034,0) then '全部退货' "
				+ "		end as install,"
				+ "		case "
				+ "			when kb010=SALHB014-NVL(HE034,0) then '全部验收' "
				+ "			when kb010>0 and kb010<SALHB014-NVL(HE034,0) then '部分验收' "
				+ "			when kb010=0 then '未验收' "
				+ "			else '未验收' "
				+ "		end as checks"
				+ "   from "+dbname+"tb_salha "
				+ "	  join (select salhb001, "
				+ "		 		   sum(SALHB014) salhb014, "
				+ "		 		   sum(SALHB029) salhb029, "
				+ "		 		   sum(SALHB043) salhb043 "
				+ "    		  from "+dbname+"tb_salhb "
				+ "   		 group by salhb001) on salhb001 = salha_id "
				+ "	  left join (select salhd010, "
				+ "			  			sum(salhe034) HE034 "
				+ "	     		   from "+dbname+"tb_salhd "
				+ "	     		   join "+dbname+"tb_salhe on salhd_id = salhe001 "
				+ "	    		  where salhd010 > 0 "
				+ "	      			and salhd021 = 'Y' "
				+ "	    		  group by salhd010) on salhd010 = salha_id "
				+ "   left join (select SERKA019, "
				+ "   					sum(nvl(serkb010,0)) kb010 "
				+ "	     		   from "+dbname+"tb_serka "
				+ "	     		   join "+dbname+"tb_serkb on serka_id = serkb001 "
				+ "	     		  where serka012 = 'Y' "
				+ "	     			and serka003 = '1' "
				+ "	    		  group by SERKA019) on serka019 = salha_id "
				+ "	  where salha_id = "+billid;

			Item itemOm = null;
			JSONObject rpdata = new JSONObject(true);
			try {
				itemOm = dao.findBySql(sql.toString(), session);
				ResultSet rs = itemOm.getRs();
				while(rs.next()){
					rpdata.put("contract", rs.getString("contract"));
					rpdata.put("send", rs.getString("send"));
					rpdata.put("install", rs.getString("install"));
					rpdata.put("checks", rs.getString("checks"));
				}
			} catch (Exception e) {
				throw e;
			} finally {
				if (itemOm!=null)
					itemOm.close();
			}
			JSONObject result = getSuccessResult();
			result.put("rpdata", rpdata);
			return result.toJSONString();
		}catch(Exception e){
			e.printStackTrace();
			return getFailureResult().toString();
		}finally {
			if (session != null)
				session.close();
		}
	}

	/**
	 * 校验手机验证码
	 * session存在服务端，cookie存在客户端
	 * 从请求中拿到当前会话，这个会话存在服务端，往会话中存放验证码
	 * @return
	 */
	public static String wechatValidatePhoneCode(HttpServletRequest request, HttpServletResponse response, String paramJson){
		JSONObject params = JSONObject.parseObject(paramJson);
		String checkCode = params.getString("checkCode");
		//		String phone = params.getString("phone");

		final HttpSession httpSession = request.getSession();
		String checkCode1 = (String) httpSession.getAttribute("checkCode");

		if(checkCode.equals(checkCode1)){
			JSONObject result = getSuccessResult();
			result.put("info", "服务端验证码"+checkCode1+",校验通过！");
			return result.toJSONString();
		}
		JSONObject result = getFailureResult();
		result.put("info", "服务端验证码"+checkCode1+",校验失败！");
		return result.toJSONString();
	}

	/**
	 * 发送短信校验码
	 * @param phone
	 * @return
	 */
	public static String wechatSendPhoneCode(HttpServletRequest request, HttpServletResponse response, String paramJson){
		JSONObject params = JSONObject.parseObject(paramJson);
		String phone = params.getString("phone");
		//		int times = userService.messageSendToday(phone); //二次验证，单个手机号每日发送上限
		//		if(times <= MAX_PER_DAY){
		String checkCode = WechatApi.generateCode();
		final HttpSession httpSession = request.getSession();
		httpSession.setAttribute("checkCode",checkCode);
		//		CheckCodeMessage checkCodeMessage = new CheckCodeMessage(phone,checkCode);
		try {
			//			HttpSender.batchSend(checkCodeMessage);
			WechatApi.sendSms(phone, checkCode);
			//TimerTask实现5分钟后从session中删除checkCode
			final Timer timer=new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					httpSession.removeAttribute("checkCode");
					System.out.println("checkCode删除成功");
					timer.cancel();
				}
			},5*60*1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getSuccessResult().toJSONString();
		//		return "redirect:/index.jsp";
	}

	/**
	 * 解密手机号
	 * @param request
	 * @param response
	 * @param paramJson
	 * @return
	 * {"phoneNumber":"13383430940","purePhoneNumber":"13383430940","countryCode":"86","watermark":{"timestamp":1645753130,"appid":"wxfc26865c7b1f4fdb"}}
	 */
	public static String getPhone(HttpServletRequest request, HttpServletResponse response, String paramJson) {
		JSONObject params = JSONObject.parseObject(paramJson);
		String sessionkey = params.getString("sessionKey");
		if(null==sessionkey || "".equals(sessionkey)) return WechatApi.getFailureResult("sessionKey不存在！");

		String encrypteddata = params.getString("encryptedData");
		if(null==encrypteddata || "".equals(encrypteddata)) return WechatApi.getFailureResult("encryptedData不存在！");

		String iv = params.getString("iv");
		if(null==iv || "".equals(iv)) return WechatApi.getFailureResult("iv不存在！");

		String decrypt = WechatApi.getPhoneNumber(iv, encrypteddata, sessionkey);
		System.out.println("decrypt = " + decrypt);
		JSONObject resultObj = JSONObject.parseObject(decrypt);

		String phone = resultObj.getString("phoneNumber");

		Session session = null;
		String sql = "";
		Transaction tx = null;
		try {
			UtilsDAO dao = new UtilsDAO();
			session = dao.getNilSession();
			dao.setDbName(HibernateSessionFactory.DefaultDbName);

			sql = "select count(1) from TB_WECHAT where sessionkey = '" + sessionkey+"'";
			Double con = ((BigDecimal)session.createSQLQuery(sql).uniqueResult()).doubleValue();

			tx = session.beginTransaction();
			if(con > 0D){
				sql = " update tb_wechat set "
					+ " phone = '"+phone +"' "
					+ " where sessionkey = '"+sessionkey+"' ";
				if(session.createSQLQuery(sql).executeUpdate()<=0) throw new CYException(-1,"修改记录失败！");
			}
			if (tx != null) tx.commit();
		}catch(CYException e){
			e.printStackTrace();
			if (tx != null) tx.rollback();
		}catch(Exception e){
			e.printStackTrace();
			if (tx != null) tx.rollback();
		}finally{
			if (session != null) session.close();
		}

		JSONObject result = getSuccessResult();
		result.put("phone", phone);
		return result.toJSONString();
	}
	
	/**
	 * 校验用户登录状态
	 * @param request
	 * @param response
	 * @param paramJson
	 * @return
	 */
	public static String validateState(HttpServletRequest request, HttpServletResponse response, String paramJson) {
		JSONObject params = JSONObject.parseObject(paramJson);
		String openid = params.getString("openid");
		if(null==openid || "".equals(openid)) return WechatApi.getFailureResult("openid不存在！");
		
		Session session = null;
		String sql = "";
		try {
			UtilsDAO dao = new UtilsDAO();
			session = dao.getNilSession();
			dao.setDbName(HibernateSessionFactory.DefaultDbName);
			
			sql = " select phone "
				+" from tb_wechat "
				+" where rownum = 1 "
				+" and openid = '" + openid +"'";
			String phone = session.createSQLQuery(sql).uniqueResult().toString();

			if(null==phone || "".equals(phone)){
				return WechatApi.getFailureResult("当前openId没有绑定手机号");
			}
			JSONObject result = getSuccessResult();
			result.put("phone", phone);
			return result.toJSONString();
		}catch(Exception e){
			e.printStackTrace();
			return WechatApi.getFailureResult("当前openId没有绑定手机号");
		}finally{
			if (session != null) session.close();
		}
	}

	/**
	 * 根据前端传过来的code,返回openid、session_key
	 * 按道理，为了安全，应该将生成uuid,和session_key关联，uuid传给前端
	 * 但这里没有这样做，辣鸡系统不怕攻击
	 * @param js_code
	 * @return
	 */
	public static String getOpenIdByJSCode(HttpServletRequest request, HttpServletResponse response, String paramJson) {
		JSONObject params = JSONObject.parseObject(paramJson);
		String js_code = params.getString("js_code");
		try {
			String result = WechatApi.getOpenIdByJSCode(js_code);
			JSONObject resultObj = JSONObject.parseObject(result);
			if (resultObj.containsKey("errcode") && resultObj.getIntValue("errcode") > 0) {
				return WechatApi.getFailureResult(result);
			}

			String openid = resultObj.getString("openid");
			String sessionkey = resultObj.getString("session_key");

			Session session = null;
			String sql = "";
			Transaction tx = null;
			try {
				UtilsDAO dao = new UtilsDAO();
				session = dao.getNilSession();
				dao.setDbName(HibernateSessionFactory.DefaultDbName);

				sql = "select count(1) from TB_WECHAT where openid = '" + openid +"'";
				Double con = ((BigDecimal)session.createSQLQuery(sql).uniqueResult()).doubleValue();

				tx = session.beginTransaction();
				if(con == 0D){
					sql = " insert into tb_wechat (openid,sessionkey,uuid,phone) "
						+ " values ("
						+ "'"+openid +"',"
						+ "'"+sessionkey +"',"
						+ "'',"
						+ "'')";
					if(session.createSQLQuery(sql).executeUpdate()<=0) throw new CYException(-1,"新增记录失败！");
				}else{
					sql = " update tb_wechat set "
						+ " sessionkey = '"+sessionkey +"', "
						+ " uuid = '' "
						+ " where openid = '"+openid+"' ";
					if(session.createSQLQuery(sql).executeUpdate()<=0) throw new CYException(-1,"修改记录失败！");
				}
				if (tx != null) tx.commit();
			}catch(CYException e){
				e.printStackTrace();
				if (tx != null) tx.rollback();
			}catch(Exception e){
				e.printStackTrace();
				if (tx != null) tx.rollback();
			}finally{
				if (session != null) session.close();
			}
			JSONObject json = getSuccessResult();
			json.put("openid", openid);
			json.put("session_key", sessionkey);
			return json.toJSONString();
		} catch (Exception e) {
			e.printStackTrace();
			return WechatApi.getFailureResult("获取openid接口出错");
		}
	}
	
}