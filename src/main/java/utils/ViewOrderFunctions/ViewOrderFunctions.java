package utils.ViewOrderFunctions;

import DB.DbConfig;
import Skeletons.FilesHandler;
import Skeletons.WorkOrder;
import io.github.palexdev.materialfx.controls.MFXListView;

import java.sql.*;

public class ViewOrderFunctions {
//    public static void loadFilesFromDb(MFXListView<FilesHandler> filesList, WorkOrder currentWorkOrder ) {
//        filesList.getItems().clear();
//        String sql = "SELECT id, file_name FROM work_order_files WHERE workorder_id = ?";
//        try {
//            Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
//            PreparedStatement ps = conn.prepareStatement(sql);
//            ps.setInt(1, currentWorkOrder.getWorkorderNumber()); // make sure this is set!
//            ResultSet rs = ps.executeQuery();
//            while (rs.next()) {
//                filesList.getItems().add(new FilesHandler(rs.getInt("id"), rs.getString("file_name")));
//            }
//            rs.close(); ps.close(); conn.close();
//        } catch (SQLException e) {
//            System.out.println("issue during loading files");
//        }
//    }
}
