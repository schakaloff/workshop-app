package utils;

import DB.SftpConfig;
import com.jcraft.jsch.*;

import java.io.*;

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
