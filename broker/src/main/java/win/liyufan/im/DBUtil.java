/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package win.liyufan.im;


import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.hazelcast.util.StringUtil;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.moquette.BrokerConstants;
import io.moquette.server.config.IConfig;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FlywayConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

import static cn.wildfirechat.common.IMExceptionEvent.EventType.RDBS_Exception;

public class DBUtil {
    private static final Logger LOG = LoggerFactory.getLogger(DBUtil.class);
    private static ComboPooledDataSource comboPooledDataSource = null;
    private static ConcurrentHashMap<Long, String>map = new ConcurrentHashMap<>();
    private static ThreadLocal<Connection> transactionConnection = new ThreadLocal<Connection>() {
        @Override
        protected Connection initialValue() {
            super.initialValue();
            return null;
        }
    };

    public static boolean IsEmbedDB = false;
    public static boolean SystemExiting = false;
    public static boolean ClearDBDebugMode = false;

    public static void init(IConfig config) {
        String embedDB = config.getProperty(BrokerConstants.EMBED_DB_PROPERTY_NAME);
        boolean autoCleanMsgs = "true".equals(config.getProperty(BrokerConstants.DB_AUTO_CLEAN_HISTORY_MESSAGES));
        if (embedDB != null && embedDB.equals("1")) {
            IsEmbedDB = true;
            LOG.info("Use h2 database");
        } else {
            IsEmbedDB = false;
            LOG.info("Use mysql database");
        }

        if (comboPooledDataSource == null) {
            String migrateLocation;
            if (IsEmbedDB) {
                migrateLocation = "filesystem:./migrate/h2";
                comboPooledDataSource = new ComboPooledDataSource();

                comboPooledDataSource.setJdbcUrl( "jdbc:h2:./h2db/wfchat;AUTO_SERVER=TRUE;MODE=MySQL" );
                comboPooledDataSource.setUser("SA");
                comboPooledDataSource.setPassword("SA");
                comboPooledDataSource.setMinPoolSize(5);
                comboPooledDataSource.setAcquireIncrement(5);
                comboPooledDataSource.setMaxPoolSize(20);

                comboPooledDataSource.setIdleConnectionTestPeriod(60 * 5);
                comboPooledDataSource.setMinPoolSize(3);
                comboPooledDataSource.setInitialPoolSize(3);

                try {
                    comboPooledDataSource.setDriverClass( "org.h2.Driver" ); //loads the jdbc driver
                } catch (PropertyVetoException e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                    System.exit(-1);
                }
            } else {
                migrateLocation = "filesystem:./migrate/mysql";
                comboPooledDataSource = new ComboPooledDataSource("mysql");
                try {
                    String url01 = comboPooledDataSource.getJdbcUrl().substring(0,comboPooledDataSource.getJdbcUrl().indexOf("?"));
                    String datasourceName = url01.substring(url01.lastIndexOf("/")+1);
                    // 连接已经存在的数据库，如：mysql
                    String jdbc = comboPooledDataSource.getJdbcUrl().replace(datasourceName, "");
                    Connection connection = DriverManager.getConnection(jdbc, comboPooledDataSource.getUser(), comboPooledDataSource.getPassword());
                    Statement statement = connection.createStatement();

                    // 创建数据库
                    statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + datasourceName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");

                    statement.close();
                    connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }

            }
            Flyway flyway = Flyway.configure().dataSource(comboPooledDataSource).locations(migrateLocation).baselineOnMigrate(true).load();
            flyway.migrate();

            if(autoCleanMsgs) {
                cleanHistoryMsg();
            }
        }
    }

