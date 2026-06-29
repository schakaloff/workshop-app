# File Storage: DB BLOB → SFTP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move file storage from MySQL BLOBs to Ubuntu server filesystem via SFTP, storing only the relative path in the DB.

**Architecture:** A dedicated `workshopfiles` Linux user (SFTP-only, chrooted) receives uploads from the JavaFX app via JSch SFTP. The DB column `file_data LONGBLOB` is replaced with `file_path VARCHAR(500)`. A one-time migration uploads existing 18 BLOBs before the column is dropped.

**Tech Stack:** Java 21, JSch (mwiede fork 0.2.20), MySQL 8, JUnit 5.10.2, Maven Shade

## Global Constraints

- Java source/target: 21
- SSH host: `100.92.61.4`, port: `1337`, SFTP user: `workshopfiles`
- Chroot base on server: `/var/workshop-files`; uploads subdirectory (visible inside chroot): `/uploads`
- Remote paths stored in DB are relative to `/uploads`, e.g. `{workorderId}/{filename}`
- File size limit already enforced in UI: 3 MB max
- Branch: `files`

---

## File Map

| Action | File | Responsibility |
|--------|------|----------------|
| Create | `src/main/java/DB/SftpConfig.java` | SFTP connection constants |
| Create | `src/main/java/utils/SftpClient.java` | upload / downloadToTemp / delete via JSch |
| Create | `src/main/java/utils/MigrateFilesToSftp.java` | one-time BLOB → SFTP migration runner |
| Modify | `pom.xml` | add JSch dependency |
| Modify | `src/main/java/Controllers/DbRepo/ViewControllerQueries.java` | `addFileToDb`, `openFileFromDb` |
| Modify | `src/main/java/Controllers/PaymentController.java` | `attachPdfToWorkOrder` |
| Modify | `src/main/java/Controllers/AdditionalDepositController.java` | `attachPdfToWorkOrder` |
| Modify | `src/main/java/utils/DeletingFilesMethods.java` | `deleteFileFromDb` — add SFTP delete |

---

### Task 1: Server setup — create SFTP user and directory

**Files:** No Java files. SSH commands on `teltron-server`.

**Interfaces:**
- Produces: SFTP endpoint reachable at `sftp://100.92.61.4:1337` with user `workshopfiles`, password `***REMOVED-ROTATED-SFTP-PASSWORD***`, chroot `/var/workshop-files`, uploads dir `/var/workshop-files/uploads`

- [ ] **Step 1: SSH to server and create directory structure**

```bash
ssh teltron-server
sudo mkdir -p /var/workshop-files/uploads
sudo chown root:root /var/workshop-files
sudo chmod 755 /var/workshop-files
sudo chown teltronics:teltronics /var/workshop-files/uploads
sudo chmod 755 /var/workshop-files/uploads
```

- [ ] **Step 2: Create SFTP-only user**

```bash
sudo useradd -m -d /var/workshop-files -s /usr/sbin/nologin workshopfiles
sudo passwd workshopfiles
# Enter: ***REMOVED-ROTATED-SFTP-PASSWORD***
```

- [ ] **Step 3: Set uploads ownership to workshopfiles**

```bash
sudo chown workshopfiles:workshopfiles /var/workshop-files/uploads
```

- [ ] **Step 4: Add sshd_config block**

```bash
sudo tee -a /etc/ssh/sshd_config <<'EOF'

Match User workshopfiles
    ChrootDirectory /var/workshop-files
    ForceCommand internal-sftp
    AllowTcpForwarding no
    X11Forwarding no
EOF
sudo systemctl restart sshd
```

- [ ] **Step 5: Verify SFTP access from dev machine**

```bash
# Run from your local machine (NOT on server):
sftp -P 1337 workshopfiles@100.92.61.4
# Enter password: ***REMOVED-ROTATED-SFTP-PASSWORD***
# Expected: sftp> prompt
sftp> ls
# Expected: uploads
sftp> exit
```

- [ ] **Step 6: Commit (no code changes yet — just note server is ready)**

```bash
git commit --allow-empty -m "chore: sftp server configured (workshopfiles user, chroot /var/workshop-files)"
```

---

