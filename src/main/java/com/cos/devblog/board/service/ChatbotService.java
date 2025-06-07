package com.cos.devblog.board.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ChatbotService {

    public String callFastApiChatbot(String question) {
        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> request = Map.of("question", question);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        String url = "http://localhost:8000/chatbot"; // FastAPI 서버 주소
        ResponseEntity<Map> response = rest.postForEntity(url, entity, Map.class);
        Map<String, String> body = response.getBody();
        return body.get("answer");
    }

}
