package com.example.demo.service;

import com.example.demo.dto.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApiService {

    private final RestTemplate restTemplate;

    public ApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void executeOnStartup() {
        GenerateWebhookResponse response = callGenerateWebhook();

        if (response != null) {
            List<List<Integer>> outcome = solveProblem(response);

            System.out.println("Calculated outcome: " + outcome);

            TestWebhookRequest testWebhookRequest = new TestWebhookRequest();
            testWebhookRequest.setRegNo("REG12347");
            testWebhookRequest.setOutcome(outcome);

            sendToWebhookWithRetry(response.getWebhook(), response.getAccessToken(), testWebhookRequest);
        }
    }

    private GenerateWebhookResponse callGenerateWebhook() {
        String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook";

        GenerateWebhookRequest request = new GenerateWebhookRequest();
        request.setName("John Doe");
        request.setRegNo("REG12347");
        request.setEmail("john@example.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<GenerateWebhookRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<GenerateWebhookResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    GenerateWebhookResponse.class
            );
            System.out.println("Data received from /generateWebhook: " + response.getBody());
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<List<Integer>> solveProblem(GenerateWebhookResponse response) {
        Map<String, Object> data = response.getData();
        Object usersWrapper = data.get("users");
    
        List<User> users = new ArrayList<>();
    
        System.out.println("Users Wrapper: " + usersWrapper);
    
        if (usersWrapper instanceof Map) {
            Map<String, Object> innerMap = (Map<String, Object>) usersWrapper;
            Object usersObj = innerMap.get("users");
    
            if (usersObj instanceof List) {
                List<Map<String, Object>> rawUsers = (List<Map<String, Object>>) usersObj;
                users = rawUsers.stream()
                        .map(this::mapToUser)
                        .collect(Collectors.toList());
            } else {
                throw new IllegalStateException("'users' field inside the wrapper is not a list.");
            }
        } else {
            throw new IllegalStateException("'users' is not a map as expected.");
        }
    
        System.out.println("Mapped Users: " + users);
    
        String regNo = "REG12347";
        int lastDigit = Character.getNumericValue(regNo.charAt(regNo.length() - 1));
    
        if (lastDigit % 2 != 0) {
            return findMutualFollowers(users);
        } else {
            int findId = ((Number) data.get("findId")).intValue();
            int n = ((Number) data.get("n")).intValue();
            return findNthLevelFollowers(users, findId, n);
        }
    }
    
    private User mapToUser(Map<String, Object> map) {
        int id = ((Number) map.get("id")).intValue();
        String name = (String) map.get("name");
        List<Integer> follows = ((List<?>) map.get("follows")).stream()
                .map(f -> ((Number) f).intValue())
                .collect(Collectors.toList());
        return new User(id, name, follows);
    }

    public List<List<Integer>> findNthLevelFollowers(List<User> users, int findId, int n) {
   
        User user = users.stream()
                .filter(u -> u.getId() == findId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<List<Integer>> nthLevelFollowers = new ArrayList<>();

        if (n == 1) {
            nthLevelFollowers.add(user.getFollows());
        }
        return nthLevelFollowers;
    }

    public List<List<Integer>> findMutualFollowers(List<User> users) {
        List<List<Integer>> mutualPairs = new ArrayList<>();
        Map<Integer, Set<Integer>> followMap = new HashMap<>();
    
        for (User user : users) {
            followMap.put(user.getId(), new HashSet<>(user.getFollows()));
        }
    
        System.out.println("Follow Map: " + followMap);
    
        for (User user : users) {
            for (Integer followedId : user.getFollows()) {
                if (followMap.containsKey(followedId)) {
                    if (followMap.get(followedId).contains(user.getId())) {
      
                        int min = Math.min(user.getId(), followedId);
                        int max = Math.max(user.getId(), followedId);
                        List<Integer> pair = Arrays.asList(min, max);
    
 
                        if (!mutualPairs.contains(pair)) {
                            mutualPairs.add(pair);
                        }
                    }
                }
            }
        }
        System.out.println("Mutual Pairs: " + mutualPairs);
    
        mutualPairs.sort((a, b) -> {
            if (a.get(0).equals(b.get(0))) {
                return a.get(1) - b.get(1);
            }
            return a.get(0) - b.get(0);
        });
    
        return mutualPairs;
    }
    

    private void sendToWebhookWithRetry(String webhookUrl, String accessToken, TestWebhookRequest request) {
        int maxRetries = 4;
        int attempt = 0;
        boolean success = false;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", accessToken);

        HttpEntity<TestWebhookRequest> entity = new HttpEntity<>(request, headers);

        while (attempt < maxRetries && !success) {
            attempt++;
            try {
                System.out.println("Sending to webhook: " + request); 
                ResponseEntity<String> response = restTemplate.exchange(
                        webhookUrl,
                        HttpMethod.POST,
                        entity,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    success = true;
                    System.out.println("Webhook call succeeded on attempt " + attempt);
                }
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                System.out.println("Webhook call failed on attempt " + attempt + ": " + e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        if (!success) {
            System.out.println("Failed to call webhook after " + maxRetries + " attempts");
        }
    }
}
