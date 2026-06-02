package com.griya.learn.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class AgentEventBus {

    private static final Logger log = LoggerFactory.getLogger(AgentEventBus.class);

    private final Map<AgentEvent.Type, List<Consumer<AgentEvent>>> listeners = new ConcurrentHashMap<>();
    private final List<AgentEvent> history = new ArrayList<>();

    public AgentEventBus on(AgentEvent.Type eventType, Consumer<AgentEvent> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
        return this;
    }

    public AgentEventBus once(AgentEvent.Type eventType, Consumer<AgentEvent> listener) {
        Consumer<AgentEvent> onceWrapper = new Consumer<>() {
            @Override
            public void accept(AgentEvent e) {
                listener.accept(e);
                listeners.getOrDefault(eventType, List.of()).remove(this);
            }
        };
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(onceWrapper);
        return this;
    }

    public AgentEvent emit(AgentEvent event) {
        history.add(event);
        log.info("[EventBus] {} -> {} (listeners={})",
            event.type(), event.sourceAgent(),
            listeners.getOrDefault(event.type(), List.of()).size());

        List<Consumer<AgentEvent>> subs = listeners.get(event.type());
        if (subs != null) {
            for (Consumer<AgentEvent> listener : subs) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    log.error("[EventBus] 监听器异常 event={}", event.type(), e);
                }
            }
        }
        return event;
    }

    public List<AgentEvent> history() {
        return new ArrayList<>(history);
    }

    public List<AgentEvent> historyOf(AgentEvent.Type type) {
        return history.stream().filter(e -> e.type() == type).toList();
    }

    public void clear() {
        listeners.clear();
        history.clear();
    }
}
