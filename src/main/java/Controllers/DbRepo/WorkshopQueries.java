package Controllers.DbRepo;

import DB.DataSourceProvider;
import Skeletons.Customer;
import Skeletons.TechWorkRow;
import Skeletons.WorkOrder;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WorkshopQueries {

    // ─── TECH WORK ───────────────────────────────────────────────────────────────

    public List<TechWorkRow> loadTechWorkByDateRange(String techUsername, LocalDate fromDate, LocalDate toDate) {
        List<TechWorkRow> list = new ArrayList<>();

        String sql = "SELECT w.workorder, w.model, w.status, SUM(r.price) AS labour_total, " +
                "DATE_FORMAT(COALESCE(w.finished_at, MAX(TIMESTAMP(r.repair_date, '00:00:00'))), '%Y-%m-%d %H:%i') AS finished_date " +
                "FROM work_order_repairs r " +
                "JOIN work_order w ON w.workorder = r.workorder_id " +
                "WHERE r.tech = ? " +
                "AND w.status IN ('Repair Complete', 'Billing Complete') " +
                "AND r.repair_date BETWEEN ? AND ? " +
                "GROUP BY w.workorder, w.model, w.status, w.finished_at " +
                "ORDER BY COALESCE(w.finished_at, MAX(TIMESTAMP(r.repair_date, '00:00:00'))) DESC";

        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, techUsername);
            stmt.setDate(2, Date.valueOf(fromDate));
            stmt.setDate(3, Date.valueOf(toDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new TechWorkRow(
                            rs.getInt("workorder"),
                            rs.getString("model"),
                            rs.getString("status"),
                            rs.getDouble("labour_total"),
                            rs.getString("finished_date")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // ─── CUSTOMER ────────────────────────────────────────────────────────────────

    public Customer getCustomerById(int customerId) {
        String sql = "SELECT * FROM customer WHERE id = ?";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Customer(
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
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ─── LOAD ALL ORDERS ─────────────────────────────────────────────────────────

    public List<WorkOrder> loadOrdersIntoTable() {
        List<WorkOrder> list = new ArrayList<>();

        String sql = "SELECT wo.workorder, wo.status, wo.type, " +
                "DATE_FORMAT(wo.createdAt, '%Y-%m-%d %H:%i') AS createdAt, " +
                "wo.vendorId, wo.warrantyNumber, wo.model, wo.serialNumber, " +
                "wo.problemDesc, wo.customer_id, wo.deposit_amount, wo.tech_id, " +
                "COALESCE(c.first_name, '') AS first_name, " +
                "COALESCE(c.last_name,  '') AS last_name, " +
                "COALESCE(t.username,   '') AS tech_username " +
                "FROM work_order wo " +
                "LEFT JOIN customer    c ON wo.customer_id = c.id " +
                "LEFT JOIN technician  t ON wo.tech_id     = t.id " +
                "ORDER BY wo.createdAt DESC " +
                "LIMIT 75";

        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

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
                wo.setTechUsername(rs.getString("tech_username"));
                wo.setCustomerName(rs.getString("first_name") + " " + rs.getString("last_name"));

                list.add(wo);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // ─── DASHBOARD FILTERS (query full table, not just the 75-row cache) ─────────

    private static final String FILTER_SELECT_BASE =
            "SELECT wo.workorder, wo.status, wo.type, " +
            "DATE_FORMAT(wo.createdAt, '%Y-%m-%d %H:%i') AS createdAt, " +
            "wo.vendorId, wo.warrantyNumber, wo.model, wo.serialNumber, " +
            "wo.problemDesc, wo.customer_id, wo.deposit_amount, wo.tech_id, " +
            "COALESCE(c.first_name, '') AS first_name, " +
            "COALESCE(c.last_name,  '') AS last_name, " +
            "COALESCE(t.username,   '') AS tech_username " +
            "FROM work_order wo " +
            "LEFT JOIN customer    c ON wo.customer_id = c.id " +
            "LEFT JOIN technician  t ON wo.tech_id     = t.id ";

    private List<WorkOrder> queryFilteredOrders(String whereClause, Object... params) {
        List<WorkOrder> list = new ArrayList<>();
        String sql = FILTER_SELECT_BASE + whereClause + " ORDER BY wo.createdAt DESC";

        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);

            try (ResultSet rs = stmt.executeQuery()) {
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
                    wo.setTechUsername(rs.getString("tech_username"));
                    wo.setCustomerName(rs.getString("first_name") + " " + rs.getString("last_name"));

                    list.add(wo);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    private int queryFilteredCount(String whereClause, Object... params) {
        String sql = "SELECT COUNT(*) FROM work_order wo " + whereClause;
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<WorkOrder> getAllOpenWO() {
        return queryFilteredOrders("WHERE wo.status NOT IN ('Repair Complete', 'Billing Complete', 'Cancelled')");
    }

    public int countAllOpenWO() {
        return queryFilteredCount("WHERE wo.status NOT IN ('Repair Complete', 'Billing Complete', 'Cancelled')");
    }

    public List<WorkOrder> getOldNewOver10() {
        return queryFilteredOrders("WHERE wo.status = 'New' AND wo.createdAt < NOW() - INTERVAL 10 DAY");
    }

    public int countOldNewOver10() {
        return queryFilteredCount("WHERE wo.status = 'New' AND wo.createdAt < NOW() - INTERVAL 10 DAY");
    }

    public List<WorkOrder> getRepairedNotBilled() {
        return queryFilteredOrders("WHERE wo.status = 'Repair Complete'");
    }

    public int countRepairedNotBilled() {
        return queryFilteredCount("WHERE wo.status = 'Repair Complete'");
    }

    public List<WorkOrder> getMyWO(int techId) {
        return queryFilteredOrders("WHERE wo.tech_id = ? AND wo.status NOT IN ('Billing Complete', 'Cancelled')", techId);
    }

    public int countMyWO(int techId) {
        return queryFilteredCount("WHERE wo.tech_id = ? AND wo.status NOT IN ('Billing Complete', 'Cancelled')", techId);
    }

    public List<WorkOrder> getWorkOrdersByCustomerId(int customerId) {
        return queryFilteredOrders("WHERE wo.customer_id = ?", customerId);
    }

    public List<WorkOrder> getWorkOrdersBySerialNumber(String serialNumber) {
        return queryFilteredOrders("WHERE wo.serialNumber LIKE ?", "%" + serialNumber + "%");
    }

    public List<WorkOrder> getWorkOrdersByModel(String model) {
        return queryFilteredOrders("WHERE wo.model LIKE ?", "%" + model + "%");
    }

    // ─── INSERT ORDER ────────────────────────────────────────────────────────────

    public int insertOrderIntoDatabase(String status, String type, String model, String serialNumber,
                                       String problemDesc, int customerId, String vendorId,
                                       String warrantyNumber, double deposit, String repairType) {
        String sql = "INSERT INTO work_order (status, type, model, serialNumber, problemDesc, customer_id, vendorId, warrantyNumber, deposit_amount, repair_type, createdAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, status);
            stmt.setString(2, type);
            stmt.setString(3, model);
            stmt.setString(4, serialNumber);
            stmt.setString(5, problemDesc);
            stmt.setInt(6, customerId);
            stmt.setString(7, vendorId);
            stmt.setString(8, warrantyNumber);
            stmt.setDouble(9, deposit);
            stmt.setString(10, repairType);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    // ─── SEARCH ──────────────────────────────────────────────────────────────────

    public List<Integer> getCustomerIdsByPhone(String phone) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM customer WHERE phone LIKE ?";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + phone + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    public List<Integer> getCustomerIdsByField(String column, String value) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM customer WHERE " + column + " LIKE ?";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + value + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    public List<Integer> getCustomerIdsByFullName(String firstName, String lastName) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM customer WHERE first_name LIKE ? AND last_name LIKE ?";
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + firstName + "%");
            ps.setString(2, "%" + lastName + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    // ─── GET SINGLE ORDER ────────────────────────────────────────────────────────

    public WorkOrder getWorkOrderById(int woNumber) {
        String sql = "SELECT wo.workorder, wo.status, wo.type, " +
                "DATE_FORMAT(wo.createdAt, '%Y-%m-%d %H:%i') AS createdAt, " +
                "wo.vendorId, wo.warrantyNumber, wo.model, wo.serialNumber, " +
                "wo.problemDesc, wo.customer_id, wo.deposit_amount, wo.tech_id, " +
                "COALESCE(c.first_name, '') AS first_name, " +
                "COALESCE(c.last_name,  '') AS last_name, " +
                "COALESCE(t.username,   '') AS tech_username " +
                "FROM work_order wo " +
                "LEFT JOIN customer   c ON wo.customer_id = c.id " +
                "LEFT JOIN technician t ON wo.tech_id     = t.id " +
                "WHERE wo.workorder = ?";

        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, woNumber);
            try (ResultSet rs = stmt.executeQuery()) {
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
                    wo.setTechUsername(rs.getString("tech_username"));
                    wo.setCustomerName(rs.getString("first_name") + " " + rs.getString("last_name"));
                    return wo;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ─── SHOP STATS ──────────────────────────────────────────────────────────────

    public static double[] loadShopStats(LocalDate fromDate, LocalDate toDate) {
        String sql = """
        SELECT SUM(r.price) AS total_labour,
               COUNT(DISTINCT w.workorder) AS total_repairs,
               SUM(w.pst) AS total_pst,
               SUM(w.gst) AS total_gst
        FROM work_order_repairs r
        JOIN work_order w ON w.workorder = r.workorder_id
        WHERE w.status IN ('Repair Complete', 'Billing Complete')
        AND r.repair_date BETWEEN ? AND ?
    """;
        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(fromDate));
            ps.setDate(2, java.sql.Date.valueOf(toDate));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new double[]{
                            rs.getDouble("total_labour"),
                            rs.getDouble("total_repairs"),
                            rs.getDouble("total_pst"),
                            rs.getDouble("total_gst")
                    };
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new double[]{0, 0, 0, 0};
    }
}