### Task 2: Add JSch dependency and SftpConfig

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/DB/SftpConfig.java`

**Interfaces:**
- Produces:
  - `SftpConfig.HOST` = `"100.92.61.4"`
  - `SftpConfig.PORT` = `1337`
  - `SftpConfig.USER` = `"workshopfiles"`
  - `SftpConfig.PASSWORD` = `"***REMOVED-ROTATED-SFTP-PASSWORD***"`
  - `SftpConfig.REMOTE_BASE` = `"/uploads"`

- [ ] **Step 1: Add JSch to pom.xml**

In `pom.xml`, inside `<dependencies>`, add after the HikariCP dependency:

```xml
        <dependency>
            <groupId>com.github.mwiede</groupId>
            <artifactId>jsch</artifactId>
            <version>0.2.20</version>
        </dependency>
```

- [ ] **Step 2: Create SftpConfig.java**

Create `src/main/java/DB/SftpConfig.java`:

```java
package DB;

public class SftpConfig {
    public static final String HOST = "100.92.61.4";
    public static final int    PORT = 1337;
    public static final String USER = "workshopfiles";
    public static final String PASSWORD = "***REMOVED-ROTATED-SFTP-PASSWORD***";
    public static final String REMOTE_BASE = "/uploads";
}
```

- [ ] **Step 3: Verify compile**

```bash
./mvnw compile -q
# Expected: BUILD SUCCESS, no errors
```

- [ ] **Step 4: Commit**

```bash
git add pom.xml src/main/java/DB/SftpConfig.java
git commit -m "feat: add JSch dependency and SftpConfig"
```

---

### Task 3: Create SftpClient utility

**Files:**
- Create: `src/main/java/utils/SftpClient.java`

**Interfaces:**
- Consumes: `SftpConfig.HOST`, `PORT`, `USER`, `PASSWORD`, `REMOTE_BASE`
- Produces:
  - `SftpClient.upload(int workorderId, String filename, InputStream data) → String`  
    Returns relative path `"{workorderId}/{filename}"` (or `"{workorderId}/{basename}_{epoch}.{ext}"` on collision)
  - `SftpClient.downloadToTemp(String relativePath, String ext) → File`  
    Returns temp `File` with content from `/uploads/{relativePath}`
  - `SftpClient.delete(String relativePath) → void`  
    Silently ignores missing file

- [ ] **Step 1: Create SftpClient.java**

Create `src/main/java/utils/SftpClient.java`:

```java
package utils;

import DB.SftpConfig;
import com.jcraft.jsch.*;

import java.io.*;
import java.nio.file.Files;

public class SftpClient {

    public static String upload(int workorderId, String filename, InputStream data) throws Exception {
        Session session = openSession();
        try {
            ChannelSftp ch = openChannel(session);
            try {
                String dir = SftpConfig.REMOTE_BASE + "/" + workorderId;
                mkdirIfAbsent(ch, dir);

                String remotePath = dir + "/" + filename;
                if (remoteExists(ch, remotePath)) {
                    String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.')) : "";
                    String base = filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;
                    filename = base + "_" + System.currentTimeMillis() + ext;
                    remotePath = dir + "/" + filename;
                }

                ch.put(data, remotePath);
                return workorderId + "/" + filename;
            } finally {
                ch.disconnect();
            }
        } finally {
            session.disconnect();
        }
    }

    public static File downloadToTemp(String relativePath, String ext) throws Exception {
        Session session = openSession();
        try {
            ChannelSftp ch = openChannel(session);
            try {
                File tmp = File.createTempFile("wo_", ext == null ? "" : ext);
                tmp.deleteOnExit();
                try (OutputStream out = new FileOutputStream(tmp)) {
                    ch.get(SftpConfig.REMOTE_BASE + "/" + relativePath, out);
                }
                return tmp;
            } finally {
                ch.disconnect();
            }
        } finally {
            session.disconnect();
        }
    }

    public static void delete(String relativePath) throws Exception {
        Session session = openSession();
        try {
            ChannelSftp ch = openChannel(session);
            try {
                String remote = SftpConfig.REMOTE_BASE + "/" + relativePath;
                if (remoteExists(ch, remote)) {
                    ch.rm(remote);
                }
            } finally {
                ch.disconnect();
            }
        } finally {
            session.disconnect();
        }
    }

