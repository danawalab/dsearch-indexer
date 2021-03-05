package com.danawa.fastcatx.indexer;

import com.danawa.fastcatx.indexer.entity.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IndexJobManager {
    private static Logger logger = LoggerFactory.getLogger(IndexJobManager.class);
    private ConcurrentHashMap<UUID, Job> jobs = new ConcurrentHashMap<>();

    public Job remove(UUID id) {
        Job job = jobs.get(id);
        if (job != null && !"RUNNING".equalsIgnoreCase(job.getStatus())) {
            jobs.remove(id);
        } else {
            job = null;
        }
        return job;
    }
    public List<UUID> getIds() {
        List<UUID> ids = new ArrayList<>();
        Iterator<UUID> iterator = jobs.keySet().iterator();
        while (iterator.hasNext()) {
            ids.add(iterator.next());
        }
        return ids;
    }

    public Job stop(UUID id) {
        Job job = jobs.get(id);
        if (job != null && "RUNNING".equalsIgnoreCase(job.getStatus())) {
            job.setStopSignal(true);
        }
        job.setStatus("STOP");
        return job;
    }

    public Job status(UUID id) {
        return jobs.get(id);
    }

    public Job start(String action, Map<String, Object> payload) {
        UUID id = genId();
        Job job = new Job();
        job.setId(id);
        job.setRequest(payload);
        job.setAction(action);
        jobs.put(id, job);
        logger.info("job ID: {}", id.toString());
        if ("FULL_INDEX".equalsIgnoreCase(action)) {
            new Thread(new IndexJobRunner(job)).start();
        } else if ("FETCH_SOURCE".equalsIgnoreCase(action)) {

            String index = (String) payload.get("index");
            String source = (String) payload.get("source");
            //TODO rsyncFile 로 들어온 전체색인중에서 source ready 를 대기하고 있는 indexing job이 있을 것이다.
            // 그 job은 source map을 만들고 대기하며 map이 0이 될때까지 색인작업은 살아있다.
            // 모든 소스가 색인되어 map이 0이 되면 색인작업이 끝난다.
            // IndexJobRunner:335 참조..
            try {
                IndexJobRunner.getIngester(index, source).trigger();
            } catch (IOException e) {
                logger.error("Error while trigger.", e);
            }
        }

        return job;
    }

    private UUID genId() {
        UUID id = UUID.randomUUID();
        while (true) {
            if (jobs.containsKey(id)) {
                id = UUID.randomUUID();
            } else {
                break;
            }
        }
        return id;
    }

}
