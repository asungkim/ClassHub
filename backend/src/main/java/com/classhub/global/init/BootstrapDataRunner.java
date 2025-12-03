package com.classhub.global.init;

import java.util.Comparator;
import java.util.List;

import com.classhub.global.init.data.BaseInitData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "dev"})
public class BootstrapDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapDataRunner.class);

    private final List<BaseInitData> initDataList;

    @Value("${bootstrap.data.enabled:true}")
    private boolean enabled;

    @Value("${bootstrap.data.force:false}")
    private boolean force;

    public BootstrapDataRunner(List<BaseInitData> initDataList) {
        this.initDataList = initDataList;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Bootstrap data runner disabled via property");
            return;
        }
        log.info("Running bootstrap data runner, force={}", force);
        initDataList.stream()
                .sorted(Comparator.comparingInt(BaseInitData::getOrder))
                .forEach(initData -> initData.initialize(force));
        log.info("Bootstrap data runner completed");
    }
}
