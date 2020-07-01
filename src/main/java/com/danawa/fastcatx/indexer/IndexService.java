package com.danawa.fastcatx.indexer;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.enrich.StatsRequest;
import org.elasticsearch.client.enrich.StatsResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class IndexService {

    private static Logger logger = LoggerFactory.getLogger(IndexService.class);

    // 몇건 색인중인지.
    private int count;

    //ES 연결정보.
    private String host;
    private Integer port;
    private String scheme;

    public IndexService(String host, Integer port, String scheme) {
        this.host = host;
        this.port = port;
        this.scheme = scheme;
    }

    public int getCount() {
        return count;
    }

    public boolean existsIndex(String index) throws IOException {
        RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, scheme)));
        GetIndexRequest request = new GetIndexRequest(index);
        return client.indices().exists(request, RequestOptions.DEFAULT);
    }

    public boolean deleteIndex(String index) throws IOException {
        RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, scheme)));
        DeleteIndexRequest request = new DeleteIndexRequest(index);
        AcknowledgedResponse deleteIndexResponse = client.indices().delete(request, RequestOptions.DEFAULT);
        return deleteIndexResponse.isAcknowledged();
    }

    public void index(Ingester ingester, String index, Integer bulkSize, Filter filter) throws IOException {
        RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, scheme)));

        count = 0;
        long start = System.currentTimeMillis();
        BulkRequest request = new BulkRequest();
        long time = System.nanoTime();
        try{
            while (ingester.hasNext()) {
                count++;
                Map<String, Object> record = ingester.next();
                if (filter != null) {
                    record = filter.filter(record);
                }
                //if(record != null) {
                    request.add(new IndexRequest(index).source(record, XContentType.JSON));
                //}

                if (count % bulkSize == 0) {
                    BulkResponse bulkResponse = client.bulk(request, RequestOptions.DEFAULT);
                    logger.debug("bulk! {}", count);
                    request = new BulkRequest();
                }

                if (count % 10000 == 0) {
                    logger.info("{} ROWS FLUSHED! in {}ms", count, (System.nanoTime() - time) / 1000000);
                }
            }

            if (request.estimatedSizeInBytes() > 0) {
                //나머지..
                BulkResponse bulkResponse = client.bulk(request, RequestOptions.DEFAULT);
                checkResponse(bulkResponse);
                logger.debug("Final bulk! {}", count);
            }
        }catch(Exception e) {
            StackTraceElement[] exception = e.getStackTrace();
            for(StackTraceElement element : exception) {
                logger.error("[Exception] : " + element.toString());
            }
        }



        long totalTime = System.currentTimeMillis() - start;
        logger.info("Flush Finished! doc[{}] elapsed[{}m]", count, totalTime / 1000 / 60);
    }


    public void elasticDynamicIndex(Ingester ingester, String index, Filter filter) throws IOException {
        RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, scheme)));

        count = 0;
        long start = System.currentTimeMillis();


        int cnt = 0;
        String[] indexArr = index.split(",");

        long time = System.nanoTime();
        ActionListener<IndexResponse> listener;

        listener = new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse indexResponse) {

            }

            @Override
            public void onFailure(Exception e) {
                logger.error("[DynamicIndex] : " + e.getMessage());
            }
        };


        try{
            while (ingester.hasNext()) {
                count++;

                Map<String, Object> record = ingester.next();
                if (filter != null) {
                    record = filter.filter(record);
                }

                //입력된 인덱스만큼 순차적으로 색인 API 호출
                if(record.size() > 0) {
                    IndexRequest request  = new IndexRequest(indexArr[cnt]).source(record, XContentType.JSON);
                    client.indexAsync(request, RequestOptions.DEFAULT, listener);

                    cnt++;
                    if(cnt == indexArr.length) {
                        cnt = 0;
                    }
                }


                if (count % 10000 == 0) {
                    logger.info("{} DynamicIndex API Call ROWS FLUSHED! in {}ms", count, (System.nanoTime() - time) / 1000000);
                }
            }

        }catch(Exception e) {
            StackTraceElement[] exception = e.getStackTrace();
            for(StackTraceElement element : exception) {
                logger.error("[Exception] : " + element.toString());
            }
        }

        long totalTime = System.currentTimeMillis() - start;
        logger.info("Flush Finished! doc[{}] elapsed[{}m]", count, totalTime / 1000 / 60);
    }


    public void fastcatDynamicIndex(Ingester ingester, String index, Filter filter) throws IOException {

        WebClient webClient = WebClient.create();

        count = 0;

        long start = System.currentTimeMillis();
        // IndexRequest request = new IndexRequest();

        int cnt = 0;

        String[] indexArr = index.split(",");

        long time = System.nanoTime();

        try{
            while (ingester.hasNext()) {
                count++;

                Map<String, Object> record = ingester.next();
                if (filter != null) {
                    record = filter.filter(record);
                }



                //FASTCAT 동적색인 API 호출
                if (record.size() > 0) {
                    String uri = String.format("http://%s:%s/service/index?collectionId=%s",host,port,indexArr[cnt]);
                    Mono<String> result = webClient.post()
                            .uri(uri)
                            .bodyValue(record)
                            .retrieve()
                            .bodyToMono(String.class);

                    result.subscribe( s-> {
                       //logger.info("record");
                    });

                }

                cnt++;
                if(cnt == indexArr.length) {
                    cnt = 0;
                }

                if (count % 10000 == 0) {
                    logger.info("{} DynamicIndex API Call ROWS FLUSHED! in {}ms", count, (System.nanoTime() - time) / 1000000);
                }
            }

        }catch(Exception e) {
            StackTraceElement[] exception = e.getStackTrace();
            for(StackTraceElement element : exception) {
                logger.error("[Exception] : " + element.toString());
            }
        }



        long totalTime = System.currentTimeMillis() - start;
        logger.info("Flush Finished! doc[{}] elapsed[{}m]", count, totalTime / 1000 / 60);
    }

    class Worker implements Callable {
        private BlockingQueue queue;
        private RestHighLevelClient client;
        public Worker(BlockingQueue queue) {
            this.queue = queue;
            this.client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, scheme)));
        }
        @Override
        public Object call() throws Exception {
            while(true) {
                Object o = queue.take();
                if (o instanceof String) {
                    //종료.
                    logger.info("Indexing Worker-{} got {}", Thread.currentThread().getId(), o);
                    break;
                }
                BulkRequest request = (BulkRequest) o;
                BulkResponse bulkResponse = client.bulk(request, RequestOptions.DEFAULT);
//                checkResponse(bulkResponse);
//                logger.debug("bulk! {}", count);
            }
            return null;
        }
    }
    public void indexParallel(Ingester ingester, String index, Integer bulkSize, Filter filter, int threadSize) throws IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadSize);

        BlockingQueue queue= new LinkedBlockingQueue(threadSize * 10);
        //여러 쓰레드가 작업큐를 공유한다.
        List<Future> list = new ArrayList<>();
        for (int i = 0; i < threadSize; i++) {
            Worker w = new Worker(queue);
            list.add(executorService.submit(w));
        }

        count = 0;
        long start = System.currentTimeMillis();
        BulkRequest request = new BulkRequest();
        long time = System.nanoTime();
        try {
            while (ingester.hasNext()) {
                count++;
                Map<String, Object> record = ingester.next();
                if (filter != null) {
                    record = filter.filter(record);
                }

                //if (record != null) {
                    request.add(new IndexRequest(index).source(record, XContentType.JSON));
                //}

                if (count % bulkSize == 0) {
                    queue.put(request);
//                    BulkResponse bulkResponse = client.bulk(request, RequestOptions.DEFAULT);
//                    logger.debug("bulk! {}", count);
                    request = new BulkRequest();
                }

                if (count % 100000 == 0) {
                    logger.info("{} ROWS FLUSHED! in {}ms", count, (System.nanoTime() - time) / 1000000);
                }
            }

            if (request.estimatedSizeInBytes() > 0) {
                //나머지..
                queue.put(request);
//                BulkResponse bulkResponse = client.bulk(request, RequestOptions.DEFAULT);
//                checkResponse(bulkResponse);
                logger.debug("Final bulk! {}", count);
            }


        } catch (InterruptedException e) {
            logger.error("interrupted! ", e);
        } finally {
            try {
                for (int i = 0; i < list.size(); i++) {
                    // 쓰레드 갯수만큼 종료시그널 전달.
                    queue.put("<END>");
                }
            } catch (InterruptedException e) {
                logger.error("", e);
                //ignore
            }

            try {
                for (int i = 0; i < list.size(); i++) {
                    Future f = list.get(i);
                    f.get();
                }
            } catch (Exception e) {
                logger.error("", e);
                //ignore
            }

            executorService.shutdown();
        }

        long totalTime = System.currentTimeMillis() - start;
        logger.info("Flush Finished! doc[{}] elapsed[{}m]", count, totalTime / 1000 / 60);
    }

    private void checkResponse(BulkResponse bulkResponse) {
        if (bulkResponse.hasFailures()) {
            for (BulkItemResponse bulkItemResponse : bulkResponse) {
                if (bulkItemResponse.isFailed()) {
                    BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                    logger.error("Doc index error >> {}", failure);
                }
            }
        }
    }

    public String getStorageSize(String index) throws IOException {
        RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, scheme)));
        StatsRequest statsRequest = new StatsRequest();
        StatsResponse statsResponse =
                client.enrich().stats(statsRequest, RequestOptions.DEFAULT);
        return null;
    }
}