package com.safefood.dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseUtil {
    private static final Properties props = new Properties();

    // 클래스가 로드될 때 config.properties 파일을 한 번만 읽어옵니다.
    static {
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("config.properties 파일을 읽는 데 실패했습니다.");
            e.printStackTrace();
        }
    }

    /**
     * Properties 파일의 정보를 바탕으로 DB 커넥션을 반환합니다.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                props.getProperty("db.url"),
                props.getProperty("db.user"),
                props.getProperty("db.password")
        );
    }
}