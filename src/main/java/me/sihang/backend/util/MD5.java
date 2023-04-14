package me.sihang.backend.util;

import org.apache.tomcat.jni.File;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

public class MD5 {
    public static String parse(String plainText) {
        byte[] secretBytes = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plainText.getBytes());
            secretBytes = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No MD5 Method");
        }

        String md5code = new BigInteger(1, secretBytes).toString(16);// 16进制数字
        for (; md5code.length() < 32  ; ) {
            md5code = "0" + md5code;
        }
        return md5code;
    }

    public static String getDicomDirMd5String(String dir) {
        byte[] secretBytes = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
                for (Path path : stream) {
                    if (!Files.isDirectory(path) && path.getFileName().toString().contains("dcm")) {
                        FileInputStream in = new FileInputStream(path.toFile());
                        FileChannel ch = in.getChannel();
                        MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, path.toFile().length());
                        md.update(byteBuffer);
                    }
                }
            }
            secretBytes = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No MD5 Method");
        } catch (IOException ex){
            throw new RuntimeException("File io error");
        }
        String md5code = new BigInteger(1, secretBytes).toString(16);// 16进制数字
        for (; md5code.length() < 32  ; ) {
            md5code = "0" + md5code;
        }
        return md5code;
    }

}
