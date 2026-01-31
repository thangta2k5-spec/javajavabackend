package com.tathang.example304.controllers;

import com.tathang.example304.dto.ChatRequest;
import com.tathang.example304.dto.ChatResponse;
import com.tathang.example304.security.services.ChatService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/ask")
    public ChatResponse ask(@RequestBody ChatRequest request) {
        String reply = chatService.reply(
                request.getMessage(),
                request.getTableCode());
        return new ChatResponse(reply);
    }
}
