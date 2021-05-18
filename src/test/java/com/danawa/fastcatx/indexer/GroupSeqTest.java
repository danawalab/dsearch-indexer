package com.danawa.fastcatx.indexer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GroupSeqTest {
    public static int idx = 0;
    public static void main(String[] args) {

        String case1 = "1";
        String case2 = "1,5";
        String case3 = "1-5";
        String case4 = "1-2,4-6";
        String case5 = "1-3,2-3";


        for (int i = 0; i < 3; i++) {

            new Thread(() -> {
                Set<Integer> groupSeqList = new LinkedHashSet<>();
                if (idx == 0) {
                    groupSeqList = parseGroupSeq(case3);
                    System.out.println(case3 + " >> " + parseGroupSeq(case3).toString());
                } else if (idx == 1) {
                    groupSeqList = parseGroupSeq(case4);
                    System.out.println(case4 + " >> " + parseGroupSeq(case4).toString());
                } else if (idx == 2) {
                    groupSeqList = parseGroupSeq(case5);
                    System.out.println(case5 + " >> " + parseGroupSeq(case5).toString());
                }
                idx ++;
                int threadSize = groupSeqList.size();
                CountDownLatch latch = new CountDownLatch(threadSize);
                for (int j = 0; j < threadSize; j++) {
                    new Thread(() -> {
                        try {
                            System.out.println("start");
                            Thread.sleep(5000);
                            System.out.println("end");
                            latch.countDown();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
                try {
                    // 일단 무한히 기다려본다.
                    latch.await();
                    System.out.println("finished");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        System.out.println("ok");

        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }




    }

    public static Set<Integer> parseGroupSeq(String groupSeq) {
        Set<Integer> list = new LinkedHashSet<>();

        if (groupSeq == null || "".equals(groupSeq)) {
            return list;
        }

        String[] arr1 = groupSeq.split(",");
        System.out.println("arr1: " + arr1);
        for (int i = 0; i < arr1.length; i++) {
            String[] r = arr1[i].split("-");
            if (r.length > 1) {
                list.addAll(getRange(Integer.parseInt(r[0]), Integer.parseInt(r[1])));
            } else {
                list.add(Integer.parseInt(r[0]));
            }
        }
        return list;
    }

    public static List<Integer> getRange(int from, int to) {
        List<Integer> range = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            range.add(i);
        }
        return range;
    }




}
