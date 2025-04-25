package com.example.demo.dto;

import java.util.List;

public class TestWebhookRequest {
    private String regNo;
    private List<List<Integer>> outcome;

    // Getters and setters
    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }
    public List<List<Integer>> getOutcome() { return outcome; }
    public void setOutcome(List<List<Integer>> outcome) { this.outcome = outcome; }
}