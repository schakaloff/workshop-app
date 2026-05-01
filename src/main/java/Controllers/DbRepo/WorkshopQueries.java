package Controllers.DbRepo;

import DB.DbConfig;
import Skeletons.Customer;
import Skeletons.TechWorkRow;
import Skeletons.WorkOrder;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WorkshopQueries {

    public List<TechWorkRow> loadTechWorkByDateRange(String techUsername, LocalDate fromDate, LocalDate toDate) {
        List<TechWorkRow> list = new ArrayList<>();

        String sql = "SELECT w.workorder, w.type, w.status, SUM(r.price) AS labour_total, " +
                "DATE_FORMAT(COALESCE(w.finished_at, MAX(TIMESTAMP(r.repair_date, '00:00:00'))), '%Y-%m-%d %H:%i') AS finished_date " +
                "FROM work_order_repairs r " +
                "JOIN work_order w ON w.workorder = r.workorder_id " +
                "WHERE r.tech = ? " +
                "AND w.status IN ('Repair Complete', 'Billing Complete') " +
                "AND r.repair_date BETWEEN ? AND ? " +
                "GROUP BY w.workorder, w.type, w.status, w.finished_at " +
                "ORDER BY COALESCE(w.finished_at, MAX(TIMESTAMP(r.repair_date, '00:00:00'))) DESC";

        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, techUsername);
            stmt.setDate(2, Date.valueOf(fromDate));
            stmt.setDate(3, Date.valueOf(toDate));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new TechWorkRow(
                        rs.getInt("workorder"),
                        rs.getString("type"),
                        rs.getString("status"),
                        rs.getDouble("labour_total"),
                        rs.getString("finished_date")
                ));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public Customer getCustomerById(int customerId) {
        String sql = "SELECT * FROM customer WHERE id = ?";
        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Customer c = new Customer(
                        rs.getString("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        "",
                        rs.getString("phone"),
                        "",
                        rs.getString("address"),
                        rs.getString("postal_code"),
                        rs.getString("town")
                );
                rs.close();
                stmt.close();
                conn.close();

                return c;
            }
            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<WorkOrder> loadOrdersIntoTable() {
        List<WorkOrder> list = new ArrayList<>();

        String sql = "SELECT wo.workorder, wo.status, wo.type, " +
                "DATE_FORMAT(wo.createdAt, '%Y-%m-%d %H:%i') AS createdAt, " +
                "wo.vendorId, wo.warrantyNumber, wo.model, wo.serialNumber, " +
                "wo.problemDesc, wo.customer_id, wo.deposit_amount, wo.tech_id, " +
                "COALESCE(c.first_name, '') AS first_name, " +
                "COALESCE(c.last_name, '')  AS last_name " +
                "FROM work_order wo " +
                "LEFT JOIN customer c ON wo.customer_id = c.id " +
                "ORDER BY wo.createdAt DESC";

        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                WorkOrder wo = new WorkOrder(
                        rs.getInt("workorder"),
                        rs.getString("status"),
                        rs.getString("type"),
                        rs.getString("createdAt"),
                        rs.getString("vendorId"),
                        rs.getString("warrantyNumber"),
                        rs.getString("model"),
                        rs.getString("serialNumber"),
                        rs.getString("problemDesc"),
                        rs.getInt("customer_id"),
                        rs.getDouble("deposit_amount")
                );

                int techId = rs.getInt("tech_id");
                if (rs.wasNull()) techId = 0;
                wo.setTechId(techId);

                wo.setCustomerName(rs.getString("first_name") + " " + rs.getString("last_name"));

                list.add(wo);
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public int insertOrderIntoDatabase(String status, String type, String model, String serialNumber, String problemDesc, int customerId, String vendorId, String warrantyNumber, double deposit) {
        String sql = "INSERT INTO work_order (status, type, model, serialNumber, problemDesc, customer_id, vendorId, warrantyNumber, deposit_amount, createdAt) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, status);
            stmt.setString(2, type);
            stmt.setString(3, model);
            stmt.setString(4, serialNumber);
            stmt.setString(5, problemDesc);
            stmt.setInt(6, customerId);
            stmt.setString(7, vendorId);
            stmt.setString(8, warrantyNumber);
            stmt.setDouble(9, deposit);

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);

                rs.close();
                stmt.close();
                conn.close();

                return id;
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }
    public List<Integer> getCustomerIdsByPhone(String phone) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM customer WHERE phone LIKE ?";
        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + phone + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("id"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }
    public List<Integer> getCustomerIdsByField(String column, String value) {
        List<Integer> ids = new ArrayList<>();
        // column is hardcoded from our own switch, safe to interpolate
        String sql = "SELECT id FROM customer WHERE " + column + " LIKE ?";
        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + value + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("id"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    public List<Integer> getCustomerIdsByFullName(String firstName, String lastName) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM customer WHERE first_name LIKE ? AND last_name LIKE ?";
        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + firstName + "%");
            ps.setString(2, "%" + lastName + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("id"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    public WorkOrder getWorkOrderById(int woNumber) {
        String sql = "SELECT wo.workorder, wo.status, wo.type, " +
                "DATE_FORMAT(wo.createdAt, '%Y-%m-%d %H:%i') AS createdAt, " +
                "wo.vendorId, wo.warrantyNumber, wo.model, wo.serialNumber, " +
                "wo.problemDesc, wo.customer_id, wo.deposit_amount, wo.tech_id, " +
                "COALESCE(c.first_name, '') AS first_name, " +
                "COALESCE(c.last_name, '')  AS last_name " +
                "FROM work_order wo " +
                "LEFT JOIN customer c ON wo.customer_id = c.id " +
                "WHERE wo.workorder = ?";

        try {
            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, woNumber);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                WorkOrder wo = new WorkOrder(
                        rs.getInt("workorder"),
                        rs.getString("status"),
                        rs.getString("type"),
                        rs.getString("createdAt"),
                        rs.getString("vendorId"),
                        rs.getString("warrantyNumber"),
                        rs.getString("model"),
                        rs.getString("serialNumber"),
                        rs.getString("problemDesc"),
                        rs.getInt("customer_id"),
                        rs.getDouble("deposit_amount")
                );
                int techId = rs.getInt("tech_id");
                if (rs.wasNull()) techId = 0;
                wo.setTechId(techId);
                wo.setCustomerName(rs.getString("first_name") + " " + rs.getString("last_name"));

                rs.close(); stmt.close(); conn.close();
                return wo;
            }

            rs.close(); stmt.close(); conn.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}