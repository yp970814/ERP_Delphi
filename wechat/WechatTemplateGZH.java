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
public class WechatTemplateGZH {

    private String appid;
    private String template_id;
    private String url;
    private Map<String, String> miniprogram;
    private Map<String, WechatTemplateItem> data;
    
	public String getAppid() {
		return appid;
	}
	public void setAppid(String appid) {
		this.appid = appid;
	}
	public String getTemplate_id() {
		return template_id;
	}
	public void setTemplate_id(String template_id) {
		this.template_id = template_id;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public Map<String, String> getMiniprogram() {
		return miniprogram;
	}
	public void setMiniprogram(Map<String, String> miniprogram) {
		this.miniprogram = miniprogram;
	}
	public Map<String, WechatTemplateItem> getData() {
		return data;
	}
	public void setData(Map<String, WechatTemplateItem> data) {
		this.data = data;
	}
    
}