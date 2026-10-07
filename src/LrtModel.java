package main;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * PRESENTATION STUDY NOTES - MODEL LAYER (LrtModel.java):
 * ----------------------------------------------------------------------------------
 * 1. Purpose: Handles data access, database connection (JDBC), batch updates,
 *    and MySQL analytical queries. Contains zero GUI code (MVC Separation).
 * 2. ERD:
 *    - stations: Primary metadata table storing Station ID, Name, and Max Capacity.
 *    - tap_logs: Transaction table recording taps linked via Foreign Key.
 * 3. Logic: Calculates station congestion % over a 10-second rolling window.
 */
public class LrtModel {
    // JDBC Configuration Parameters matching MySQL Workbench settings
    private static final String DB_URL = "jdbc:mysql://localhost:3306/lrt1_db?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "DoggieBoo18!"; // Password configured in MySQL Workbench
    private final Random random = new Random();

    // Baseline capacities for all 25 LRT-1 stations (Used for ERD initialization)
    private static final Object[][] STATIONS_DATA = {
        {"Fernando Poe Jr.", 150}, {"Balintawak", 200}, {"Roosevelt", 200}, 
        {"Monumento", 250}, {"5th Avenue", 150}, {"R. Papa", 100}, 
        {"Abad Santos", 150}, {"Blumentritt", 200}, {"Tayuman", 150}, 
        {"Bambang", 100}, {"Doroteo Jose", 300}, {"Carriedo", 200}, 
        {"Central Terminal", 250}, {"United Nations", 200}, {"Pedro Gil", 200}, 
        {"Quirino", 150}, {"Vito Cruz", 200}, {"Gil Puyat", 250}, 
        {"Libertad", 150}, {"EDSA", 350}, {"Baclaran", 300}, 
        {"Redemptorist", 150}, {"MIA", 150}, {"Asia World", 150}, {"Dr. Santos", 150}
    };

    public LrtModel() {
        initDatabase();
    }

