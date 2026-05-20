package Controllers.DbRepo;

import DB.DbConfig;
import Skeletons.Vendor;

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
    public static List<Vendor> getAllVendors() {
        List<Vendor> vendors = new ArrayList<>();
        String sql = "SELECT id, name, pays_labour, pays_parts, pays_pst, pays_gst, " +
                "address, city, province, postal, contact, phone FROM vendors ORDER BY name ASC";
        try (Connection con = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                vendors.add(new Vendor(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getBoolean("pays_labour"),
                        rs.getBoolean("pays_parts"),
                        rs.getBoolean("pays_pst"),
                        rs.getBoolean("pays_gst"),
                        rs.getString("address"),
                        rs.getString("city"),
                        rs.getString("province"),
                        rs.getString("postal"),
                        rs.getString("contact"),
                        rs.getString("phone")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return vendors;
    }

    public static void updateVendorFull(int id, String name, String address, String city,
                                        String province, String postal,
                                        String contact, String phone) {
        String sql = "UPDATE vendors SET name=?, address=?, city=?, province=?, postal=?, contact=?, phone=? WHERE id=?";
        try (Connection con = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, address);
            ps.setString(3, city);
            ps.setString(4, province);
            ps.setString(5, postal);
            ps.setString(6, contact);
            ps.setString(7, phone);
            ps.setInt(8, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static boolean insertVendorFull(String name, String address, String city,
                                           String province, String postal,
                                           String contact, String phone) {
        String sql = "INSERT INTO vendors (name, address, city, province, postal, contact, phone) VALUES (?,?,?,?,?,?,?)";
        try (Connection con = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, address);
            ps.setString(3, city);
            ps.setString(4, province);
            ps.setString(5, postal);
            ps.setString(6, contact);
            ps.setString(7, phone);
            ps.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static void updateVendor(int id, boolean paysLabour, boolean paysParts,
                                    boolean paysPst, boolean paysGst) {
        String sql = "UPDATE vendors SET pays_labour=?, pays_parts=?, pays_pst=?, pays_gst=? WHERE id=?";
        try (Connection con = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, paysLabour);
            ps.setBoolean(2, paysParts);
            ps.setBoolean(3, paysPst);
            ps.setBoolean(4, paysGst);
            ps.setInt(5, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static Vendor getVendorByName(String name) {
        if (name == null || name.isBlank()) return null;
        String sql = "SELECT id, name, pays_labour, pays_parts, pays_pst, pays_gst, " +
                "address, city, province, postal, contact, phone FROM vendors WHERE name = ?";
        try (Connection con = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Vendor(
                        rs.getInt("id"), rs.getString("name"),
                        rs.getBoolean("pays_labour"), rs.getBoolean("pays_parts"),
                        rs.getBoolean("pays_pst"), rs.getBoolean("pays_gst"),
                        rs.getString("address"), rs.getString("city"),
                        rs.getString("province"), rs.getString("postal"),
                        rs.getString("contact"), rs.getString("phone")
                );
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
}