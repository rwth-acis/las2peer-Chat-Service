package i5.las2peer.services.chatService;

import i5.las2peer.execution.L2pThread;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.security.UserAgent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 * <p>Data class that is used by the {@link i5.las2peer.services.chatService.ChatService} save information
 * on a chatroom environment.<br>
 * It also provides security checks for adding members for example, but the implementation mostly relies 
 * on the service to check these things.
 * 
 * @author Peter de Lange
 * 
 */
public class ChatRoom implements Serializable{
	
	private static final long serialVersionUID = -3247589356093475133L;
	
	private String roomName;
	private long adminId;
	private boolean isPrivate;
	private List<Long> members = new ArrayList<Long>();
	private List<Long> invitedAgents; //Only used for private chatrooms
	
	
	/**
	 * Constructor for a {@link i5.las2peer.services.chatService.ChatRoom}. Will be called by the 
	 * {@link i5.las2peer.services.chatService.ChatService} when a new room is added to the shared storage.
	 * 
	 * @param name the name of the {@link i5.las2peer.services.chatService.ChatRoom}, must be unique
	 * @param isPrivate determines, if this chatroom is private and can only be joined by invitation
	 * @param admin the administrator of this {@link i5.las2peer.services.chatService.ChatRoom}; in the current
	 * implementation, this is the creating user
	 * 
	 */
	public ChatRoom(String name, boolean isPrivate, UserAgent admin){
		
		this.roomName = name;
		this.isPrivate = isPrivate;
		this.adminId = admin.getId();
		if(isPrivate)
			invitedAgents = new ArrayList<Long>();
		members.add(admin.getId());
		
	}
	
	
	//Getter
	/**
	 * Determines, if the given user is a member of this {@link i5.las2peer.services.chatService.ChatRoom}.
	 * 
	 * @param userAgent the user to be checked
	 * 
	 * @return True or False.
	 */
	public boolean isMember(UserAgent userAgent){
		return members.contains(userAgent.getId());
	}
	
	
	/**
	 * Determines, if the given user is invited to this (private)
	 * {@link i5.las2peer.services.chatService.ChatRoom}.
	 * 
	 * @param userAgent the user to be checked
	 * 
	 * @return True or False.
	 */
	public boolean isInvited(UserAgent userAgent) {
		if(!isPrivate())
			return false;
		return invitedAgents.contains(userAgent.getId());
	}
	
	
	/**
	 * Returns a list of invited UserAgents.
	 * 
	 * @return An Array of Id's.
	 */
	public Long[] getInvitedAgentsIdList(){
		if(!isPrivate())
			return null;
		return invitedAgents.toArray(new Long[0]);
	}
	
	
	/**
	 * Returns the administrator id.
	 * 
	 * @return A user Id.
	 */
	public long getAdminId(){
		return this.adminId;
	}
	
	
	/**
	 * Returns the {@link i5.las2peer.services.chatService.ChatRoom} name.
	 * 
	 * @return The {@link i5.las2peer.services.chatService.ChatRoom} name.
	 */
	public String getRoomName(){
		return this.roomName;
	}
	
	
	/**
	 * Returns, if this {@link i5.las2peer.services.chatService.ChatRoom} is private.
	 * 
	 * @return True or false.
	 */
	public boolean isPrivate() {
		return this.isPrivate;
	}
	
	
	/**
	 * Returns a list of Members to this {@link i5.las2peer.services.chatService.ChatRoom}.
	 * 
	 * @return An Array of Id's.
	 */
	public Long[] getMemberIdList(){
		return members.toArray(new Long[0]);
	}
	
	
	/**
	 * Returns a list of Members to this {@link i5.las2peer.services.chatService.ChatRoom}.
	 * 
	 * @return An Array of login names.
	 */
	public String[] getMemberLoginList(){
		Long[] members = getMemberIdList();
		String[] returnArray = new String[members.length];
		
		for(int i = 0; i < returnArray.length; i++) {
			try {
				returnArray[i] = ((UserAgent) getActiveNode().getAgent(members[i])).getLoginName();
			} catch (AgentNotKnownException e) {
				e.printStackTrace();
			}
		}
		return returnArray;
	}
	
	/**
	 * Returns the size of this {@link i5.las2peer.services.chatService.ChatRoom}.
	 * 
	 * @return A number.
	 */
	public int getSize() {
		return members.size();
	}
	
	/**
	 * Returns information of this {@link i5.las2peer.services.chatService.ChatRoom}.
	 * 
	 * @return An array containing the three values name, admin and private-status.
	 */
	public String[] getInfo() {
		String[] info = new String[3];
		info[0] = this.roomName;
		try {
			info[1] = ((UserAgent) getActiveNode().getAgent(this.adminId)).getLoginName();
		} catch (AgentNotKnownException e) {
			//Admin Does not exist..Problem!
			info[1] = "NO ADMIN";
			e.printStackTrace();
		}
		if(isPrivate)
			info[2] = "private";
		else
			info[2] = "public";
		return info;
	}
	
	
	
	//Setter
	/**
	 * Sets a new administrator of this {@link i5.las2peer.services.chatService.ChatRoom}.
	 * 
	 * @param adminId id of the administrator
	 * 
	 */
	public void setAdminId(long adminId){
		this.adminId = adminId;
	}
	
	
	/**
	 * Adds a new member to this {@link i5.las2peer.services.chatService.ChatRoom}.
	 * 
	 * @param userAgent the member
	 * 
	 * @return True, if successful.
	 */
	public boolean addMember(UserAgent userAgent){
		if(!isMember(userAgent)){
			members.add(userAgent.getId());
			if(isPrivate() && userAgent.getId()!=adminId)
				invitedAgents.remove(userAgent.getId());
			return true;
		}
		return false;
	}
	
	
	/**
	 * Invites a new member to this {@link i5.las2peer.services.chatService.ChatRoom}.
	 * 
	 * @param userAgent the member
	 * 
	 * @return True, if successful.
	 */
	public boolean inviteMember(UserAgent userAgent){
		if(isPrivate() && !isMember(userAgent) && !isInvited(userAgent)){
			invitedAgents.add(userAgent.getId());
			return true;
		}
		return false;
	}
	
	
	/**
	 * Removes a member from this {@link i5.las2peer.services.chatService.ChatRoom}.
	 * 
	 * @param userAgent the member
	 * 
	 * @return True, if successful.
	 */
	public boolean removeMember(UserAgent userAgent){
		if(isMember(userAgent)){
			members.remove(userAgent.getId());
			return true;
		}
		return false;
	}
	
	
	//Helper methods to get the current node
	private final L2pThread getL2pThread () {
		Thread t = Thread.currentThread();
		if (! ( t instanceof L2pThread ))
			throw new IllegalStateException ( "Not executed in a L2pThread environment!");
		return (L2pThread) t;
	}
		
	
	private Node getActiveNode() {
		return getL2pThread().getContext().getLocalNode();
	}
}
