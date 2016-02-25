package gmx.iderc.geoserver.tjs.data.jdbc.hsql;

import gmx.iderc.geoserver.tjs.data.TJSDataStore;
import gmx.iderc.geoserver.tjs.data.jdbc.JDBC_TJSDataStoreFactory;
import gmx.iderc.geoserver.tjs.data.xml.SQLToXSDMapper;
import net.opengis.tjs10.*;
import org.eclipse.emf.common.util.EList;


import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.digest.*;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 29/07/13
 * Time: 12:14
 * To change this template use File | Settings | File Templates.
 */
public class HSQLDB_GDAS_Cache {

    static Connection connection;
    static File allgdas;

    static String schemaName = "PUBLIC";

    static private boolean existCacheTables() throws SQLException {
        // Thijs Brentjens: should there be a schema in here or not?
        // now  without schemaName, just asume PUBLIC
        ResultSet tables = getConnection().getMetaData().getTables(null, null, "CACHE", new String[]{"TABLE"});

        boolean exist = tables.next();
        tables.close();
        return exist;
    }

    static PreparedStatement insertStatement = null;

    static private PreparedStatement getInsertPreparedStatement() throws SQLException {
        if (insertStatement == null){
            String insertSQL = "INSERT INTO CACHE(GDAS_URL, LOAD_DATE, TABLENAME) VALUES (?,?,?);";
            insertStatement = getConnection().prepareStatement(insertSQL);
        }
        return insertStatement;
    }

    static PreparedStatement updateStatement = null;

    static private PreparedStatement getUpdatePreparedStatement() throws SQLException {
        if (updateStatement == null){
            String updateSQL = "UPDATE CACHE SET LOAD_DATE = ? WHERE GDAS_URL=?;";
            updateStatement = getConnection().prepareStatement(updateSQL);
        }
        return updateStatement;
    }


    static public boolean append(String url, String tableName){
        try {
            getInsertPreparedStatement().setString(1, url);
            java.util.Date now = new java.util.Date();
            getInsertPreparedStatement().setTimestamp(2, new Timestamp(now.getTime()));
            getInsertPreparedStatement().setString(3, tableName);
            getInsertPreparedStatement().executeUpdate();
            getInsertPreparedStatement().closeOnCompletion();
            insertStatement=null;
            return true;
        } catch (SQLException e) {
            e.printStackTrace();  
        }
        return false;
    }

    static public boolean update(String url, String tableName){
        try {
            java.util.Date now = new java.util.Date();
            getUpdatePreparedStatement().setTimestamp(1, new Timestamp(now.getTime()));
            getUpdatePreparedStatement().setString(2, url);
            getUpdatePreparedStatement().executeUpdate();
            getUpdatePreparedStatement().closeOnCompletion();
            updateStatement = null;
            return true;
        } catch (SQLException e) {
            e.printStackTrace();  
        }
        return false;
    }

    static public String existGDAS(String url){
        try {
            // Statement statement = getConnection().createStatement();
            PreparedStatement statement = getConnection().prepareStatement("SELECT TABLENAME FROM CACHE WHERE GDAS_URL=?;") ;
            // ResultSet resultSet = statement.executeQuery("SELECT TABLENAME FROM CACHE WHERE GDAS_URL='" + url + "';");
            statement.setString(1,url);
            ResultSet resultSet = statement.executeQuery();
            boolean exists = resultSet.next();
            String tableName = null;
            if (exists) {
                tableName = resultSet.getString(1);
            }
            resultSet.close();
            statement.closeOnCompletion();
            return tableName;
        } catch (SQLException e) {
            e.printStackTrace();

        }
        return null;
    }

    static public boolean clearGDAS(String tableName){
        try {
            Statement statement = getConnection().createStatement();
            if (!tableName.contains(".")) {
                // we can add a schemaName
                // TODO: use a schema name?
                // tableName = schemaName+"."+tableName;
            }
            statement.executeUpdate("DELETE FROM " + tableName + ";");
            statement.closeOnCompletion();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();  
        }
        return false;
    }

    static private void initCacheIndex(){
        StringBuilder sqlBuilder = new StringBuilder();
        //Crea la tabla donde almacenar el listado de GDAS cargados
        // sqlBuilder.append("CREATE TABLE "+schemaName+".CACHE(");
        sqlBuilder.append("CREATE TABLE CACHE(");
        sqlBuilder.append("id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,");
        sqlBuilder.append("GDAS_URL VARCHAR (2048),");
        sqlBuilder.append("LOAD_DATE TIMESTAMP,");
        sqlBuilder.append("TABLENAME VARCHAR (255));");
        //Crea un índice que permite buscar rápido si existe un GDAS por la URL
        String sql = sqlBuilder.toString();
        try {
            Connection connection = getConnection();
            Statement statement = connection.createStatement();
            statement.executeUpdate(sql);
            String sqlIndex =  "CREATE INDEX GDAS_URL_IDX ON CACHE(GDAS_URL);";
            statement.executeUpdate(sqlIndex);
            statement.closeOnCompletion();
        } catch (SQLException e) {
            e.printStackTrace();  
        }

    }

