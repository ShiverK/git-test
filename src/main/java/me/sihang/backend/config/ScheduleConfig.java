package me.sihang.backend.config;

import me.sihang.backend.util.DiskMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduleConfig {

    @Autowired
    private DiskMonitor diskMonitor;

    @Scheduled(fixedDelay = 60000)
    public void updateDiskInfoScheduler(){
        diskMonitor.updateDiskInfo();
    }
}
