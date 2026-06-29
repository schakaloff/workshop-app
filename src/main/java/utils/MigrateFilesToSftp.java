package utils;

import DB.DbConfig;

import java.io.ByteArrayInputStream;
import java.sql.*;

public class MigrateFilesToSftp {

    public static void main(String[] args) throws Exception {
        String select = "SELECT id, workorder_id, file_name, file_data FROM work_order_files WHERE file_path IS NULL AND file_data IS NOT NULL";
        String update = "UPDATE work_order_files SET file_path = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement sel = conn.prepareStatement(select);
             PreparedStatement upd = conn.prepareStatement(update)) {

            ResultSet rs = sel.executeQuery();
            int count = 0;

            while (rs.next()) {
                int id = rs.getInt("id");
                int workorderId = rs.getInt("workorder_id");
                String fileName = rs.getString("file_name");
                byte[] data = rs.getBytes("file_data");

                System.out.printf("Migrating file id=%d workorder=%d name=%s (%d bytes)%n",
                        id, workorderId, fileName, data.length);

                String remotePath = SftpClient.upload(workorderId, fileName, new ByteArrayInputStream(data));

                upd.setString(1, remotePath);
                upd.setInt(2, id);
                upd.executeUpdate();

                System.out.printf("  -> uploaded to %s%n", remotePath);
                count++;
            }

            System.out.printf("Migration complete. %d files migrated.%n", count);
        }
    }
}
