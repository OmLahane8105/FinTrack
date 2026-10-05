package com.fintrack.dto;

import java.util.List;

public class AiInsightsResponse {

    private String summary;

    private List<String> insights;

    private List<String> recommendations;

    public AiInsightsResponse() {
    }

    public AiInsightsResponse(
            String summary,
            List<String> insights,
            List<String> recommendations
    ) {
        this.summary = summary;
        this.insights = insights;
        this.recommendations = recommendations;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getInsights() {
        return insights;
    }

    public void setInsights(List<String> insights) {
        this.insights = insights;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(
            List<String> recommendations
    ) {
        this.recommendations = recommendations;
    }
}