    /**
     * DATABASE:
     * - "Where are your tables created in Java?"
     * - in initDatabase(). Uses DDL statements (CREATE TABLE IF NOT EXISTS)
     *   to construct 'stations' and 'tap_logs' tables if they don't exist yet.
     */
    private void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {

            // DDL: Create STATIONS table (Entity 1 in ERD)
            stmt.execute("CREATE TABLE IF NOT EXISTS stations (" +
                    "station_id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "station_name VARCHAR(100) NOT NULL, " +
                    "max_capacity INT NOT NULL)");

            // DDL: Create TAP_LOGS table (Entity 2 in ERD with Foreign Key relationship)
            stmt.execute("CREATE TABLE IF NOT EXISTS tap_logs (" +
                    "tap_id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "station_id INT NOT NULL, " +
                    "tap_type VARCHAR(5) NOT NULL, " +
                    "direction VARCHAR(5) NOT NULL, " +
                    "timestamp DATETIME NOT NULL, " +
                    "FOREIGN KEY (station_id) REFERENCES stations(station_id) ON DELETE CASCADE)");

            // Populate stations on first startup if empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM stations");
            if (rs.next() && rs.getInt(1) == 0) {
                PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO stations (station_name, max_capacity) VALUES (?, ?)");
                for (Object[] data : STATIONS_DATA) {
                    pstmt.setString(1, (String) data[0]);
                    pstmt.setInt(2, (Integer) data[1]);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Database Error! Verify MySQL Server is running and DB_PASS is correct.");
            e.printStackTrace();
        }
    }

    /**
     * TAPS:
     * - "How do you simulate passenger influx?"
     * - Generates batch SQL inserts into 'tap_logs' based on operational conditions
     *   (Rush hour = 35..60 taps/sec, Rainy season = +25% volume multiplier).
     * - Hotspots: 60% of rush hour taps focus on Monumento, Doroteo Jose, and EDSA.
     */
    public int generateMockTaps(boolean isOpen, boolean isRushHour, boolean isRainySeason, boolean isHoliday) {
        if (!isOpen) return 0;

        int numTaps;
        if (isRushHour) {
            numTaps = random.nextInt(26) + 35; // Peak rush hour range
        } else if (isHoliday) {
            numTaps = random.nextInt(4) + 1;   // Low volume holiday range
        } else {
            numTaps = random.nextInt(8) + 2;   // Regular off-peak range
        }

        // Apply 25% rainy season commuter surge multiplier
        if (isRainySeason && isOpen) {
            numTaps = (int) (numTaps * 1.25);
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "INSERT INTO tap_logs (station_id, tap_type, direction, timestamp) VALUES (?, ?, ?, NOW())";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < numTaps; i++) {
                    int stationId;
                    if (isRushHour) {
                        double roll = random.nextDouble();
                        if (roll < 0.60) {
                            // Heavy Hotspots (60% weight): Station 4 (Monumento), 11 (Doroteo Jose), 20 (EDSA)
                            int[] heavyHotspots = {4, 11, 20};
                            stationId = heavyHotspots[random.nextInt(heavyHotspots.length)];
                        } else if (roll < 0.85) {
                            // Secondary Hotspots (25% weight)
                            int[] moderateHotspots = {2, 8, 13, 17, 21}; 
                            stationId = moderateHotspots[random.nextInt(moderateHotspots.length)];
                        } else {
                            // Remaining 15% distributed among quiet stations
                            stationId = random.nextInt(STATIONS_DATA.length) + 1;
                        }
                    } else {
                        stationId = random.nextInt(STATIONS_DATA.length) + 1;
                    }

                    pstmt.setInt(1, stationId);
                    pstmt.setString(2, random.nextDouble() < 0.85 ? "IN" : "OUT");
                    pstmt.setString(3, random.nextBoolean() ? "NB" : "SB");
                    pstmt.addBatch(); // Batch insert optimization
                }
                pstmt.executeBatch(); // Send batch to MySQL server
            }
        } catch (SQLException e) {
            System.err.println("Database Error during Mock Tap Generation: " + e.getMessage());
        }
        return numTaps;
    }

    /**
     * CONGESTION:
     * - "How is station congestion rate calculated?"
     * - Executes a 10-second rolling window SQL query with CASE WHEN conditional counts.
     *   Congestion Rate % = ((NB Taps + SB Taps) / Max Capacity) * 100.
     *   Status thresholds: >=80% = CONGESTED, >=50% = MODERATE, <50% = NORMAL.
     */
    public List<Object[]> getCongestionData(boolean isOpen) {
        List<Object[]> rowsData = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // Clean up logs older than 10 seconds to maintain sliding window
            try (Statement cleanupStmt = conn.createStatement()) {
                cleanupStmt.executeUpdate("DELETE FROM tap_logs WHERE timestamp < DATE_SUB(NOW(), INTERVAL 10 SECOND)");
            } catch (SQLException ignored) {}

            // Relational JOIN and Aggregation Query
            String query = "SELECT s.station_id, s.station_name, s.max_capacity, " +
                    "COUNT(CASE WHEN t.direction = 'NB' THEN 1 END) AS nb_taps, " +
                    "COUNT(CASE WHEN t.direction = 'SB' THEN 1 END) AS sb_taps, " +
                    "COUNT(t.tap_id) AS total_in_taps " +
                    "FROM stations s " +
                    "LEFT JOIN tap_logs t ON s.station_id = t.station_id " +
                    "AND t.tap_type = 'IN' " +
                    "AND t.timestamp >= DATE_SUB(NOW(), INTERVAL 10 SECOND) " +
                    "GROUP BY s.station_id, s.station_name, s.max_capacity " +
                    "ORDER BY s.station_id";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    int id = rs.getInt("station_id");
                    String name = id + ". " + rs.getString("station_name");
                    int maxCap = rs.getInt("max_capacity");
                    int rawNb = rs.getInt("nb_taps");
                    int rawSb = rs.getInt("sb_taps");

                    if (!isOpen) {
                        rowsData.add(new Object[]{id, name, maxCap, 0, 0, "0.0%", "CLOSED"});
                        continue;
                    }

                    // Scale 10-second real sample to 60-second operational window equivalent
                    int nbTaps = rawNb * 3;
                    int sbTaps = rawSb * 3;
                    int totalIn = nbTaps + sbTaps;

                    // Formula: (Total Platform Inflow / Station Capacity) * 100
                    double rate = ((double) totalIn / maxCap) * 100;
                    String status = "NORMAL";
                    if (rate >= 80.0) status = "CONGESTED";
                    else if (rate >= 50.0) status = "MODERATE";

                    // Directional Flow Imbalance Logic
                    if (totalIn > 5) {
                        if (nbTaps > sbTaps * 1.5) status += " [Heavy NB]";
                        else if (sbTaps > nbTaps * 1.5) status += " [Heavy SB]";
                        else status += " [Balanced]";
                    }

                    rowsData.add(new Object[]{id, name, maxCap, nbTaps, sbTaps, String.format("%.1f%%", rate), status});
                }
            }
        } catch (Exception e) {
            System.err.println("Query Exception in getCongestionData: " + e.getMessage());
        }

        // Guarantees UI rows populate even if DB returns 0 rows
        if (rowsData.isEmpty()) {
            for (int i = 0; i < STATIONS_DATA.length; i++) {
                int id = i + 1;
                String name = id + ". " + STATIONS_DATA[i][0];
                int maxCap = (Integer) STATIONS_DATA[i][1];
                String status = isOpen ? "NORMAL" : "CLOSED";
                rowsData.add(new Object[]{id, name, maxCap, 0, 0, "0.0%", status});
            }
        }

        return rowsData;
    }
}