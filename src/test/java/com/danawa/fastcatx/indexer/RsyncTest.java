package com.danawa.fastcatx.indexer;

import com.github.fracpete.rsync4j.RSync;

public class RsyncTest {


    public static void main(String[] args) {
        copy("D:\\indexFile-simple\\V0\\prodExt_0", "D:\\indexFile-simple\\");
        copy("D:\\indexFile-simple\\V1\\prodExt_1", "D:\\indexFile-simple\\");
        copy("D:\\indexFile-simple\\V2\\prodExt_2", "D:\\indexFile-simple\\");
        copy("D:\\indexFile-simple\\V3\\prodExt_3", "D:\\indexFile-simple\\");
        copy("D:\\indexFile-simple\\V4\\prodExt_4", "D:\\indexFile-simple\\");
        copy("D:\\indexFile-simple\\V5\\prodExt_5", "D:\\indexFile-simple\\");
        copy("D:\\indexFile-simple\\V6\\prodExt_6", "D:\\indexFile-simple\\");
        copy("D:\\indexFile-simple\\V7\\prodExt_7", "D:\\indexFile-simple\\");
        copy("D:\\indexFile-simple\\V8\\prodExt_8", "D:\\indexFile-simple\\");
        copy("D:\\indexFile-simple\\V9\\prodExt_9", "D:\\indexFile-simple\\");
        copy("D:\\indexFile-simple\\V10\\prodExt_10", "D:\\indexFile-simple\\");

    }



    public static void copy(String fromPath, String toPath) {
        RSync rsync = new RSync()
                .source(fromPath)
                .destination(toPath)
                .recursive(true)
                .progress(true)
                .archive(true)
                .compress(true)
                .bwlimit("10240")
                .inplace(true);

        try {
            rsync.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
