package com.onefly.united.oss.service;

import com.onefly.united.oss.web.UnitedSsoClient;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UnitedSsoService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private UnitedSsoClient unitedSsoClient;

    private static String TASK_QUEUE_NAME = "convert-task";

    /**
     * 添加转换队列
     */
    public void addQueueTask(String url) {
        RBlockingQueue<String> queue = redissonClient.getBlockingQueue(TASK_QUEUE_NAME);
        queue.addAsync(url);
    }
}
