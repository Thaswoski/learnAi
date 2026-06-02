package com.griya.learn.agent;

import com.griya.learn.service.XfSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class SearchAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(SearchAgent.class);

    private final XfSearchService xfSearchService;

    public SearchAgent(XfSearchService xfSearchService) {
        this.xfSearchService = xfSearchService;
    }

    @Override
    public String getName() {
        return "SearchAgent";
    }

    @Override
    public String getRole() {
        return "讯飞搜索员";
    }

    @Override
    public AgentResult execute(AgentContext context, Consumer<String> onStep) {
        try {
            String query = context.getCourseName();
            String knowledgeGaps = context.getKnowledgeGaps();
            if ((query == null || query.isEmpty()) && (knowledgeGaps == null || knowledgeGaps.isEmpty())) {
                query = context.get("topic") != null ? (String) context.get("topic") : "学习资料";
            }

            String searchQuery = (knowledgeGaps != null && !knowledgeGaps.isEmpty())
                ? knowledgeGaps
                : query;

            if (onStep != null) onStep.accept("正在通过讯飞 ONE SEARCH 全网搜索相关资料...");

            log.info("[SearchAgent] 开始搜索: query={}", searchQuery);

            List<Map<String, String>> results = xfSearchService.search(searchQuery, 8);
            String searchText = xfSearchService.searchToText(searchQuery, 8);

            StringBuilder formatted = new StringBuilder();
            if (!results.isEmpty()) {
                formatted.append("## 📚 全网搜索资源\n\n");
                for (int i = 0; i < results.size(); i++) {
                    Map<String, String> item = results.get(i);
                    String name = item.get("name");
                    String url = item.get("url");
                    formatted.append(i + 1).append(". ").append(name != null ? name : "无标题").append("\n");
                    if (url != null && !url.isEmpty()) {
                        formatted.append("   🔗 ").append(url).append("\n");
                    }
                    formatted.append("\n");
                }
            } else {
                formatted.append("暂未搜索到相关资源\n");
                searchText = "";
            }

            context.put("searchResults", formatted.toString());
            context.put("searchRawText", searchText);
            context.put("searchItemCount", String.valueOf(results.size()));

            int count = results.size();

            if (count < 3) {
                context.emit(AgentEvent.Type.SEARCH_INSUFFICIENT, "SearchAgent",
                    Map.of("count", count, "query", searchQuery));
            }
            context.emit(AgentEvent.Type.SEARCH_COMPLETED, "SearchAgent",
                Map.of("count", count, "query", searchQuery));

            String summary = "搜索完成，找到 " + count + " 条相关资源";
            log.info("[SearchAgent] {}", summary);

            return AgentResult.ok(summary, Map.of(
                "results", (Object) results,
                "count", count,
                "formattedText", formatted.toString()
            ));

        } catch (Exception e) {
            log.error("[SearchAgent] 搜索失败", e);
            return AgentResult.fail("搜索失败: " + e.getMessage());
        }
    }
}
