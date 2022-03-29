/**
 * 
 */
package com.bz.cy.app.wechat;

import java.util.Map;

/**
 * @author ���ߣ�yuanping
 * @version ����ʱ�䣺2022-2-15����09:40:51
 * @description:
 */
public class WechatTemplate {

    private String touser;
    private String template_id;
    private String page; // ��תС����ҳ��
//    private String page = "pages/index/index"; // ��תС����ҳ��
//    private String form_id; //���ύ������Ϊformid��֧��������Ϊprepay_id
    private Map<String, WechatTemplateItem> data;
//    private String emphasis_keyword; // ��Ҫ�Ŵ�Ĺؼ��֣��磺keyword1.DATA
    
    private String miniprogram_state;//��תС�������ͣ�developerΪ�����棻trialΪ����棻formalΪ��ʽ�棻Ĭ��Ϊ��ʽ��
    private String lang;//����С����鿴�����������ͣ�֧��zh_CN(��������)��en_US(Ӣ��)��zh_HK(��������)��zh_TW(��������)��Ĭ��Ϊzh_CN
    
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