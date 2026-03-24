package Controllers.DbRepo;

import DB.DbConfig;
import Skeletons.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ViewControllerQueries {

    public static void updateStatusInDb(int workorderNumber, String newStatus, double labourAmount) {
        String sql = """
            UPDATE work_order
            SET status = ?,
                finished_at = CASE
                    WHEN ? IN ('Repair Complete', 'Billing Complete') AND finished_at IS NULL THEN NOW()
                    ELSE finished_at
                END,
                labour_amount = ?
            WHERE workorder = ?
        """;

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setString(2, newStatus);
            ps.setDouble(3, labourAmount);
            ps.setInt(4, workorderNumber);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<String> loadTechList() {
        List<String> techNames = new ArrayList<>();
        String sql = "SELECT username FROM technician";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                techNames.add(rs.getString("username"));
            }

        } catch (SQLException e) {
            System.out.println("error during loading tech list");
        }

        return techNames;
    }

    public static int getTechIdByUsername(String username) {
        int id = 0;
        String sql = "SELECT id FROM technician WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getInt("id");
            }

        } catch (SQLException e) {
            System.out.println("error during loading tech id");
        }

        return id;
    }

    public static String getTechUsernameById(int techId) {
        String username = "";
        String sql = "SELECT username FROM technician WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, techId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                username = rs.getString("username");
            }

        } catch (SQLException e) {
            System.out.println("error during loading tech username");
        }

        return username;
    }

    public static void updateTechIdInDb(int techId, int workorderNumber) {
        String sql = "UPDATE work_order SET tech_id = ? WHERE workorder = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, techId);
            ps.setInt(2, workorderNumber);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void refreshWorkOrderFromDb(WorkOrder currentWorkOrder) {
        String sql = "SELECT status, type, model, serialNumber, problemDesc, vendorId, warrantyNumber, tech_id FROM work_order WHERE workorder = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, currentWorkOrder.getWorkorderNumber());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                currentWorkOrder.setStatus(rs.getString("status"));
                currentWorkOrder.setType(rs.getString("type"));
                currentWorkOrder.setModel(rs.getString("model"));
                currentWorkOrder.setSerialNumber(rs.getString("serialNumber"));
                currentWorkOrder.setProblemDesc(rs.getString("problemDesc"));
                currentWorkOrder.setVendorId(rs.getString("vendorId"));
                currentWorkOrder.setWarrantyNumber(rs.getString("warrantyNumber"));

                int techId = rs.getInt("tech_id");
                if (rs.wasNull()) techId = 0;
                currentWorkOrder.setTechId(techId);
            }

        } catch (SQLException e) {
            System.out.println("issue during refreshing work order");
        }
    }

    public static boolean hasAtLeastOneLabourNoteDb(int workorderNumber) {
        boolean ok = false;
        String sql = "SELECT tech, description FROM work_order_repairs WHERE workorder_id = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, workorderNumber);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String tech = rs.getString("tech");
                String desc = rs.getString("description");

                if (tech != null && !tech.equals("") && desc != null && !desc.equals("")) {
                    ok = true;
                    break;
                }
            }

        } catch (SQLException e) {
            System.out.println("error during checking labour notes");
        }

        return ok;
    }

    public static List<WorkTable> loadRepairsFromDb(int workorderNumber) {
        List<WorkTable> rows = new ArrayList<>();
        String sql = "SELECT repair_date, tech, description, price FROM work_order_repairs WHERE workorder_id = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, workorderNumber);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                LocalDate date = rs.getDate("repair_date").toLocalDate();
                String tech = rs.getString("tech");
                String desc = rs.getString("description");
                double price = rs.getDouble("price");

                rows.add(new WorkTable(date, tech, desc, price));
            }

        } catch (SQLException e) {
            System.out.println("error during loading repairs");
        }

        return rows;
    }

    public static void saveRepairsToDb(int workorderNumber, List<WorkTable> repairData) {
        String deleteSQL = "DELETE FROM work_order_repairs WHERE workorder_id = ?";
        String insertSQL = "INSERT INTO work_order_repairs (workorder_id, repair_date, tech, description, price) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password)) {

            PreparedStatement del = conn.prepareStatement(deleteSQL);
            del.setInt(1, workorderNumber);
            del.executeUpdate();
            del.close();

            PreparedStatement ps = conn.prepareStatement(insertSQL);
            for (WorkTable r : repairData) {
                if (r.getTech() == null || r.getTech().isBlank()) continue;
                ps.setInt(1, workorderNumber);
                ps.setDate(2, java.sql.Date.valueOf(r.getDate()));
                ps.setString(3, r.getTech());
                ps.setString(4, r.getDescription());
                ps.setDouble(5, r.getPrice());
                ps.addBatch();
            }
            ps.executeBatch();
            ps.close();

        } catch (SQLException e) {
            System.out.println("error during saving repairs");
        }
    }

    public static double depositFromDb(int workorderNumber) {
        double deposit = 0.0;
        String sql = "SELECT deposit_amount FROM work_order WHERE workorder = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, workorderNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                deposit = rs.getDouble("deposit_amount");
            }

        } catch (SQLException e) {
            System.out.println("error during loading deposit");
        }

        return deposit;
    }

    public static double labourTotalDb(int workorderNumber) {
        double total = 0.0;
        String sql = "SELECT SUM(price) FROM work_order_repairs WHERE workorder_id = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, workorderNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                total = rs.getDouble(1);
            }

        } catch (SQLException e) {
            System.out.println("error during loading labour total");
        }

        return total;
    }

    public static double partsTotalDb(int workorderNumber) {
        double total = 0.0;
        String sql = "SELECT SUM(total_price) FROM work_order_parts WHERE workorder_id = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, workorderNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                total = rs.getDouble(1);
            }

        } catch (SQLException e) {
            System.out.println("error during loading parts total");
        }

        return total;
    }

    public static void updateOrderInDb(int workorderNumber, String type, String model, String serialNumber,
                                       String problemDesc, String vendorId, String warrantyNumber,
                                       String serviceNotes, int techId, String status) {
        String sql = """
            UPDATE work_order 
            SET type = ?, 
                model = ?, 
                serialNumber = ?, 
                problemDesc = ?, 
                vendorId = ?, 
                warrantyNumber = ?, 
                service_notes = ?, 
                tech_id = ?, 
                status = ?
            WHERE workorder = ?
        """;

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, type);
            ps.setString(2, model);
            ps.setString(3, serialNumber);
            ps.setString(4, problemDesc);
            ps.setString(5, vendorId);
            ps.setString(6, warrantyNumber);
            ps.setString(7, serviceNotes);
            ps.setInt(8, techId);
            ps.setString(9, status);
            ps.setInt(10, workorderNumber);

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("error during updating order");
            e.printStackTrace();
        }
    }

    public static void updateServiceNotesInDb(int workorderNumber, String serviceNotes) {
        String sql = "UPDATE work_order SET service_notes = ? WHERE workorder = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, serviceNotes);
            ps.setInt(2, workorderNumber);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("error during updating service notes");
            e.printStackTrace();
        }
    }

    public static void addFileToDb(int workorderNumber, java.io.File file) throws Exception {
        String sql = "INSERT INTO work_order_files (workorder_id, file_name, file_data) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql);
             FileInputStream fis = new FileInputStream(file)) {

            ps.setInt(1, workorderNumber);
            ps.setString(2, file.getName());
            ps.setBinaryStream(3, fis, (long) file.length());
            ps.executeUpdate();
        }
    }

    public static FilesHandler[] loadFilesFromDb(int workorderNumber) {
        List<FilesHandler> files = new ArrayList<>();
        String sql = "SELECT id, file_name FROM work_order_files WHERE workorder_id = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, workorderNumber);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                files.add(new FilesHandler(rs.getInt("id"), rs.getString("file_name")));
            }

        } catch (SQLException e) {
            System.out.println("issue during loading files");
        }

        return files.toArray(new FilesHandler[0]);
    }

    public static Object[] openFileFromDb(int fileId) throws Exception {
        String sql = "SELECT file_name, file_data FROM work_order_files WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, fileId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String name = rs.getString("file_name");
                InputStream in = rs.getBinaryStream("file_data");
                return new Object[]{name, in};
            }
        }

        return null;
    }

    public static void savePartsToDb(int workorderNumber, List<PartTable> partsData) {
        String deleteSQL = "DELETE FROM work_order_parts WHERE workorder_id = ?";
        String insertSQL = "INSERT INTO work_order_parts (workorder_id, part_name, quantity, price, total_price) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password)) {

            PreparedStatement del = conn.prepareStatement(deleteSQL);
            del.setInt(1, workorderNumber);
            del.executeUpdate();
            del.close();

            PreparedStatement ps = conn.prepareStatement(insertSQL);
            for (PartTable part : partsData) {
                if (part.getName() == null || part.getName().isBlank()) continue;
                ps.setInt(1, workorderNumber);
                ps.setString(2, part.getName());
                ps.setInt(3, part.getQuantity());
                ps.setDouble(4, part.getPrice());
                ps.setDouble(5, part.getTotalPrice());
                ps.addBatch();
            }
            ps.executeBatch();
            ps.close();

        } catch (SQLException e) {
            System.out.println("error during saving parts");
        }
    }

    public static List<PartTable> loadPartsFromDb(int workorderNumber) {
        List<PartTable> rows = new ArrayList<>();
        String sql = "SELECT id, part_name, quantity, price, total_price FROM work_order_parts WHERE workorder_id = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, workorderNumber);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("part_name");
                int qty = rs.getInt("quantity");
                double price = rs.getDouble("price");
                double total = rs.getDouble("total_price");

                rows.add(new PartTable(id, name, qty, price, total));
            }

        } catch (SQLException e) {
            System.out.println("error during loading parts");
        }

        return rows;
    }

    public static String loadServiceNotes(int workorderNumber) {
        String sql = "SELECT service_notes FROM work_order WHERE workorder = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, workorderNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("service_notes");
            }

        } catch (SQLException e) {
            System.out.println("issue during loading service notes");
        }

        return null;
    }
}