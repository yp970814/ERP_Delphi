/**
 * 
 */
package com.bz.cy.app.wechat;

/**
 * @author ���ߣ�yuanping
 * @version ����ʱ�䣺2022-1-10����09:30:38
 * @description:
 */
public class WechatConfig {
	
	//253����api url��http://smssh1.253.com/msg/send/json
	public static final String SEND_URL = "http://smssh1.253.com/msg/v1/send/json";
//	public static final String SEND_URL = "http://smssh1.253.com/msg/send/json";

	//253����api �û�����N2536511
	public static final String ACCOUNT= "N2536511";

	//253����api ���룺X86OfCpQTxfabe
	public static final String PASSWORD = "X86OfCpQTxfabe";
	
	/**
	 * С����appid
	 */
	//public static final String AppID = "wx362c31db093e3ccf";
	public static final String XCX_AppID = "wxfc26865c7b1f4fdb";
	/**
	 * С����appsecret
	 */
	//public static final String AppSecret = "cb088f262247bf051f4135d317d8634d";
	public static final String XCX_AppSecret = "a6777a63d9acd33df980446a8d582b39";
	/**
	 * С����ģ����Ϣ
	 */
	public static final String XCX_TEMPLATE_ID = "FQvM76ojefhcfJv5PverLzJ0v6Go2xtgDaEHK1xz8u0";

	/**
	 * ���ں�appid
	 */
	public static final String GZH_AppID = "wx9d0476bb6f3e7cd2";
	/**
	 * ���ں�appsecret
	 */
	public static final String GZH_AppSecret = "a6777a63d9acd33df980446a8d582b39";
	/**
	 * ���ں�ģ����Ϣ
	 */
	public static final String GZH_TEMPLATE_ID = "Kir35YgIQt_d1Utp6t96PvS2pQJoxDqlBNkH5iPaO98";

	public static final String AES = "AES";

	public static final String AES_CBC_PADDING = "AES/CBC/PKCS7Padding";
	
	public static final String TOKEN = "XXX";
	
	public static final String RESP_DATA_TYPE = "JSON";

	public static final String CALLBACK_URL = "";
	
	public static final String Jump_Page = "pages/index/confirm";
}
