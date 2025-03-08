package vip.lycheer.ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import vip.lycheer.ai.service.LoggingAdvisor;

import java.time.LocalDate;

@RestController
@CrossOrigin
public class OpenAiController {


    private final ChatClient chatClient;

    public OpenAiController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, ChatMemory chatMemory) {

        this.chatClient = chatClientBuilder
                //系统指令
                .defaultSystem("""
                        您是荔枝航空公司的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
                        您正在通过在线聊天系统与客户互动。
                        在提供有关预订或取消预订的信息之前，您必须始终从用户处获取以下信息：预订号、客户姓名。
                        在询问用户之前，请检查消息历史记录以获取此信息。
                        在更改预订之前，您必须确保条款允许这样做。
                        如果更改需要收费，您必须在继续之前征得用户同意。
                        使用提供的功能获取预订详细信息、更改预订和取消预订。
                        如果需要，可以调用相应函数调用完成辅助动作。
                        请讲中文。
                        今天的日期是 {current_date}.
                        """)
                //拦截器
                .defaultAdvisors(
                        //保持会话记忆
                        new PromptChatMemoryAdvisor(chatMemory),
                        //RAG
                        new QuestionAnswerAdvisor(vectorStore, SearchRequest.builder().topK(4).similarityThresholdAll().build()),
                        //记录日志
                        new LoggingAdvisor())
                //调用 Function Call
                .defaultFunctions("cancelBooking", "changeBooking", "getBookingDetails").build();
    }

    // 直接输出
//    @CrossOrigin
//    @GetMapping(value = "/ai/generateStreamAsString", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public String generateStreamAsString(@RequestParam(value = "message", defaultValue = "讲个笑话") String message) {
//        return this.chatClient.prompt().user(message).call().content();
//    }
    //流模式
    @CrossOrigin
    @GetMapping(value = "/ai/generateStreamAsString", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public Flux<String> generateStreamAsString(@RequestParam(value = "message", defaultValue = "讲个笑话") String message) {
        Flux<String> content = this.chatClient.prompt()
                //用户指令
                .user(message)
                //传递参数
                .system(promptSystemSpec -> promptSystemSpec.param("current_date", LocalDate.now().toString()))
                //保持会话
                .advisors(advisorSpec -> advisorSpec.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                //流式输出
                .stream().content();
        return content.concatWith(Flux.just("[complete]"));
    }


}
