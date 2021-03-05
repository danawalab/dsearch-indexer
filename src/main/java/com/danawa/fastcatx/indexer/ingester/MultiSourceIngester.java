package com.danawa.fastcatx.indexer.ingester;

import com.danawa.fastcatx.indexer.Ingester;

import java.io.IOException;
import java.util.Map;

public class MultiSourceIngester implements Ingester {
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

    public void add(Object o) {

    }

    @Override
    public void trigger() throws IOException {

    }

    @Override
    public boolean isTriggered() {
        return false;
    }
}