    private static Session openSession() throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(SftpConfig.USER, SftpConfig.HOST, SftpConfig.PORT);
        session.setPassword(SftpConfig.PASSWORD);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(10_000);
        return session;
    }

    private static ChannelSftp openChannel(Session session) throws JSchException {
        ChannelSftp ch = (ChannelSftp) session.openChannel("sftp");
        ch.connect(5_000);
        return ch;
    }

    private static void mkdirIfAbsent(ChannelSftp ch, String path) {
        try {
            ch.mkdir(path);
        } catch (SftpException ignored) {
            // directory already exists
        }
    }

    private static boolean remoteExists(ChannelSftp ch, String path) {
        try {
            ch.lstat(path);
            return true;
        } catch (SftpException e) {
            return false;
        }
    }
}
```

- [ ] **Step 2: Compile**

```bash
./mvnw compile -q
# Expected: BUILD SUCCESS
```

- [ ] **Step 3: Manual smoke test — verify upload/download round-trip**

Temporarily add a `main` method or run from a scratch test. Easier: proceed to Task 4 (migration) which tests SftpClient against real server.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/utils/SftpClient.java
git commit -m "feat: add SftpClient (upload/downloadToTemp/delete)"
```

---

### Task 4: DB migration — add file_path column and migrate existing BLOBs

**Files:**
- Create: `src/main/java/utils/MigrateFilesToSftp.java`

**Interfaces:**
- Consumes: `SftpClient.upload(int, String, InputStream)`, `DbConfig`, `SftpConfig`
- Produces: all existing rows in `work_order_files` have `file_path` set; `file_data` still present (dropped in Task 5)

- [ ] **Step 1: Add file_path column to DB**

```bash
ssh teltron-server "mysql -u appuser -pteltron2026 workshopdb -e \"ALTER TABLE work_order_files ADD COLUMN file_path VARCHAR(500) NULL;\""
# Expected: no error output
```

- [ ] **Step 2: Verify column added**

```bash
ssh teltron-server "mysql -u appuser -pteltron2026 workshopdb -e \"DESCRIBE work_order_files;\""
# Expected: file_path column present, nullable
```

- [ ] **Step 3: Create MigrateFilesToSftp.java**

Create `src/main/java/utils/MigrateFilesToSftp.java`:

```java
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
```

- [ ] **Step 4: Compile**

```bash
./mvnw compile -q
# Expected: BUILD SUCCESS
```

- [ ] **Step 5: Run migration**

```bash
./mvnw exec:java -Dexec.mainClass="utils.MigrateFilesToSftp" -Dexec.classpathScope=compile
# Expected output example:
# Migrating file id=1 workorder=42 name=invoice.pdf (45231 bytes)
#   -> uploaded to 42/invoice.pdf
# ...
# Migration complete. 18 files migrated.
```

- [ ] **Step 6: Verify on server that files exist**

```bash
ssh teltron-server "find /var/workshop-files/uploads -type f | head -20"
# Expected: list of uploaded files
```

- [ ] **Step 7: Verify file_path populated in DB**

```bash
ssh teltron-server "mysql -u appuser -pteltron2026 workshopdb -e \"SELECT id, file_name, file_path FROM work_order_files LIMIT 10;\""
# Expected: all rows have file_path set (no NULLs)
```

- [ ] **Step 8: Commit**

```bash
git add src/main/java/utils/MigrateFilesToSftp.java
git commit -m "feat: one-time BLOB-to-SFTP migration runner"
```

---

### Task 5: Drop file_data column from DB

**Files:** No Java changes. DB schema only.

**Interfaces:**
- Consumes: all rows have `file_path` set (Task 4 complete)
- Produces: `work_order_files` has no `file_data` column; `file_path` is NOT NULL

- [ ] **Step 1: Confirm no NULLs in file_path**

```bash
ssh teltron-server "mysql -u appuser -pteltron2026 workshopdb -e \"SELECT COUNT(*) as nulls FROM work_order_files WHERE file_path IS NULL;\""
# Expected: nulls = 0
```

- [ ] **Step 2: Drop file_data and make file_path NOT NULL**

```bash
ssh teltron-server "mysql -u appuser -pteltron2026 workshopdb -e \"
ALTER TABLE work_order_files
  DROP COLUMN file_data,
  MODIFY file_path VARCHAR(500) NOT NULL;
\""
# Expected: no error
```

