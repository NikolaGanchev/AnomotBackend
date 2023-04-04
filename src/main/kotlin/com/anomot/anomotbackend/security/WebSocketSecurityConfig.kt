package com.anomot.anomotbackend.security

import com.anomot.anomotbackend.services.ChatService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer

@Configuration
class WebSocketSecurityConfig @Autowired constructor(
        private val chatService: ChatService
) : AbstractSecurityWebSocketMessageBrokerConfigurer() {

    override fun configureInbound(messages: MessageSecurityMetadataSourceRegistry) {
        messages.nullDestMatcher().authenticated()
                .simpSubscribeDestMatchers("/chat/{chatId}")
                    .access("@chatService.canSeeMessagesInChat(authentication,#chatId)")
                .anyMessage().denyAll()
    }
}