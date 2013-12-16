package me.loki2302;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

@Controller
@RequestMapping("/")
public class HomeController {
    private List<Message> messages = new ArrayList<Message>();
    private List<PendingRequest> pendingRequests = new ArrayList<PendingRequest>();
    
    public HomeController() {
        messages.add(new Message(UUID.randomUUID().toString(), "Message one"));
        messages.add(new Message(UUID.randomUUID().toString(), "Message two"));
        messages.add(new Message(UUID.randomUUID().toString(), "Message three"));
    }
    
    @RequestMapping
    public String index() {
        return "index.jsp";
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
    public DeferredResult<List<Message>> getMessages(@PathVariable final String lastMessageId) {
        DeferredResult<List<Message>> deferredResult = new DeferredResult<List<Message>>();
        
        List<Message> filteredMessages = getMessagesSince(lastMessageId);
        if(!filteredMessages.isEmpty()) {
            deferredResult.setResult(filteredMessages);
        } else {
            synchronized(pendingRequests) {
                pendingRequests.add(new PendingRequest(lastMessageId, deferredResult));
            }
        }
        
        return deferredResult;
    }
    
    private List<Message> getMessagesSince(String lastMessageId) {
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
            
            if(lastMessageIndex + 1 == messages.size()) {
                return new ArrayList<Message>();
            }
            
            return messages.subList(lastMessageIndex + 1, messages.size());
        }
    }
        
    @RequestMapping(value = "/messages", method = RequestMethod.POST)
    @ResponseBody
    public void postMessage(@RequestBody Message message) {
        synchronized(messages) {
            message.id = UUID.randomUUID().toString();
            messages.add(message);
            
            synchronized(pendingRequests) {
                while(!pendingRequests.isEmpty()) {
                    PendingRequest pendingRequest = pendingRequests.get(0);
                    List<Message> filteredMessages = getMessagesSince(pendingRequest.lastMessageId);
                    pendingRequest.deferredResult.setResult(filteredMessages);
                    pendingRequests.remove(pendingRequest);
                }
            }
        }
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
    
    private static class PendingRequest {
        public String lastMessageId;
        public DeferredResult<List<Message>> deferredResult;
        
        public PendingRequest(String lastMessageId, DeferredResult<List<Message>> deferredResult) {
            this.lastMessageId = lastMessageId;
            this.deferredResult = deferredResult;
        }
    }
}