package com.danawa.fastcatx.indexer.ingester;

import com.danawa.fastcatx.indexer.Ingester;

import java.io.IOException;
import java.util.Map;

public class RsyncFileIngester implements Ingester {
    private boolean isTriggered;

    public RsyncFileIngester(String key, Object value, Map<String, Object> globalMap) {
        this(key, value, globalMap, true);
    }

    public RsyncFileIngester(String key, Object value, Map<String, Object> globalMap, boolean triggerAuto) {
        //바로 시작하지 않는다면 대기..

        if (triggerAuto) {
            isTriggered = true;
        }
    }

    private void runRsync() {


    }

    @Override
    public boolean hasNext() throws IOException {
        return false;
    }

    @Override
    public Map<String, Object> next() throws IOException {
        return null;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void trigger() throws IOException {
        //TODO trigger가 들어와야 rsync 를 시작한다.

        if (!isTriggered) {
            runRsync();
        }

        isTriggered = true;
    }

    @Override
    public boolean isTriggered() {
        return isTriggered;
    }
}
