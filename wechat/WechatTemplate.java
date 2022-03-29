/**
 * 
 */
package com.bz.cy.app.wechat;

import java.util.Map;

/**
 * @author 作者：yuanping
 * @version 创建时间：2022-2-15上午09:40:51
 * @description:
 */
public class WechatTemplate {

    private String touser;
    private String template_id;
    private String page; // 跳转小程序页面
//    private String page = "pages/index/index"; // 跳转小程序页面
//    private String form_id; //表单提交场景下为formid，支付场景下为prepay_id
    private Map<String, WechatTemplateItem> data;
//    private String emphasis_keyword; // 需要放大的关键字，如：keyword1.DATA
    
    private String miniprogram_state;//跳转小程序类型：developer为开发版；trial为体验版；formal为正式版；默认为正式版
    private String lang;//进入小程序查看”的语言类型，支持zh_CN(简体中文)、en_US(英文)、zh_HK(繁体中文)、zh_TW(繁体中文)，默认为zh_CN
    
	public String getTouser() {
		return touser;
	}
	public void setTouser(String touser) {
		this.touser = touser;
	}
	public String getTemplate_id() {
		return template_id;
	}
	public void setTemplate_id(String template_id) {
		this.template_id = template_id;
	}
	public String getPage() {
		return page;
	}
	public void setPage(String page) {
		this.page = page;
	}
//	public String getForm_id() {
//		return form_id;
//	}
//	public void setForm_id(String form_id) {
//		this.form_id = form_id;
//	}
	public Map<String, WechatTemplateItem> getData() {
		return data;
	}
	public void setData(Map<String, WechatTemplateItem> data) {
		this.data = data;
	}
//	public String getEmphasis_keyword() {
//		return emphasis_keyword;
//	}
//	public void setEmphasis_keyword(String emphasis_keyword) {
//		this.emphasis_keyword = emphasis_keyword;
//	}
	public String getMiniprogram_state() {
		return miniprogram_state;
	}
	public void setMiniprogram_state(String miniprogram_state) {
		this.miniprogram_state = miniprogram_state;
	}
	public String getLang() {
		return lang;
	}
	public void setLang(String lang) {
		this.lang = lang;
	}
}