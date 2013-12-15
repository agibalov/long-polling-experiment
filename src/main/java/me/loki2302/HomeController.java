package me.loki2302;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
public class HomeController {
    private List<Message> messages = new ArrayList<Message>();
    
    public HomeController() {
        messages.add(new Message(UUID.randomUUID().toString(), "Message one"));
        messages.add(new Message(UUID.randomUUID().toString(), "Message two"));
        messages.add(new Message(UUID.randomUUID().toString(), "Message three"));
    }
    
    @RequestMapping
    public String index() {
        return "index";
    }
        
    @RequestMapping(value = "/messages", method = RequestMethod.GET)
    @ResponseBody
    public List<Message> getMessages() {
        synchronized(messages) {
            return messages;
        }
    }
    
    @RequestMapping(value = "/messages/{lastMessageId}", method = RequestMethod.GET)
    @ResponseBody
    public Callable<List<Message>> getMessages(@PathVariable final String lastMessageId) {
        return new Callable<List<Message>>() {
            @Override
            public List<Message> call() throws Exception {
                synchronized(messages) {                
                    Message foundMessage = null;
                    for(Message message : messages) {
                        if(!message.id.equals(lastMessageId)) {
                            continue;
                        }
                        
                        foundMessage = message;
                        break;
                    }
                    
                    if(foundMessage == null) {
                        throw new RuntimeException("No such message");
                    }
                    
                    int lastMessageIndex = messages.indexOf(foundMessage);
                    
                    while(true) {
                        if(lastMessageIndex + 1 == messages.size()) {
                            messages.wait();
                        }
                        
                        List<Message> filteredMessages = messages.subList(lastMessageIndex + 1, messages.size());
                        if(!filteredMessages.isEmpty()) {
                            return filteredMessages;
                        }                        
                    }
                }
            }
        };        
    }
    
    @RequestMapping(value = "/messages", method = RequestMethod.POST)
    @ResponseBody
    public Object postMessage(@RequestBody Message message) {
        synchronized(messages) {
            message.id = UUID.randomUUID().toString();
            messages.add(message);
            messages.notifyAll();
        }
        return null;
    }
    
    public static class Message {
        public String id;
        public String text;
        
        public Message() {            
        }
        
        public Message(String id, String text) {
            this.id = id;
            this.text = text;
        }
    }
}