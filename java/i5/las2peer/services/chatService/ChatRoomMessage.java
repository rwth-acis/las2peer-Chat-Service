package i5.las2peer.services.chatService;

import java.io.Serializable;
import java.util.Date;


/**
 * 
 * <p>Data class that is used by the {@link i5.las2peer.services.chatService.ChatService} to transport
 * messages.<br>
 * It contains the message itself as well as some meta-data that will be used to categorize this message.
 * 
 * @author Peter de Lange
 * 
 */
public class ChatRoomMessage implements Serializable{

	private static final long serialVersionUID = 4158164047266143442L;
	private String content;
	private long sendById;
	private Date timestamp;
	private String inChatRoom;
	private boolean isPrivate; 
	
	
	/**
	 * Constructor for a {@link i5.las2peer.services.chatService.ChatRoomMessage}. Will be called by the 
	 * {@link i5.las2peer.services.chatService.ChatService} before a message is sent.
	 * 
	 * @param content the message content itself
	 * @param sendById id of the user this message was sent from
	 * @param chatRoom the {@link i5.las2peer.services.chatService.ChatRoom} this message is sent from (and to)
	 * @param isPrivate determines, if this message is a private (sent to only one user) or public message.
	 * 
	 */
	public ChatRoomMessage(String content, long sendById, String chatRoom, boolean isPrivate){
		this.content = content;
		this.sendById = sendById;
		this.inChatRoom = chatRoom;
		this.timestamp = new Date();
		this.isPrivate = isPrivate;
	}
	
	
	/**
	 * Gets the id of the user this {@link i5.las2peer.services.chatService.ChatRoomMessage} was sent from.
	 * 
	 * @return The user Id.
	 */
	public long getSendById() {
		return sendById;
	}
	
	
	/**
	 * Gets the content of this {@link i5.las2peer.services.chatService.ChatRoomMessage}.
	 * 
	 * @return A String containing the content.
	 */
	public String getContent() {
		return content;
	}
	
	
	/**
	 * Gets the time this {@link i5.las2peer.services.chatService.ChatRoomMessage} was sent.
	 * 
	 * @return A {@link java.util.Date} containing the time of sending.
	 */
	public Date getTimestamp(){
		return this.timestamp;
	}
	
	
	/**
	 * Gets the name of the {@link i5.las2peer.services.chatService.ChatRoom} this
	 * {@link i5.las2peer.services.chatService.ChatRoomMessage} was sent in.
	 * 
	 * @return A String containing the name of the {@link i5.las2peer.services.chatService.ChatRoom}.
	 */
	public String getInChatRoom(){
		return this.inChatRoom;
	}
	
	/**
	 * Determines, if this {@link i5.las2peer.services.chatService.ChatRoomMessage} is private.
	 * 
	 * @return A boolean value.
	 */
	public boolean isPrivate(){
		return this.isPrivate;
	}
}
