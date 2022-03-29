/**
 * 
 */
package com.bz.cy.app.wechat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * @author ���ߣ�yuanping
 * @version ����ʱ�䣺2022-1-10����09:29:21
 * @description:
 */
public class HttpUtils {

	/**
	 * ����ͨ�ò���timestamp��sig��respDataType
	 * 
	 * @return
	 */
	public static String createCommonParam(String sid,String token) {
		// ʱ���
		long timestamp = System.currentTimeMillis();
		// ǩ��
		String sig = DigestUtils.md5Hex(sid + token + timestamp);
		return "��tamp=" + timestamp + "&sig=" + sig + "&respDataType=" + WechatConfig.RESP_DATA_TYPE;
	}

	/**
	 * post����
	 * 
	 * @param url
	 *            ���ܺͲ���
	 * @param body
	 *            Ҫpost������
	 * @return
	 * @throws IOException
	 */
	public static String post(String url, String body) {
		
		System.out.println("body:" + System.getProperty("line.separator") + body);

		String result = "";
		try {
			OutputStreamWriter out = null;
			BufferedReader in = null;
			URL realUrl = new URL(url);
			URLConnection conn = realUrl.openConnection();
			
			// �������Ӳ���
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(20000);
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			// �ύ����
			out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
			out.write(body);
			out.flush();

			// ��ȡ��������
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			String line = "";
			boolean firstLine = true; // ����һ�в��ӻ��з�
			while ((line = in.readLine()) != null) {
				if (firstLine) {
					firstLine = false;
				} else {
					result += System.getProperty("line.separator");
				}
				result += line;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * �ص����Թ��߷���
	 * 
	 * @param url
	 * @return
	 */
	public static String postHuiDiao(String url, String body) {
		String result = "";
		try {
			OutputStreamWriter out = null;
			BufferedReader in = null;
			URL realUrl = new URL(url);
			URLConnection conn = realUrl.openConnection();

			// �������Ӳ���
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(20000);

			// �ύ����
			out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
			out.write(body);
			out.flush();

			// ��ȡ��������
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			String line = "";
			boolean firstLine = true; // ����һ�в��ӻ��з�
			while ((line = in.readLine()) != null) {
				if (firstLine) {
					firstLine = false;
				} else {
					result += System.getProperty("line.separator");
				}
				result += line;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

}