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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.hazelcast.util.StringUtil;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.moquette.BrokerConstants;
import io.moquette.server.config.IConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.Connection;
import java.util.concurrent.ConcurrentHashMap;

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

        public static void init(IConfig config) {
            String embedDB = config.getProperty(BrokerConstants.EMBED_DB_PROPERTY_NAME);
            if (embedDB != null && embedDB.equals("1")) {
                IsEmbedDB = true;
                LOG.info("Use h2 database");
            } else {
                IsEmbedDB = false;
                LOG.info("Use mysql database");
            }


            if (comboPooledDataSource == null) {
                if (IsEmbedDB) {
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
                    }

                    ResultSet rs = null;
                    Connection connection = null;
                    try {
                        connection = comboPooledDataSource.getConnection();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        Utility.printExecption(LOG, e);
                    }
                    Statement stmt = null;
                    try {
                        connection = DBUtil.getConnection();
                        rs  = connection.getMetaData().getTables(null, null,  "t_user_setting", null );
                        if (!rs.next()) {
                            List<String> creatsqls = getCreateSql();
                            for (String createsql : creatsqls) {
                                if (StringUtil.isNullOrEmpty(createsql)) {
                                    break;
                                }
                                stmt = connection.createStatement();
                                if (0 != stmt.executeLargeUpdate(createsql)) {
                                    LOG.error("创建表失败！ {}", createsql);
//                                    System.exit(-1);
                                }
                                try {
                                    stmt.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    Utility.printExecption(LOG, e);
                                }
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        Utility.printExecption(LOG, e);
                    } finally {
                        DBUtil.closeDB(connection, null, rs);
                    }
                } else {
                    comboPooledDataSource = new ComboPooledDataSource("mysql");
                }
            }
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
