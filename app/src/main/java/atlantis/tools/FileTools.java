/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package atlantis.tools;

import atlantis.tools.Parser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.DecimalFormat;

/**
 *
 * @author cnsaeman
 */
public class FileTools {

    /**
     * Determine Filetype
     */
    public static String getFileType(String s) {
        if (s == null) {
            return null;
        }
        String tmp;
        tmp = Parser.cutFromLast(s, ".");
        if (tmp.equals("gz")) {
            tmp = Parser.cutFromLast(Parser.cutUntilLast(s, "."), ".") + "." + tmp;
        }
        return tmp;
    }

    public static boolean removeFolder(String name) {
        File folder = new File(name);
        if (folder.isDirectory()) {
            String[] entries = folder.list();
            for (int i = 0; i < entries.length; i++) {
                boolean del = removeFolder(name + "/" + entries[i]);
                if (!del) {
                    return false;
                }
            }
        }
        return folder.delete();
    }

    public static String fileSize(String n) {
        long l = new File(n).length();
        if (l < 1024) {
            return String.valueOf(l) + " Bytes";
        }
        DecimalFormat df = new DecimalFormat("0.000");
        if (l < (1024 * 1024)) {
            return df.format(l / 1024.0) + " KiB";
        }
        return df.format(l / 1024.0 / 1024.0) + " MiB";
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String md5checksum(String path) {
        String md5 = null;
        try {
            byte[] b = Files.readAllBytes(Paths.get(path));
            byte[] hash = MessageDigest.getInstance("MD5").digest(b);
            md5 = FileTools.byteArrayToHex(hash);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return md5;
    }

    /**
     * Deletes a file if it exists
     */
    public static boolean deleteIfExists(String tmp) {
        if (tmp == null) {
            return false;
        }
        File f = new File(tmp);
        if (f.exists()) {
            try {
                return f.delete();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    /**
     * Swaps a file s1 with file s2
     */
    public static boolean swapFile(String s1, String s2) throws IOException {
        String tmp = s1 + ".tmp";
        while (new File(tmp).exists()) {
            tmp += "t";
        }
        boolean succ = moveFile(s1, tmp);
        succ = succ && moveFile(s2, s1);
        succ = succ && moveFile(tmp, s2);
        return succ;
    }

    /**
     * Move a file from s1 to s2, first trying rename
     */
    public static boolean moveFile(String s1, String s2) throws IOException {
        boolean succ = (new File(s1)).renameTo(new File(s2));
        if (!succ) {
            try {
                copyFile(s1, s2);
                (new File(s1)).delete();
                succ = true;
            } catch (IOException e) {
                e.printStackTrace();
                succ = false;
            }
        }
        return succ;
    }

    /**
     * Kopiere eine Datei von s1 nach s2
     */
    public static void copyFile(String s1, String s2) throws IOException {
        FileInputStream fis = new FileInputStream(new File(s1));
        FileOutputStream fos = new FileOutputStream(new File(s2));
        byte[] buf = new byte[4096];
        int i = 0;
        while ((i = fis.read(buf)) != -1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
    }

    public static void makeDir(String name) {
        if (!(new File(name)).exists()) (new File(name)).mkdir();
    }
    
}