    private static void cleanHistoryMsg() {
        new Thread(()->{
            long usedTime = 0;
            while (true) {
                if(SystemExiting) {
                    break;
                }
                try {
                    long sleepTime;
                    if(ClearDBDebugMode) {
                        sleepTime = 60 * 1000 - usedTime;
                    } else {
                        sleepTime = 60 * 60 * 1000 - usedTime;
                    }
                    if(sleepTime < 0) {
                        sleepTime = 5 * 1000;
                    }

                    Thread.sleep(sleepTime);
                    LOG.info("Start clean history messages");

                    if(SystemExiting) {
                        break;
                    }
                    long start = System.currentTimeMillis();
                    if(IsEmbedDB) {
                        clearH2DB();
                    } else {
                        clearMySQL();
                    }
                    usedTime = System.currentTimeMillis() - start;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    private static boolean sleep() {
        if(SystemExiting)
            return true;

        try {
            if(ClearDBDebugMode) {
                Thread.sleep(500);
            } else {
                Thread.sleep(10000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return SystemExiting;
    }

    private static void clearH2DB() {
        boolean hasMore = true;
        while (hasMore) {
            hasMore = clearOneTable("t_user_messages");
            if(sleep()) {
                return;
            }
        }
        hasMore = true;

        while (hasMore) {
            hasMore = clearOneTable("t_messages");
            if(sleep()) {
                return;
            }
        }
    }
    private static void clearMySQL() {
        int i = 0;
        do {
            if(!clearOneTable("t_user_messages_" + i)) {
                i++;
            }

            if(sleep()) {
                return;
            }
        } while (i < 128);

        boolean hasMore = true;
        String msgTableName;
        if(ClearDBDebugMode) {
            msgTableName = MessageShardingUtil.getMessageTable(MessageShardingUtil.getMsgIdFromTimestamp(System.currentTimeMillis()));
        } else {
            msgTableName = MessageShardingUtil.getMessageTable(MessageShardingUtil.getMsgIdFromTimestamp(System.currentTimeMillis()), 1);
        }
        while (hasMore) {
            hasMore = clearOneTable(msgTableName);
            if(sleep()) {
                return;
            }
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        if(cal.get(Calendar.DAY_OF_MONTH) > 25 && !ClearDBDebugMode) {
            hasMore = true;
            msgTableName = MessageShardingUtil.getMessageTable(MessageShardingUtil.getMsgIdFromTimestamp(System.currentTimeMillis()), 2);
            while (hasMore) {
                hasMore = clearOneTable(msgTableName);
                if(sleep()) {
                    return;
                }
            }
        }
    }

    private static boolean clearOneTable(String tableName) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        int clearCount = 999;
        if(ClearDBDebugMode) {
            clearCount = 19;
        }
        boolean hasMore = true;
        try {
            connection = DBUtil.getConnection();
            String sql = "select id, _mid from " + tableName + " order by id limit 1 offset " + clearCount;
            statement = connection.prepareStatement(sql);

            rs = statement.executeQuery();
            int id = 0;
            long mid = 0;
            if (rs.next()) {
                id = rs.getInt(1);
                mid = rs.getLong(2);
            }
            if(id == 0) {
                hasMore = false;
                rs.close();
                rs = null;
                statement.close();

                sql = "select id, _mid from " + tableName + " order by id desc limit 1";
                statement = connection.prepareStatement(sql);

                rs = statement.executeQuery();
                if (rs.next()) {
                    id = rs.getInt(1);
                    mid = rs.getLong(2);
                }
            }

            rs.close();
            rs = null;
            LOG.info("Check history message in table {}, id {}, mid {}, hasMore {}", tableName, id, mid, hasMore);
            if(id != 0) {
                Calendar tm = MessageShardingUtil.getCalendarFromMessageId(mid);
                if(ClearDBDebugMode) {
                    tm.add(Calendar.HOUR, 1);
                } else {
                    tm.add(Calendar.YEAR, 3);
                    tm.add(Calendar.MONTH, -1);
                    tm.add(Calendar.DATE, -4);
                }
                if(tm.getTimeInMillis() < System.currentTimeMillis()) {
                    statement.close();
                    sql = "delete from " + tableName + " where id <= " + id;
                    statement = connection.prepareStatement(sql);
                    int count = statement.executeUpdate();
                    LOG.info("Clear {} rows", count);
                } else {
                    LOG.info("No history messages need clean");
                    hasMore = false;
                }
            } else {
                LOG.info("No messages");
            }
        } catch (Exception e) {
            hasMore = false;
            e.printStackTrace();
            Utility.printExecption(LOG, e, RDBS_Exception);
        } finally {
            DBUtil.closeDB(connection, statement, rs);
        }
        return hasMore;
    }

    private static List<String> getCreateSql() {
        List<String> out = new ArrayList<>();
        try{
            BufferedReader br = new BufferedReader(new FileReader("h2/create_table.sql"));//构造一个BufferedReader类来读取文件
            String s = null;
            StringBuilder result = new StringBuilder();
            while((s = br.readLine())!=null) {
                result.append(s);
                if (s.contains(";")) {
                    out.add(result.toString());
                    result = new StringBuilder();
                }
            }
            br.close();
        }catch(Exception e){
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
        return out;
    }
    //从数据源中获取数据库的连接
    public static Connection getConnection() throws SQLException {
        long threadId = Thread.currentThread().getId();

        if (map.get(threadId) != null) {
            LOG.error("error here!!!! DB connection not close correctly");
        }
        map.put(threadId, Thread.currentThread().getStackTrace().toString());
        Connection connection = transactionConnection.get();
        if (connection != null) {
            LOG.debug("Thread {} get db connection {}", threadId, connection);
            return connection;
        }

        connection = comboPooledDataSource.getConnection();
        LOG.debug("Thread {} get db connection {}", threadId, connection);
        return connection;
    }

    public static void beginTransaction() {
        try {
            Connection connection = getConnection();
            connection.setAutoCommit(false);
            transactionConnection.set(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }

    public static void commit() {
        try {
            Connection connection = transactionConnection.get();
            if (connection != null) {
                connection.commit();
                connection.setAutoCommit(true);
                transactionConnection.remove();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }

    public static void roolback() {
        try {
            Connection connection = transactionConnection.get();
            if (connection != null) {
                connection.rollback();
                connection.setAutoCommit(true);
                transactionConnection.remove();
            };
        } catch (SQLException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }

    //释放资源，将数据库连接还给数据库连接池
    public static void closeDB(Connection conn,PreparedStatement ps,ResultSet rs) {
        LOG.debug("Thread {} release db connection {}", Thread.currentThread().getId(), conn);
        try {
            if (rs!=null) {
                rs.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }

        try {
            if (ps!=null) {
                ps.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }

        try {
            if (conn!=null && transactionConnection.get() != conn) {
                conn.close();
                map.remove(Thread.currentThread().getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }
    //释放资源，将数据库连接还给数据库连接池
    public static void closeDB(Connection conn, PreparedStatement ps) {
        LOG.debug("Thread {} release db connection {}", Thread.currentThread().getId(), conn);
        try {
            if (ps!=null) {
                ps.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }

        try {
            if (conn!=null && transactionConnection.get() != conn) {
                conn.close();
                map.remove(Thread.currentThread().getId());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
        }
    }
}