    // THijs: do we need a function close, where preparedstatements are closed?
    static public void closeConnections() {
       if (insertStatement!=null) {
          try {
            insertStatement.close();
          } catch (SQLException ex) {
              ex.printStackTrace();
          }
       }
        if (updateStatement!=null) {
            try {
                updateStatement.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        if (connection!=null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    static public Connection getConnection() {
        if (connection != null) {
            return connection;
        }
        try {

            Class.forName("org.hsqldb.jdbcDriver");
            String tmpDir = System.getProperty("java.io.tmpdir");
            // TODO: Thijs: or better save the database in the datadir?
            // tjs.getGeoserver()
            
            // Thijs: fix the dir by using proper separator
            tmpDir = tmpDir.concat(File.separator).concat("allgdas").concat(File.separator);
            allgdas = new File(tmpDir);
            String url = "jdbc:hsqldb:file:/" + allgdas.toString();
            // String url = "jdbc:hsqldb:mem:allgdas";

            url = url.replace(File.separatorChar, '/');
            connection = DriverManager.getConnection(url, "SA", "");
            connection.setAutoCommit(true);
            if (!existCacheTables()){
                initCacheIndex();
            }
        } catch (SQLException e) {
            e.printStackTrace();  
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  
        }
        return connection;
    }

    static public String getSafeTableName(String tableName){
        tableName = tableName.toUpperCase();
        tableName = tableName.replaceAll("[^A-Z0-9_]", "");
        // tableName = tableName.replace("-","_");
        // tableName = tableName.replace(".","_");
        // Thijs: shorten the tablename if it is longer than 17 characters. This number is choosen because of the time in millisecons that is added.
        // TODO: determine how to deal with the length of the tablename and if the timestamp in millisecs is needed
        // replace the timestamp by the id value of the database? this is shorter...
        if (tableName.length() >= 32) {
            tableName = tableName.substring(0,32);
        }
        return tableName;
    }

    public static String importGDAS(GDASType gdasType, String url) {
        String tableName = existGDAS(url);
        boolean newGdas = tableName == null;
        if (newGdas){
            // Thijs: should there be a schema "PUBLIC." in front here or not?
            // Let's try without it
            // tableName = schemaName+gdasType.getFramework().getDataset().getTitle();
            // TODO: Thijs: document how the filename is determined: use the Title of the GDAS-file + part of the md5 hash of the URL
            tableName = gdasType.getFramework().getDataset().getTitle();
            // replace "-" by "_" in the table_name
            // make the table name no longer than X chars
            tableName = getSafeTableName(tableName);
            // Should we include a timestamp or not? I think we shouldn't..
            // tableName = tableName.concat(String.valueOf(System.currentTimeMillis()));
            tableName = tableName.toUpperCase();
            // md5 is too long probably
            // get the first 6 chars of md5 -> very small chance (1 in 1 million?) that the same layer is found in combination with the tablename
            // String urlHash = DigestUtils.md5Hex( url );
            // tableName.substring(0,31);
            // max 24 chars of the tablename+
            tableName = tableName.substring(0, Math.min(tableName.length(), 25)) + "_";
            String t = String.valueOf(System.currentTimeMillis());
            // t = DigestUtils.md5Hex( url );
            tableName = tableName.concat(t.substring(t.length()-6) );
        }else {
            tableName = getSafeTableName(tableName);
            clearGDAS(tableName);
        }
        GDAS_Importer_Thread thread = new GDAS_Importer_Thread(gdasType,  tableName, newGdas);
        thread.run();
        // TODO: Thijs: dispose the thread at some point?
        while (!thread.alldone){
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();  
            }
        }

        if (newGdas){
            append(url, tableName);
        }else{
            update(url, tableName);
        }
        try {
            getConnection().close();
        }  catch (SQLException e) {
            // TODO: throw this exception?
            e.printStackTrace() ;
        }   finally {
           connection = null;
        }

        return tableName;
    }

    static TJSDataStore dataStore = null;

    public static HSQLDB_TJSDataStore getCacheDataStore(){
        if (dataStore == null){
            HSQLDB_TJSDataStoreFactory factory = new HSQLDB_TJSDataStoreFactory();
            HashMap<String, String> params = new HashMap<String, String>();
            params.put(JDBC_TJSDataStoreFactory.DATABASE.key, allgdas.toString());
            params.put(JDBC_TJSDataStoreFactory.USER.key, "SA");
            try {
                dataStore = factory.createDataStore(params);
            } catch (IOException e) {
                e.printStackTrace();  
            }
        }
        return (HSQLDB_TJSDataStore) dataStore;
    }


}
