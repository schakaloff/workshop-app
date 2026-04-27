package Controllers.DbRepo;

import DB.DbConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VendorsQueries {

    public static List<String> getAllVendorNames() {
        List<String> vendors = new ArrayList<>();
        String sql = "SELECT name FROM vendors ORDER BY name ASC";

        try (Connection con = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                vendors.add(rs.getString("name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return vendors;
    }

    public static boolean insertVendor(String name) {
        String sql = "INSERT INTO vendors (name) VALUES (?)";

        try (Connection con = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name.trim());
            ps.executeUpdate();
            return true;

        } catch (SQLIntegrityConstraintViolationException e) {
            return false; // duplicate
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteVendor(String name) {
        String sql = "DELETE FROM vendors WHERE name = ?";

        try (Connection con = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}