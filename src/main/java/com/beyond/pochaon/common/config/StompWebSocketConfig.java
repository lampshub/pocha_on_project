package com.beyond.pochaon.common.config;


import com.beyond.pochaon.common.auth.StompHandler;
import com.beyond.pochaon.common.auth.WebSocketAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompHandler stompHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Autowired
    public StompWebSocketConfig(StompHandler stompHandler, WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.stompHandler = stompHandler;
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }




    @Override
    @Qualifier("order")
    public void registerStompEndpoints(StompEndpointRegistry registry){
        registry.addEndpoint("/connect")
                .setAllowedOrigins("http://localhost:3002")
                .withSockJS();
    }

    @Override
    @Qualifier("order")
    public void configureMessageBroker(MessageBrokerRegistry registry){
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }


    @Override
    public void configureClientInboundChannel(ChannelRegistration registration){
        registration.interceptors(
                stompHandler,
                webSocketAuthInterceptor
        );
    }
}
