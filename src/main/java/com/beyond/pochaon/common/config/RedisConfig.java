package com.beyond.pochaon.common.config;

import com.beyond.pochaon.cart.domain.RedisCartItem;
import com.beyond.pochaon.common.service.SseAlramService;
import com.beyond.pochaon.common.service.SseChatAlarmService;
import com.beyond.pochaon.common.web.WebSubscriber;
import com.beyond.pochaon.present.dto.OwnerEventDto;
import com.beyond.pochaon.present.dto.PresentReceiverDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {


    @Value("${spring.redis.host1}")
    public String host;
    @Value("${spring.redis.port1}")
    public int port;

    @Value("${spring.redis.host2}")
    private String host2;
    @Value("${spring.redis.port2}")
    private int port2;

//    @Value("${spring.redis.host3}")
//    public String host3;
//    @Value("${spring.redis.port3}")
//    public int port3;

    //idempotency
    @Value("${spring.redis.host3}")
    private String idempotencyHost;
    @Value("${spring.redis.port3}")
    private int idempotencyPort;

    @Value("${spring.redis.host4}")
    private String groupHost;
    @Value("${spring.redis.port4}")
    private int groupPort;

    // pubsub
    @Value("${spring.redis.host5}")
    private String emailHost;
    @Value("${spring.redis.port5}")
    private int emailPort;

    @Value("${spring.redis.host6}")
    private String smsHost;
    @Value("${spring.redis.port6}")
    private int smsPort;

    @Value("${spring.redis.host7}")
    private String schedulerHost;
    @Value("${spring.redis.port7}")
    private int schedulerPort;

    @Value("${spring.redis.host8}")
    private String host8;
    @Value("${spring.redis.port8}")
    private int port8;

    @Value("${spring.redis.host9}")
    private String sseAlarmHost;
    @Value("${spring.redis.port9}")
    private int sseAlarmPort;

    @Value("${spring.redis.host10}")
    private String chatHost;
    @Value("${spring.redis.port10}")
    private int chatPort;


    // RefreshToken Redis
    @Bean
    @Qualifier("rtInventoryConnection")
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setDatabase(0);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    @Qualifier("rtInventory")
    public RedisTemplate<String, String> redisTemplate(
            @Qualifier("rtInventoryConnection") RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }


//    // UUID Redis
//    @Bean("uuidConnection")
//    public RedisConnectionFactory uuidConnectionFactory() {
//        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
//        config.setHostName(host3);
//        config.setPort(port3);
//        config.setDatabase(0);
//        return new LettuceConnectionFactory(config);
//    }

//    @Bean("uuid")
//    public RedisTemplate<String, String> uuidRedisTemplate(
//            @Qualifier("uuidConnection") RedisConnectionFactory factory) {
//        RedisTemplate<String, String> t = new RedisTemplate<>();
//        t.setConnectionFactory(factory);
//        t.setKeySerializer(new StringRedisSerializer());
//        t.setValueSerializer(new StringRedisSerializer());
//        return t;
//    }

    // Email 인증 Redis
    @Bean("emailConnection")
    public RedisConnectionFactory emailVerifyRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(emailHost);
        config.setPort(emailPort);
        config.setDatabase(0);
        return new LettuceConnectionFactory(config);
    }

    @Bean("emailVerify")
    public RedisTemplate<String, String> emailVerifyRedisTemplate(
            @Qualifier("emailConnection") RedisConnectionFactory factory) {
        RedisTemplate<String, String> t = new RedisTemplate<>();
        t.setConnectionFactory(factory);
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new StringRedisSerializer());
        return t;
    }


    // SMS 인증 Redis
    @Bean("smsConnection")
    public RedisConnectionFactory smsRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(smsHost);
        config.setPort(smsPort);
        config.setDatabase(0);
        return new LettuceConnectionFactory(config);
    }

    @Bean("smsVerify")
    public RedisTemplate<String, String> smsRedisTemplate(
            @Qualifier("smsConnection") RedisConnectionFactory factory) {
        RedisTemplate<String, String> t = new RedisTemplate<>();
        t.setConnectionFactory(factory);
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new StringRedisSerializer());
        return t;
    }


    // Scheduler Redis

    @Bean
    @Qualifier("schedularRedis")
    public RedisConnectionFactory schedularRedisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(schedulerHost);
        configuration.setPort(schedulerPort);
        configuration.setDatabase(0);
        return new LettuceConnectionFactory(configuration);
    }