- [ ] **Step 3: Verify final schema**

```bash
ssh teltron-server "mysql -u appuser -pteltron2026 workshopdb -e \"DESCRIBE work_order_files;\""
# Expected: columns: id, workorder_id, file_name, file_path — no file_data
```

- [ ] **Step 4: Commit**

```bash
git commit --allow-empty -m "chore: drop file_data BLOB column, file_path NOT NULL"
```

---

### Task 6: Update ViewControllerQueries — addFileToDb and openFileFromDb

**Files:**
- Modify: `src/main/java/Controllers/DbRepo/ViewControllerQueries.java`

**Interfaces:**
- Consumes: `SftpClient.upload(int, String, InputStream)`, `SftpClient.downloadToTemp(String, String)`
- Produces:
  - `addFileToDb(int workorderNumber, java.io.File file)` — SFTP uploads file, INSERTs `file_path`
  - `openFileFromDb(int fileId)` — returns `Object[]{name, InputStream}` (from temp file via SFTP)

- [ ] **Step 1: Update addFileToDb in ViewControllerQueries.java**

In `ViewControllerQueries.java`, replace the entire `addFileToDb` method (currently lines ~352-364):

```java
    public static void addFileToDb(int workorderNumber, java.io.File file) throws Exception {
        String remotePath;
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            remotePath = SftpClient.upload(workorderNumber, file.getName(), fis);
        }

        String sql = "INSERT INTO work_order_files (workorder_id, file_name, file_path) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, workorderNumber);
            ps.setString(2, file.getName());
            ps.setString(3, remotePath);
            ps.executeUpdate();
        }
    }
```

- [ ] **Step 2: Update openFileFromDb in ViewControllerQueries.java**

Replace the entire `openFileFromDb` method (currently lines ~386-402):

```java
    public static Object[] openFileFromDb(int fileId) throws Exception {
        String sql = "SELECT file_name, file_path FROM work_order_files WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, fileId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String name = rs.getString("file_name");
                String path = rs.getString("file_path");
                String ext  = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
                java.io.File tmp = SftpClient.downloadToTemp(path, ext);
                return new Object[]{name, new java.io.FileInputStream(tmp)};
            }
        }

        return null;
    }
```

- [ ] **Step 3: Remove unused FileInputStream import if now unused elsewhere**

Check top of `ViewControllerQueries.java` for `import java.io.FileInputStream;` — only remove it if `addFileToDb` was the sole user.

- [ ] **Step 4: Compile**

```bash
./mvnw compile -q
# Expected: BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/Controllers/DbRepo/ViewControllerQueries.java
git commit -m "feat: update addFileToDb and openFileFromDb to use SFTP"
```

---

### Task 7: Update PaymentController and AdditionalDepositController

**Files:**
- Modify: `src/main/java/Controllers/PaymentController.java`
- Modify: `src/main/java/Controllers/AdditionalDepositController.java`

**Interfaces:**
- Consumes: `SftpClient.upload(int, String, InputStream)`
- Produces: `attachPdfToWorkOrder(byte[], String)` writes temp file → SFTP → INSERT `file_path`

- [ ] **Step 1: Update PaymentController.attachPdfToWorkOrder**

In `PaymentController.java`, replace `attachPdfToWorkOrder` (lines ~338-347):

```java
    private void attachPdfToWorkOrder(byte[] pdfBytes, String displayName) throws Exception {
        java.io.File tmp = java.io.File.createTempFile("pdf_", ".pdf");
        tmp.deleteOnExit();
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) {
            fos.write(pdfBytes);
        }

        String remotePath;
        try (java.io.FileInputStream fis = new java.io.FileInputStream(tmp)) {
            remotePath = SftpClient.upload(currentWorkOrder.getWorkorderNumber(), displayName, fis);
        }

        String sql = "INSERT INTO work_order_files (workorder_id, file_name, file_path) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentWorkOrder.getWorkorderNumber());
            ps.setString(2, displayName);
            ps.setString(3, remotePath);
            ps.executeUpdate();
        }
    }
```

- [ ] **Step 2: Update AdditionalDepositController.attachPdfToWorkOrder**

In `AdditionalDepositController.java`, replace `attachPdfToWorkOrder` (lines ~198-207) with identical implementation:

