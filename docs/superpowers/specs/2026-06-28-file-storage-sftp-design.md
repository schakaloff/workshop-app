# File Storage: DB BLOB → SFTP Filesystem

**Date:** 2026-06-28  
**Branch:** files

## Problem

Files stored as `LONGBLOB` in `work_order_files.file_data`. DB grows unbounded with binary data. Goal: store files on Ubuntu server filesystem, keep only path reference in DB.

## Chosen Approach: SFTP

SSH already runs on `teltron-server` (100.92.61.4, port 1337). Dedicated SFTP-only user `workshopfiles` with chroot to `/var/workshop-files`. No new services required. Credentials stored same pattern as `DbConfig`.

## Server Setup

1. Create `/var/workshop-files` owned by `root:root`, mode `755`
2. Create user `workshopfiles`, no shell (`/usr/sbin/nologin`), home `/var/workshop-files`
3. Create `/var/workshop-files/uploads` owned by `workshopfiles`, mode `755`
4. Add to `/etc/ssh/sshd_config`:

```
Match User workshopfiles
    ChrootDirectory /var/workshop-files
    ForceCommand internal-sftp
    AllowTcpForwarding no
    X11Forwarding no
```

5. `systemctl restart sshd`

Files stored at (within chroot): `/uploads/{workorder_id}/{filename}`  
Actual path on disk: `/var/workshop-files/uploads/{workorder_id}/{filename}`

## DB Schema Change

```sql
ALTER TABLE work_order_files ADD COLUMN file_path VARCHAR(500) NULL;
-- run migration script to populate file_path from existing BLOBs
ALTER TABLE work_order_files DROP COLUMN file_data;
ALTER TABLE work_order_files MODIFY file_path VARCHAR(500) NOT NULL;
```

## New Java Classes

### `DB/SftpConfig.java`
Constants: host, port (`1337`), user (`workshopfiles`), password, remote base path (`/uploads`).

### `utils/SftpClient.java`
Three static methods using JSch:

- `upload(int workorderId, String filename, InputStream data) → String`  
  Creates `/uploads/{workorderId}/` if absent. Uploads stream. Returns `"{workorderId}/{filename}"`.

- `downloadToTemp(String filePath, String ext) → File`  
  Downloads `/uploads/{filePath}` to a temp file. Returns temp `File`.

- `delete(String filePath)`  
  Removes `/uploads/{filePath}`. Silently ignores missing file.

## Changed Code Sites

| File | Method | Change |
|------|--------|--------|
| `ViewControllerQueries` | `addFileToDb` | SFTP upload → INSERT `file_path` (no `file_data`) |
| `ViewControllerQueries` | `openFileFromDb` | SFTP `downloadToTemp` → return stream from temp file |
| `PaymentController` | `attachPdfToWorkOrder` | Write bytes to tmp → SFTP upload → INSERT `file_path` |
| `AdditionalDepositController` | `attachPdfToWorkOrder` | Same as PaymentController |
| `DeletingFilesMethods` | `deleteFileFromDb` | SFTP `delete` then SQL DELETE |

## Migration (18 existing files)

One-time migration class `MigrateFilesToSftp`:
1. `SELECT id, workorder_id, file_name, file_data FROM work_order_files WHERE file_path IS NULL`
2. For each row: SFTP upload BLOB bytes → `UPDATE work_order_files SET file_path = ? WHERE id = ?`
3. Run before dropping `file_data` column.

## Dependency

Add to `pom.xml`:
```xml
<dependency>
    <groupId>com.github.mwiede</groupId>
    <artifactId>jsch</artifactId>
    <version>0.2.20</version>
</dependency>
```

(mwiede fork of JSch — actively maintained, Java 11+ compatible)

## File Naming Collision

If same filename uploaded twice for same workorder, append timestamp suffix: `basename_<epoch>.ext`.
