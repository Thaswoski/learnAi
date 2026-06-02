package com.griya.learn.agent;

import java.util.function.Consumer;

public interface Agent {

    String getName();

    String getRole();

    AgentResult execute(AgentContext context, Consumer<String> onStep);
}
