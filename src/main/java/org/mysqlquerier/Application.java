package org.mysqlquerier;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class Application {

    private static final String property_db_url = "DB_URL";
    private static final String property_db_user = "DB_USER";
    private static final String property_db_password = "DB_PASSWORD";
    private static final String property_app_number_calls = "APP_NUMBER_CALLS";
    private static final String property_app_millisecs_between_calls = "APP_MILLISECS_BETWEEN_CALLS";

    private static final String configFilePath = "config.properties";
    private static final String queryFilePath = "query.sql";

    private static final String dateFormat = "hh:mm:ss yyyy-MM-dd";
    private static final String jdbcProtocol = "jdbc:mysql://";
    private static final String infoTookString = "Make and proccess request took: %s ms";
    private static final String errorMandatoryProperty = "ERROR - Mandatory property -> %s <- not set. Check config file.";
    private static final String errorMandatoryProperties = "Not all mandatory parameters set. Check error messages.";
    private static final String executionDateColumn = "execution date";
    private static final String columnSeparator = ",\t";
    private static final String emptyString = "";
    private static final String spaceString = " ";

    private static long timeToSleepInMilisecBetweenCalls;
    private static long numberOfCalls;
    private static String url;
    private static String user;
    private static String password;
    private static boolean firstTime = true;

    public static void main(String[] args) {
        try {
            initializeProperties();
            for (int i = 0; i < numberOfCalls; i++) {
                executeAndPrint();
                if (i != numberOfCalls -1)
                    Thread.sleep(timeToSleepInMilisecBetweenCalls);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initializeProperties() throws IOException {
        Properties properties = getProperties();
        validateProperties(properties);
        url = jdbcProtocol + properties.get(property_db_url).toString();
        user = properties.get(property_db_user).toString();
        password = properties.get(property_db_password).toString();
        numberOfCalls = Long.parseLong(properties.get(property_app_number_calls).toString());
        timeToSleepInMilisecBetweenCalls = Long.parseLong(properties.get(property_app_millisecs_between_calls).toString());
    }

    private static void validateProperties(Properties properties) {
        List<String> propertyList = getPropertiesList();
        boolean allPropertiesSet = true;
        for (String property : propertyList)
            if (properties.get(property) == null || properties.get(property).toString().isEmpty()) {
                allPropertiesSet = false;
                log(errorMandatoryProperty, property);
            }
        if (!allPropertiesSet)
            throw new RuntimeException(errorMandatoryProperties);
    }

    private static List<String> getPropertiesList() {
        List<String> propertyList = new ArrayList<>();
        propertyList.add(property_db_url);
        propertyList.add(property_db_user);
        propertyList.add(property_db_password);
        propertyList.add(property_app_number_calls);
        propertyList.add(property_app_millisecs_between_calls);
        return propertyList;
    }

    private static void executeAndPrint() throws SQLException, IOException, InterruptedException {
        long ms = System.currentTimeMillis();
        ResultSet rs = makeSQLRequest();
        if (firstTime)
            printColumnNames(rs);
        printResult(rs);
        logln(infoTookString, (System.currentTimeMillis() - ms));
    }

    private static ResultSet makeSQLRequest() throws SQLException, IOException {
        Connection conn = DriverManager.getConnection(url, user, password);
        Statement stmt = conn.createStatement();
        String query = buildQuery();
        return stmt.executeQuery(query);
    }

    private static String buildQuery() throws IOException {
        String query = emptyString;
        List<String> queryLines = getAllLines(queryFilePath);
        for (String queryLine : queryLines)
            query += spaceString + queryLine;
        return query;
    }

    private static void printColumnNames(ResultSet rs) throws SQLException {
        firstTime = false;
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnsNumber = rsmd.getColumnCount();
        log(executionDateColumn);
        for (int i = 1; i <= columnsNumber; i++)
            log(columnSeparator + rsmd.getColumnName(i));
        logln(emptyString);
    }

    private static void printResult(ResultSet rs) throws SQLException {
        int columnsNumber = rs.getMetaData().getColumnCount();
        Date execDate = new Date();
        while (rs.next()) {
            printDate(execDate);
            for (int i = 1; i <= columnsNumber; i++)
                log(columnSeparator + rs.getString(i));
            logln(emptyString);
        }
    }

    private static List<String> getAllLines(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    private static Properties getProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(configFilePath));
        return properties;
    }

    private static void printDate(Date date) {
        SimpleDateFormat dt = new SimpleDateFormat(dateFormat);
        log(dt.format(date));
    }

    private static void logln(String s) {
        System.out.println(s);
    }

    private static void log(String string) {
        System.out.print(string);
    }

    private static void log(String format, Object ... args) {
        System.out.format(format, args);
    }

    private static void logln(String format, Object ... args) {
        System.out.format(format, args);
        logln(emptyString);
    }
}