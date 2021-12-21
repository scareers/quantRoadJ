package com.scareers.datasource.eastmoney;

import java.security.spec.PSSParameterSpec;

/**
 * description: dfcf 常规设定
 *
 * @author: admin
 * @date: 2021/12/21/021-22:21:52
 */
public class SettingsOfEastMoney {
    /**
     * 同 python. efinance 某些设定
     * EASTMONEY_REQUEST_HEADERS = {
     * 'User-Agent': 'Mozilla/5.0 (Windows NT 6.3; WOW64; Trident/7.0; Touch; rv:11.0) like Gecko',
     * 'Accept': '*\/*',
     * 'Accept-Language':'zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2',
     * 'Referer':'http://quote.eastmoney.com/center/gridlist.html',
     * <p>
     * }
     */

    public static String HEADER_VALUE_OF_USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64; Trident/7.0; Touch; rv:11.0) like Gecko";
    public static String HEADER_VALUE_OF_ACCEPT = "*/*";
    public static String HEADER_VALUE_OF_ACCEPT_LANGUAGE = "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2";
    public static String HEADER_VALUE_OF_REFERER = "http://quote.eastmoney.com/center/gridlist.html";

    public static int DEFAULT_TIMEOUT = 3 * 1000;



}