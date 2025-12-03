package com.classhub.global.init;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;

@Slf4j
public abstract class BaseInitData implements Ordered {

    private final String name;
    private final int order;

    protected BaseInitData(String name, int order) {
        this.name = name;
        this.order = order;
    }

    public String getName() {
        return name;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public final void initialize(boolean force) {
        log.info("Bootstrap [{}] start (force={})", name, force);
        doInitialize(force);
        log.info("Bootstrap [{}] completed", name);
    }

    protected abstract void doInitialize(boolean force);
}
