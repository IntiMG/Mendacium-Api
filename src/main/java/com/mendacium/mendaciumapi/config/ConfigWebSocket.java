package com.mendacium.mendaciumapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class ConfigWebSocket implements WebSocketMessageBrokerConfigurer {

    // El cliente Android se conecta a: ws://10.0.2.2:8080/api/ws
    // (el /api viene del context-path; aquí solo se declara /ws)
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /topic = broadcast a toda la sala · /queue = mensaje privado a un jugador
        registry.enableSimpleBroker("/topic", "/queue");
        // El cliente envía mensajes a destinos que empiezan con /app
        registry.setApplicationDestinationPrefixes("/app");
    }
}
