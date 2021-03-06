package com.scareers.sqlapi;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.selfdb.ConnectionFactory;
import joinery.DataFrame;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.scareers.utils.SqlUtil.execSql;
import static com.scareers.utils.SqlUtil.execSqlQuery;

/**
 * description: mysql本身相关的某些api
 * <p>
 * noti:
 * 1.注意连接对象, 需要设置 &allowMultiQueries=true   允许多条语句一起执行 . 实测不太方便, 建议多次execSql即可
 *
 * @author: admin
 * @date: 2021/11/16/016-5:20
 */
public class MysqlApi {
    public static void main(String[] args) throws Exception {

        Connection connection = ConnectionFactory.getConnLocalFS1MFromEastmoney();

        Console.log(alreadyHasTable(connection,"2022-03-25"));

//        Connection connection = ConnectionFactory.getConnLocalTushare();
//
//
//
//        System.out.println(getDiskUsageOfDB("kline_forms", connection).toString(100));
//
////        Console.com.scareers.log(getMemoryUsageOfBuffer(connection));
//
//        Console.log(getNonBufferedRate(connection)); // 0.0014903166436285268 , 磁盘使用
//        setBufferPoolSizeGB(20, connection);
//        Console.log(getBufferPoolSizeSetted(connection));
//
//
//        connection.close();
    }


    /*
    set @ibpdata = (select variable_value
                from performance_schema.global_status
                where variable_name = 'innodb_buffer_pool_pages_data');
    set @idbpgsize = (select variable_value
                      from performance_schema.global_status
                      where variable_name = 'innodb_page_size');
    set @ibpsize = @ibpdata * @idbpgsize / (1024 * 1024 * 1024);
    select @ibpsize;

    返回mysql buffer 占用内存大小.  单位GB;  同样, 默认不会关闭传递来的conn
     */
    public static double getMemoryUsageOfBuffer(Connection connection) throws Exception {
        execSql("set @ibpdata = (select variable_value\n" +
                "                from performance_schema.global_status\n" +
                "                where variable_name = 'innodb_buffer_pool_pages_data');\n", connection, false);
        execSql("    set @idbpgsize = (select variable_value\n" +
                "                      from performance_schema.global_status\n" +
                "                      where variable_name = 'innodb_page_size');\n", connection, false);
        ResultSet resultSet = execSqlQuery("select @ibpdata * @idbpgsize / (1024 * 1024 * 1024);", connection);
        double res = 0.0;
        while (resultSet.next()) {
            res = resultSet.getDouble(1); // 列从 1开始
        }
        resultSet.close();
        return res;
    }

    /**
     * 获取 从磁盘读取次 / (磁盘读取 + buffer读取) . 表示 buffer失效比率;  非百分比
     *
     * @return
     */
    public static Double getNonBufferedRate(Connection connection) throws SQLException {
        DataFrame<Object> df_ = DataFrame.readSql(connection, "show status like 'innodb_buffer_pool_read%'");

        long innodbBufferPoolReads = 0L; // 磁盘读取次数
        long innodbBufferPoolReadRequests = 0L; // buffer读取次数
        for (int i = 0; i < df_.length(); i++) {
            List<Object> row = df_.row(i);
            if ("Innodb_buffer_pool_reads".equals(row.get(0).toString())) {
                innodbBufferPoolReads = Long.valueOf(row.get(1).toString());
            }
            if ("Innodb_buffer_pool_read_requests".equals(row.get(0).toString())) {
                innodbBufferPoolReadRequests = Long.valueOf(row.get(1).toString());
            }
        }
        if (innodbBufferPoolReadRequests == 0 && innodbBufferPoolReads == 0) {
            return null; // 获取失败
        }
        return (double) innodbBufferPoolReads / (innodbBufferPoolReads + innodbBufferPoolReadRequests);
    }

    /**
     * 获取当前的 Innodb_buffer_pool_size  设定
     *
     * @param connection
     * @return
     */
    public static Long getBufferPoolSizeSetted(Connection connection) throws SQLException {
        DataFrame<Object> df = DataFrame.readSql(connection, "show variables like '%innodb_buffer_pool_size%';");
        return Long.valueOf(df.get(0, 1).toString());
    }

    /**
     * 动态设置 Innodb_buffer_pool_size, 默认情况mysql会调整到1GB整数倍, 这里直接设置GB整数倍
     *
     * @param connection
     * @return
     */
    public static void setBufferPoolSizeGB(int newSizeGB, Connection connection) throws Exception {
        Long rawSize = getBufferPoolSizeSetted(connection);
        Long actualSize = (long) newSizeGB * (1024L * 1024 * 1024);
        execSql(StrUtil.format("SET GLOBAL innodb_buffer_pool_size = {};", actualSize), connection);
        Thread.sleep(3);
        Console.log(StrUtil.format("try to modified MySQL config: Innodb_buffer_pool_size from {} to {};\nbut will " +
                "spend " +
                "some " +
                "time, you can check that call getBufferPoolSizeSetted() later.", rawSize, actualSize));
    }

    public static DataFrame<Object> getDiskUsageOfDB(String dbName, Connection connection) throws SQLException {
        String sql = StrUtil.format("select\n" +
                "        table_schema as 'db',\n" +
                "        table_name as 'tablename',\n" +
                "        table_rows as 'rows',\n" +
                "        truncate(data_length/1024/1024, 2) as 'space(MB)',\n" +
                "        truncate(index_length/1024/1024, 2) as 'index_space(MB)',\n" +
                "        truncate((index_length+data_length)/1024/1024/1024, 2) as 'total(GB)'\n" +
                "        from information_schema.tables\n" +
                "        where table_schema='{}'\n" +
                "        order by table_rows desc, index_length desc;", dbName);
        return DataFrame.readSql(connection, sql);
    }



    /**
     * 给定表名, 返回是否已经存在该表? 常用于判定是否需要建表等情况
     *
     * @param conn
     * @param tableName
     * @return
     */
    @SneakyThrows
    public static boolean alreadyHasTable(Connection conn, String tableName) {
        String sql = "show tables";
        DataFrame<Object> dataFrame = DataFrame.readSql(conn, sql);
        for (int i = 0; i < dataFrame.length(); i++) {
            if (tableName.equals(dataFrame.get(i, 0).toString())) {
                return true;
            }
        }
        return false;
    }
}