```java
    private void attachPdfToWorkOrder(byte[] pdfBytes, String displayName) throws Exception {
        java.io.File tmp = java.io.File.createTempFile("pdf_", ".pdf");
        tmp.deleteOnExit();
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) {
            fos.write(pdfBytes);
        }

        String remotePath;
        try (java.io.FileInputStream fis = new java.io.FileInputStream(tmp)) {
            remotePath = SftpClient.upload(currentWorkOrder.getWorkorderNumber(), displayName, fis);
        }

        String sql = "INSERT INTO work_order_files (workorder_id, file_name, file_path) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentWorkOrder.getWorkorderNumber());
            ps.setString(2, displayName);
            ps.setString(3, remotePath);
            ps.executeUpdate();
        }
    }
```

- [ ] **Step 3: Compile**

```bash
./mvnw compile -q
# Expected: BUILD SUCCESS
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/Controllers/PaymentController.java src/main/java/Controllers/AdditionalDepositController.java
git commit -m "feat: update PDF attachment to use SFTP in Payment and Deposit controllers"
```

---

### Task 8: Update DeletingFilesMethods — delete from SFTP on file delete

**Files:**
- Modify: `src/main/java/utils/DeletingFilesMethods.java`

**Interfaces:**
- Consumes: `SftpClient.delete(String)`, DB `file_path` column
- Produces: `deleteFileFromDb(int, int)` — fetches `file_path`, SFTP deletes, then SQL DELETEs

- [ ] **Step 1: Update deleteFileFromDb in DeletingFilesMethods.java**

Replace `deleteFileFromDb` (lines ~75-88):

```java
    private void deleteFileFromDb(int fileId, int workorderNumber) {
        String select = "SELECT file_path FROM work_order_files WHERE id = ? AND workorder_id = ?";
        String delete = "DELETE FROM work_order_files WHERE id = ? AND workorder_id = ?";

        try (Connection conn = DriverManager.getConnection(DbConfig.url, DbConfig.user, DbConfig.password)) {
            String filePath = null;
            try (PreparedStatement sel = conn.prepareStatement(select)) {
                sel.setInt(1, fileId);
                sel.setInt(2, workorderNumber);
                ResultSet rs = sel.executeQuery();
                if (rs.next()) filePath = rs.getString("file_path");
            }

            if (filePath != null) {
                SftpClient.delete(filePath);
            }

            try (PreparedStatement del = conn.prepareStatement(delete)) {
                del.setInt(1, fileId);
                del.setInt(2, workorderNumber);
                del.executeUpdate();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
```

- [ ] **Step 2: Add import for SftpClient at top of DeletingFilesMethods.java**

Add after existing imports:

```java
import utils.SftpClient;
```

- [ ] **Step 3: Compile**

```bash
./mvnw compile -q
# Expected: BUILD SUCCESS
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/utils/DeletingFilesMethods.java
git commit -m "feat: delete SFTP file when removing work order attachment"
```

---

### Task 9: End-to-end manual verification

**Files:** No code changes.

- [ ] **Step 1: Build fat JAR**

```bash
./mvnw package -q
# Expected: BUILD SUCCESS, target/workordermanager-app.jar created
```

- [ ] **Step 2: Run app and upload a file**

Launch the app. Open any work order. Use the file attach button to upload a PDF or image.  
Expected: file appears in the files list with no errors.

- [ ] **Step 3: Verify file landed on server**

```bash
ssh teltron-server "find /var/workshop-files/uploads -type f"
# Expected: newly uploaded file present
```

- [ ] **Step 4: Open the file from the app**

Click the file in the list, open it.  
Expected: file opens in system default app (PDF viewer / image viewer).

- [ ] **Step 5: Delete the file from the app**

Right-click → Delete.  
Expected: file disappears from list.

- [ ] **Step 6: Verify file removed from server**

```bash
ssh teltron-server "find /var/workshop-files/uploads -type f"
# Expected: file no longer present
```

- [ ] **Step 7: Test auto PDF attachment (Payment flow)**

Complete a payment on a work order. Navigate to its files.  
Expected: PDF appears in file list, opens correctly.

- [ ] **Step 8: Final commit**

```bash
git add -A
git commit -m "chore: SFTP file storage complete — all manual tests passing"
```
