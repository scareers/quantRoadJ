package com.scareers.tools.stockplan.news.bean.dao;

import cn.hutool.core.date.*;
import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.tools.stockplan.news.bean.FourPaperNew;
import com.scareers.tools.stockplan.news.bean.MajorIssue;
import com.scareers.utils.log.LogUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/3/16/016-23:43:25
 */
public class FourPaperNewDao {
    public static void main(String[] args) throws SQLException {
        Console.log(getNewsForTradePlanByDate("2022-03-17"));
        Console.log(getNewsForTradePlanByDateGrouped("2022-03-17"));
    }

    /**
     * 给定日期, 返回当日所有 四大报媒精华新闻
     *
     * @param dateStr 标准日期字符串
     * @return
     * @throws SQLException
     */
    public static List<FourPaperNew> getNewsForTradePlanByDate(String dateStr) {
        // hibernate API, 访问数据库
        Session session = SimpleNewEmDao.sessionFactory.openSession();
        String hql = "FROM FourPaperNew E WHERE E.dateStr=:dateStr"; // 访问发布时间在区间内的新闻列表, 类型==1, 即财经导读
        Query query = session.createQuery(hql);
        query.setParameter("dateStr", dateStr); // 注意类型
        List beans = query.list();
        List<FourPaperNew> res = new ArrayList<>();
        for (Object bean : beans) {
            res.add((FourPaperNew) bean);
        }
        session.close();
        return res;
    }


    /**
     * 给定日期, 返回当日所有 四大报媒精华新闻; 按照原始顺序(新闻中出现的顺序) 已经分类好!
     *
     * @param dateStr
     * @return
     * @throws SQLException
     */
    public static List<FourPaperNew.FourPaperNewBatch> getNewsForTradePlanByDateGrouped(String dateStr)
            throws SQLException {
        // hibernate API, 访问数据库
        List<FourPaperNew> items = getNewsForTradePlanByDate(dateStr);

        List<String> types = new ArrayList<>(); // 维持顺序
        for (FourPaperNew item : items) {
            if (!types.contains(item.getType())) {
                types.add(item.getType());
            }
        }

        HashMap<String, ArrayList<FourPaperNew>> mapped = new HashMap<>();
        for (FourPaperNew item : items) {
            mapped.putIfAbsent(item.getType(), new ArrayList<>());
            mapped.get(item.getType()).add(item);
        }

        List<FourPaperNew.FourPaperNewBatch> res = new ArrayList<>();
        for (String type : types) {
            res.add(new FourPaperNew.FourPaperNewBatch(mapped.get(type), type));
        }
        return res;
    }

    /**
     * 根据id获取bean
     *
     * @param id
     * @return
     */
    public static FourPaperNew getBeanById(long id) {
        Session session = SimpleNewEmDao.sessionFactory.openSession();
        FourPaperNew simpleNewEm = session.get(FourPaperNew.class, id);
        session.close();
        return simpleNewEm;
    }

    /**
     * 保存单个bean
     *
     * @param id
     * @return
     */
    public static void updateBean(FourPaperNew bean) {
        Session session = SimpleNewEmDao.sessionFactory.openSession();
        session.beginTransaction();
        session.update(bean);
        session.getTransaction().commit();
        log.warn("FourPaperNew: 更新成功, id: {}", bean.getId());
        session.close();
    }

    /**
     * 为操盘计划, 获取适当日期的 四大报媒精华新闻 --> 与 重大事件/利好/新闻联播 不一样; 因为它们是 下午晚上, 而四大报媒是二天早上
     * 逻辑:
     * 1.当今日是交易日
     * 当时间<=15:00, 则获取 今日
     * 当时间>=15:00, 则获取 明日 (但是明日很可能没出数据), 此时为明天做操盘计划
     * 2.当今日非交易日
     * 获取上一交易日, 为下一交易日做计划
     *
     * @param dateStr
     * @return
     */
    public static List<FourPaperNew> getNewsForPlan(Date equivalenceNow) throws SQLException {
        String today = DateUtil.format(equivalenceNow, DatePattern.NORM_DATE_PATTERN);
        if (EastMoneyDbApi.isTradeDate(today)) {
            if (DateUtil.hour(equivalenceNow, true) >= 15) { // 超过下午3点
                String nextDate = EastMoneyDbApi.getPreNTradeDateStrict(today, -1);
                return getNewsForTradePlanByDate(nextDate);
            } else {
                return getNewsForTradePlanByDate(today);
            }
        }
        String preDate = EastMoneyDbApi.getPreNTradeDateStrict(today, 1);
        List<FourPaperNew> res = new ArrayList<>();
        DateRange range = new DateRange(DateUtil.parse(preDate), DateUtil.parse(today), DateField.DAY_OF_MONTH,
                1, true, false); // 此时不包含今日
        for (DateTime dateTime : range) { // 这里将是 上一交易日 到 绝对的 昨天, 0点.
            res.addAll(getNewsForTradePlanByDate(DateUtil.format(dateTime, DatePattern.NORM_DATE_PATTERN)));
        }
        return res;
    }



    private static final Log log = LogUtil.getLogger();
}
