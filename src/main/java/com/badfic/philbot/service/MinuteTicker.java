package com.badfic.philbot.service;

import java.lang.invoke.MethodHandles;
import java.util.List;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MinuteTicker extends BaseService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private List<MinuteTickable> minuteTickables;

    @Scheduled(cron = "0 * * * * ?", zone = "GMT")
    public void masterTick() {
        for (MinuteTickable minuteTickable : minuteTickables) {
            try {
                minuteTickable.tick();
            } catch (Exception e) {
                logger.error("Exception in tickable", e);
                honeybadgerReporter.reportError(e, null, "Exception in tickable: " + minuteTickable.getClass().getName());
            }
        }
    }
}