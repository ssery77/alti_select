import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AltibaseTest {

    static class DbConfig {
        String name;
        String ip;
        String port;
        String user;
        String password;

        DbConfig(String name, String ip, String port, String user, String password) {
            this.name = name;
            this.ip = ip;
            this.port = port;
            this.user = user;
            this.password = password;
        }
    }

    public static void main(String[] args) {
        // Altibase JDBC Driver Class
        String driver = "Altibase5.jdbc.driver.AltibaseDriver"; // Altibase5.jar

        // 1. dblist.cnf 읽기
        List<DbConfig> dbConfigList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("dblist.cnf"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                String[] parts = line.split("\\s+", 5);
                if (parts.length < 5) {
                    System.err.println("Invalid dblist.cnf line (expected: name ip port user password): " + line);
                    continue;
                }
                dbConfigList.add(new DbConfig(parts[0], parts[1], parts[2], parts[3], parts[4]));
            }
        } catch (IOException e) {
            System.err.println("Failed to read dblist.cnf: " + e.getMessage());
            return;
        }

        // 2. query.sql 읽기 (UTF-8 명시)
        String query;
        try {
            query = new String(Files.readAllBytes(Paths.get("query.sql")), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            System.err.println("Failed to read query.sql: " + e.getMessage());
            return;
        }

        // 3. JDBC 드라이버 로드
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            System.err.println("Altibase JDBC Driver not found. Please check if Altibase5.jar is in the classpath.");
            e.printStackTrace();
            return;
        }

        // 4. 순차적으로 각 DB에 접속하여 질의 실행
        for (DbConfig db : dbConfigList) {
            String url = "jdbc:Altibase://" + db.ip + ":" + db.port + "/";

            // Try-with-resources를 사용하여 자동 자원 해제
            try (Connection conn = DriverManager.getConnection(url, db.user, db.password);
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    String resultValue = rs.getString(1);
                    String testValue = "OK".equals(resultValue) ? "TestOK" : "TestFail";
                    // System.out.println을 사용하여 포맷 관련 오류 방지
                    System.out.println(db.name + "|" + resultValue + "|" + testValue);
                }

            } catch (SQLException e) {
                System.err.println("[" + db.name + "] Connection or Query failed: " + e.getMessage());
            }
        }
    }
}