//    // Owner ↔ Guest Redis
//    @Bean("ownerGuestConnection")
//    public RedisConnectionFactory ownerGuestConnectionFactory() {
//        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
//        config.setHostName(ownerGuestHost);
//        config.setPort(ownerGuestPort);
//        config.setDatabase(0);
//        return new LettuceConnectionFactory(config);
//    }
//
//    @Bean("ownerGuest")
//    public RedisTemplate<String, String> ownerGuestRedisTemplate(
//            @Qualifier("ownerGuestConnection") RedisConnectionFactory factory) {
//        RedisTemplate<String, String> t = new RedisTemplate<>();
//        t.setConnectionFactory(factory);
//        t.setKeySerializer(new StringRedisSerializer());
//        t.setValueSerializer(new StringRedisSerializer());
//        return t;
//    }


    //    직원호출용
    @Bean
    @Qualifier("ssePubSub")
    public RedisConnectionFactory ssePubSubConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(sseAlarmHost);
        configuration.setPort(sseAlarmPort);
        configuration.setDatabase(0);
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    @Qualifier("ssePubSub")
    public RedisTemplate<String, String> ssePubSubRedisTemplate(@Qualifier("ssePubSub") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    @Qualifier("ssePubSub")
    public RedisMessageListenerContainer redisMessageListenerContainer(@Qualifier("ssePubSub") RedisConnectionFactory redisConnectionFactory, @Qualifier("ssePubSub") MessageListenerAdapter messageListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(messageListenerAdapter, new PatternTopic("staffcall-channel"));
        return container;
    }

    @Bean
    @Qualifier("ssePubSub")
    public MessageListenerAdapter messageListenerAdapter(SseAlramService sseAlramService) {
        return new MessageListenerAdapter(sseAlramService, "onMessage");
    }

    //    채팅알림
    @Bean
    @Qualifier("ssePubSubChat")
    public RedisConnectionFactory ssePubSubChatConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(sseAlarmHost);
        configuration.setPort(sseAlarmPort);
        configuration.setDatabase(1);
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    @Qualifier("ssePubSubChat")
    public RedisTemplate<String, String> ssePubSubChatRedisTemplate(@Qualifier("ssePubSubChat") RedisConnectionFactory ssePubSubChatConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(ssePubSubChatConnectionFactory);
        return redisTemplate;
    }

    @Bean
    @Qualifier("ssePubSubChat")
    public RedisMessageListenerContainer redisMessageAlarmListenerContainer(@Qualifier("ssePubSubChat") RedisConnectionFactory ssePubSubChatConnectionFactory, @Qualifier("ssePubSubChat") MessageListenerAdapter chatListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(ssePubSubChatConnectionFactory);
        container.addMessageListener(chatListenerAdapter, new PatternTopic("chatting-channel"));
        return container;
    }

    @Bean
    @Qualifier("ssePubSubChat")
    public MessageListenerAdapter chatListenerAdapter(SseChatAlarmService sseChatAlarmService) {
        return new MessageListenerAdapter(sseChatAlarmService, "onMessage");
    }


//     rim

    // Cart** Redis 2번
    @Bean
    @Qualifier("cartInventory")
    public RedisConnectionFactory cartRedisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host2);
        configuration.setPort(port2);
        configuration.setDatabase(0);
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    @Qualifier("cartInventory")
    public RedisTemplate<String, RedisCartItem> cartRedisTemplate(@Qualifier("cartInventory") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, RedisCartItem> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        Jackson2JsonRedisSerializer<RedisCartItem> jsonSerializer = new Jackson2JsonRedisSerializer<>(RedisCartItem.class);
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(jsonSerializer);
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }


    //    idempotency redis **redis 3번
    @Bean(name = "idempotencyRedisConnectionFactory")
    public RedisConnectionFactory idempotencyRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(idempotencyHost, idempotencyPort);

        return new LettuceConnectionFactory(config);
    }


    @Bean(name = "idempotencyRedisTemplate")
    @Qualifier("idempotencyRedisConnectionFactory")
    public RedisTemplate<String, String> idempotencyRedisTemplate(@Qualifier("idempotencyRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>(); //key:value 문자열
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }


    // groupId 저장** redis 4번
    @Bean
    public RedisConnectionFactory groupRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(groupHost, groupPort);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    @Qualifier("groupRedisTemplate")
    public RedisTemplate<String, String> groupRedisTemplate(@Qualifier("groupRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }


    // pubsup -guest/owner** redis 8번
    @Bean
    public RedisConnectionFactory pubsubRedisConnectionFactory() {
        return new LettuceConnectionFactory(host8, port8);
    }

    @Bean
    @Qualifier("pubsubRedisTemplate")
    public RedisTemplate<String, OwnerEventDto> pubsubRedisTemplate(@Qualifier("pubsubRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, OwnerEventDto> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        Jackson2JsonRedisSerializer<OwnerEventDto> serializer = new Jackson2JsonRedisSerializer<>(OwnerEventDto.class);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }


    @Bean
    public ChannelTopic orderTopic() {
//        채널 생성
        return new ChannelTopic("t-order");
    }


    //     t-order채널 구독
    @Bean
    public RedisMessageListenerContainer redisContainer(@Qualifier("pubsubRedisConnectionFactory") RedisConnectionFactory connectionFactory, @Qualifier("listenerAdapter") MessageListenerAdapter listenerAdapter, @Qualifier("orderTopic") ChannelTopic orderTopic, @Qualifier("tableAdapter") MessageListenerAdapter tableAdapter, @Qualifier("tableTopic") ChannelTopic tableTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(listenerAdapter, orderTopic);
        container.addMessageListener(tableAdapter, tableTopic);
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(WebSubscriber subscriber) {
        Jackson2JsonRedisSerializer<OwnerEventDto> serializer = new Jackson2JsonRedisSerializer<>(OwnerEventDto.class);
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "onMessage");
        adapter.setSerializer(serializer);
        return adapter;
    }


    //    table전송용
    @Bean
    @Qualifier("tableRedisTemplate")
    public RedisTemplate<String, PresentReceiverDto> pubsubTableRedisTemplate(@Qualifier("pubsubRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, PresentReceiverDto> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        Jackson2JsonRedisSerializer<PresentReceiverDto> serializer = new Jackson2JsonRedisSerializer<>(PresentReceiverDto.class);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }


    //    상대테이블 전송용
    @Bean
    public ChannelTopic tableTopic() {
        return new ChannelTopic("t-present");
    }


    @Bean
    public MessageListenerAdapter tableAdapter(WebSubscriber subscriber) {
        Jackson2JsonRedisSerializer<PresentReceiverDto> serializer = new Jackson2JsonRedisSerializer<>(PresentReceiverDto.class);
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "onTableMessage");
        adapter.setSerializer(serializer);
        return adapter;
    }


    // 채팅 redis
    @Bean
    @Qualifier("chatRedis")
    public RedisConnectionFactory chatRedisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(chatHost);
        configuration.setPort(chatPort);
        configuration.setDatabase(0);
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    @Qualifier("chatRedis")
    public RedisTemplate<String, Object> chatRedisTemplate(
            @Qualifier("chatRedis") RedisConnectionFactory connectionFactory, ObjectMapper objectMapper
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }
}

