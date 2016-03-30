package i5.las2peer.services.chatService;

import i5.las2peer.api.Service;
import i5.las2peer.communication.Message;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.MessageResultListener;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.UserAgent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


/**
 * 
 * <p>This is a middleware service for LAS2peer that provides methods to run a chat tool in LAS2peer.
 * It is stateless, so there exist no session dependent values and it uses the LAS2peer shared storage
 * for persistence. This makes it possible to run and use the service either at each node that joins a
 * LAS2peer network or to just call the service from a LAS2peer instance that joined a network that
 * contains at least one node hosting this service.<br>
 * This project comes with an additional frontend for the service. If you want to try it locally, please be 
 * aware that you have to disable same origin policy at your browser (for Chrome, this can be achieved by
 * launching from command line with "--disable-web-security"), otherwise the service will 
 * not work. If you want to deploy this service at a remote LAS2peer server, please adjust the server address
 * at the chatServiceLibrary.js file.
 * 
 * <h3>Usage Hints</h3>
 * 
 * <p>Since this service only provides about ten methods with pretty self explaining names, there are only few
 * things to be explained.<br>
 * This service supports public and private chatrooms: While public chatrooms can be found and directly joined
 * by each user, private chatrooms are only visible and joinable after invitation. Since there currently exists
 * a bug in LAS2peer that makes it not possible to search for a user by its login, invitations have to use the
 * user id.<br>
 * If you are new to LAS2peer and only want to start an instance (or ring) hosting this service, you can make
 * use of the start-scripts that come with this project. The first one starts a new network and the second one
 * tries to join the just started network as another node. You will have to change the port and IP to your
 * needs.<br>
 * Since there currently exists no user manager application, you will have to add each user as an XML-file 
 * to the "startup" directory. This directory will be uploaded when you execute the start scripts.
 * To produce agent XML-files, you will have to make use of the LAS2peer ServiceAgentGenerator.
 * At GitHub, there exists a start-script to use this tool in the LAS2peer-Sample-Project of the RWTH-ACIS
 * organization.
 * 
 * @author Peter de Lange
 *
 */
public class ChatService extends Service {
	private final String knownChatRoomsIdentifier = "KNOWN_CHAT_ROOMS";
		
	private MessageResultListener messageResultListener = null;
	
	//private final L2pLogger logger = L2pLogger.getInstance(ChatService.class.getName());
	/**
	 * Constructor: Loads the property file and enables the service monitoring.
	 */
	public ChatService(){
		setFieldValues(); //This sets the values of the property file
	}
	
	
	/**
	 * Adds a {@link i5.las2peer.services.chatService.ChatRoom} to the shared storage.
	 * The chatroom name is unique.
	 * 
	 * @param chatRoomName the name of the {@link i5.las2peer.services.chatService.ChatRoom} to be created
	 * @param isPrivate determines, if the created {@link i5.las2peer.services.chatService.ChatRoom} will
	 * be public or private
	 * 
	 * @return Result of the creation, either that is was created or that its name was already taken.
	 */
	public String addChatRoom(String chatRoomName, String isPrivate){
		ChatRoom chatRoom = findChatRoom(chatRoomName);
		if (chatRoom == null){ //Not found
			chatRoom = new ChatRoom(chatRoomName, Boolean.valueOf(isPrivate), (UserAgent) getContext().getMainAgent());
			if(addChatRoomToNetwork(chatRoom))
				if(addChatRoomNameToNetwork(chatRoomName)){
					L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_8, ""+chatRoomName);
					return "Chatroom " + chatRoomName + " was created!";
				}
			return "Problems during chatroom creation!";
		}
		return "Chatroom name " + chatRoomName + " was already taken!";
	}
	
	
	/**
	 * Returns information on the requested {@link i5.las2peer.services.chatService.ChatRoom}. 
	 * Returns an error message, if the {@link i5.las2peer.services.chatService.ChatRoom} does not
	 * exist or is private (and the requesting agent is not a member).
	 * 
	 * @param chatRoomName name of the {@link i5.las2peer.services.chatService.ChatRoom} the info will be
	 * fetched from
	 * 
	 * @return An array of Strings with size three. Or an array of size one with the error message.
	 */
	public String[] getChatRoomInfo(String chatRoomName){
		String[] returnArray;
		UserAgent requestingAgent = (UserAgent) this.getContext().getMainAgent();
		ChatRoom chatRoom = findChatRoom(chatRoomName);
		if(chatRoom == null){
			returnArray = new String[1];
			returnArray[0] = "Chatroom " + chatRoomName + " does not exist!";
			return returnArray;			
		}
		if(!chatRoom.isPrivate() || chatRoom.isMember(requestingAgent)){
			returnArray = chatRoom.getInfo();
			return returnArray;
		}
		else{
			returnArray = new String[1];
			returnArray[0] = "Chatroom " + chatRoomName + " is private!";
			return returnArray;
		}
	}
	
	
	/**
	 * Sends a {@link i5.las2peer.services.chatService.ChatRoomMessage} to all users (including the sending one)
	 * of the given {@link i5.las2peer.services.chatService.ChatRoom}. Has a build in wait mechanism to prevent
	 * floating the network with new messages.
	 * 
	 * @param message a simple text message
	 * @param chatRoomName the {@link i5.las2peer.services.chatService.ChatRoom} the user is currently in
	 * 
	 * @return Success or error message.
	 */
	public String sendChatRoomMessage(String message, String chatRoomName) {
		UserAgent sendingAgent = (UserAgent) this.getContext().getMainAgent();
		ChatRoom chatRoom = findChatRoom(chatRoomName);
		if(chatRoom == null){
			return "Chatroom " + chatRoomName + " does not exist!";
		}
		if(chatRoom.isMember(sendingAgent)){
			ChatRoomMessage chatRoomMessage = new ChatRoomMessage(message, sendingAgent.getId(), chatRoom.getRoomName(), false);
			try {
				Long[] members = chatRoom.getMemberIdList();
				for(int i = 0; i < members.length; i++){
					Agent receivingAgent = getContext().getLocalNode().getAgent(members[i]);
					Message toSend = new Message(sendingAgent, receivingAgent, chatRoomMessage);
					toSend.setSendingNodeId(getContext().getLocalNode().getNodeId()); //For monitoring, otherwise sending node is not stored (Security/Privacy?)
					if(messageResultListener == null || messageResultListener.isFinished()){
						messageResultListener = new MessageResultListener(2000);
						getContext().getLocalNode().sendMessage(toSend, messageResultListener);
						L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_2, "" + toSend.getId());
						messageResultListener.waitForOneAnswer();
						L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_10, "" + toSend.getId());
					}
					else{
						return "Wait a little, busy!";
					}
				}
				return "Message sent!";
			} catch (Exception e) {
				e.printStackTrace();
				return "Problems with sending! Exception: " + e.toString();
			}
		}
		else{
			return "You are no member of chatroom " + chatRoomName + "!";
		}
	}
	
	
	/**
	 * Sends a private {@link i5.las2peer.services.chatService.ChatRoomMessage}.
	 * Works as the {@link #sendChatRoomMessage(String message, String chatRoomName)} but without sending to everyone in the chatroom.
	 * This means, that sender and recipient have to be in the same {@link i5.las2peer.services.chatService.ChatRoom}
	 * for this. Otherwise, this method will return an error.
	 * 
	 * @param message a simple text message
	 * @param chatRoomName the {@link i5.las2peer.services.chatService.ChatRoom} the user is currently in
	 * 
	 * @return Success or error message.
	 */
	public String sendPrivateMessage(String message, String chatRoomName, String recipientLogin){
		UserAgent sendingAgent = (UserAgent) this.getContext().getMainAgent();
		ChatRoom chatRoom = findChatRoom(chatRoomName);
		if(chatRoom == null){
			return "Chatroom " + chatRoomName + " does not exist!";
		}
		if(chatRoom.isMember(sendingAgent)){
			ChatRoomMessage chatRoomMessage = new ChatRoomMessage(message, sendingAgent.getId(), chatRoom.getRoomName(), true);
			try {
				Long[] members = chatRoom.getMemberIdList();
				for(int i = 0; i < members.length; i++){
					Agent receivingAgent = getContext().getLocalNode().getAgent(members[i]);
					if(((UserAgent) receivingAgent).getLoginName().equals(recipientLogin)){
						Message toSend = new Message(sendingAgent, receivingAgent, chatRoomMessage);
						//For monitoring, otherwise sending node is not stored (Security/Privacy?)
						toSend.setSendingNodeId(getContext().getLocalNode().getNodeId());
						if(messageResultListener == null || messageResultListener.isFinished()){
							messageResultListener = new MessageResultListener(2000);
							getContext().getLocalNode().sendMessage(toSend, messageResultListener);
							L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_1, "" + toSend.getId());
							messageResultListener.waitForOneAnswer(2000);
						}
						else{
							return "Wait a little, busy!";
						}
						return "Message sent!";
					}
				}
				return "User is not in chatroom!";
			} catch (Exception e) {
				e.printStackTrace();
				return "Problems with sending! Exception: " + e.toString();
			}
		}
		else{
			return "You are no member of chatroom " + chatRoomName + "!";
		}
	}
	
	
	/**
	 * Returns an array of member login names of the given {@link i5.las2peer.services.chatService.ChatRoom}.
	 * Either the {@link i5.las2peer.services.chatService.ChatRoom} is private and the calling user is a member,
	 * or the {@link i5.las2peer.services.chatService.ChatRoom} has to be public. Otherwise this method will return
	 * an error message.
	 * 
	 * @param chatRoomName the {@link i5.las2peer.services.chatService.ChatRoom} the information is requested from
	 * 
	 * @return An array of Strings containing all login names. Or an array of size one with the error message.
	 */
	public String[] getMembersOfChatRoom(String chatRoomName){
		String[] returnArray;
		UserAgent currentAgent = (UserAgent) getContext().getMainAgent();
		ChatRoom chatRoom = findChatRoom(chatRoomName);
		if(chatRoom == null){
			returnArray = new String[1];
			returnArray[0] = "Chatroom " + chatRoomName + " does not exist!";
			return returnArray;			
		}
		if(!chatRoom.isPrivate() || chatRoom.isMember(currentAgent)){
			returnArray = chatRoom.getMemberLoginList();
			return returnArray;
		}
		else{
			returnArray = new String[1];
			returnArray[0] = "You are no member of this private chatroom!";
			return returnArray;
		}
	}
	
	
	/**
	 * Adds a member to the given {@link i5.las2peer.services.chatService.ChatRoom}. A user can only add himself.
	 * 
	 * @param chatRoomName the {@link i5.las2peer.services.chatService.ChatRoom} name the user will be added to
	 * @param agentLogin currently, this has to be the login of the calling user
	 * 
	 * @return Success or error message.
	 */
	public String addMember(String chatRoomName, String agentLogin){
		ChatRoom chatRoom = findChatRoom(chatRoomName);
		if(chatRoom == null){
			return "Chatroom " + chatRoomName + " does not exist!";
		}
		UserAgent agentToAdd;
		try {
			agentToAdd = (UserAgent) getContext().getLocalNode().getAgent(getContext().getLocalNode().getAgentIdForLogin(agentLogin));
		} catch (AgentNotKnownException e) {
			return "There exists no agent with login " + agentLogin + "!";
		}
		if(agentToAdd.getId() != getContext().getMainAgent().getId()){
			return "A user can only add himself to a chatroom. If this chatroom is private, use invite instead!";
		}
		if(!chatRoom.isPrivate() || chatRoom.isInvited(agentToAdd)){
			if(chatRoom.addMember(agentToAdd)){
				//Chatroom was empty, add new user as admin
				if(chatRoom.getSize() == 1){
					chatRoom.setAdminId(agentToAdd.getId());
				}
				if(updateChatRoom(chatRoom)){
					L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_8, ""+chatRoomName);
					return "User with login " + agentLogin + " added!";
				}
			}
			else{
				return "User is already member!";
			}
		}
		else{
			return "This chatroom is private, you have to be invited!";
		}
		return "Problems with adding user";
	}
	
	
	/**
	 * Sends out an invitation to the given user. This is only necessary for private chatrooms.
	 * The calling user has to be a member of the given {@link i5.las2peer.services.chatService.ChatRoom}.
	 * 
	 * @param chatRoomName the {@link i5.las2peer.services.chatService.ChatRoom} the invite will be for
	 * @param agentLogin the login name of an agent
	 * 
	 * @return Success or error message.
	 */
	public String inviteUser(String chatRoomName, String agentLogin){
		UserAgent activeAgent = (UserAgent) getContext().getMainAgent();
		ChatRoom chatRoom = findChatRoom(chatRoomName);
		if(chatRoom == null){
			return "Chatroom " + chatRoomName + " does not exist!";
		}
		UserAgent agentToAdd;
		try {
			//Please mind that an UserList update has to happen before any other node can get an agent for its login!
			agentToAdd = (UserAgent) getContext().getLocalNode().getAgent(getContext().getLocalNode().getAgentIdForLogin(agentLogin));
		} catch (AgentNotKnownException e) {
			return "There exists no agent with login " + agentLogin + "!";
		}
		if(!chatRoom.isPrivate()){
			return "This is a public chatroom. No invites necessary!";
		}
		if(chatRoom.isMember(agentToAdd)){
			return "This agent is already a member!";
		}
		if(chatRoom.isMember(activeAgent)){
			if(chatRoom.inviteMember(agentToAdd)){
				if(updateChatRoom(chatRoom))
					return "User with login " + agentLogin + " invited!";
			}
			else{
				return "User is already invited!";
			}
		}
		else{
			return "You are no member of this chatroom!";
		}
		return "Problems with inviting member!";
	}
	
	
	/**
	 * Removes a member from a {@link i5.las2peer.services.chatService.ChatRoom}. Only the admin can remove
	 * other members. All other members can only remove themselves.
	 * 
	 * @param chatRoomName the {@link i5.las2peer.services.chatService.ChatRoom} the user will be removed from
	 * @param agentLogin the login of the user to be removed
	 * 
	 * @return Success or error message.
	 */
	public String removeMember(String chatRoomName, String agentLogin){
		UserAgent currentAgent = (UserAgent) getContext().getMainAgent();
		UserAgent agentToRemove;
		ChatRoom chatRoom = findChatRoom(chatRoomName);
		
		if(chatRoom == null){
			return "Chatroom " + chatRoomName + " does not exist!";
		}
		try {
			agentToRemove = (UserAgent) getContext().getLocalNode().getAgent(getContext().getLocalNode().getAgentIdForLogin(agentLogin));
		} catch (AgentNotKnownException e) {
			return "There exists no agent with login " + agentLogin + "!";
		}
		
		//Own removal always possible
		if(agentToRemove.getId() == currentAgent.getId()){
			if(chatRoom.removeMember(agentToRemove)){
				//If admin was removed and there exists another member
				//(otherwise the next joining member will become admin)
				if(chatRoom.getAdminId() == ((UserAgent) agentToRemove).getId() && chatRoom.getSize() != 0){
					try {
						Agent newAdmin = getContext().getLocalNode().getAgent(chatRoom.getMemberIdList()[0]);
						chatRoom.setAdminId(newAdmin.getId());
					} catch (AgentNotKnownException e) {
						e.printStackTrace();
					}
				}
				if(updateChatRoom(chatRoom)){
					L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_9, ""+chatRoomName);
					return "User Agent with login " + agentLogin + " removed!";
				}
			}
			else{
				return "User is no member of chatroom!";
			}
		}
		//Only admin can remove other members
		else if(chatRoom.getAdminId() == currentAgent.getId()){
			if(chatRoom.removeMember(agentToRemove)){
				if(updateChatRoom(chatRoom)){
					L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_9, ""+chatRoomName);
					return "User Agent with login " + agentLogin + " removed!";
				}
			}
			else{
				return "User is no member of chatroom!";
			}
		}
		else{
			return "You are no admin, only the admin can remove members!";
		}
		return "Problems with removing member!";
	}
	
	
	/**
	 * Returns all (private and public) new messages of a user.
	 * 
	 * @param chatRoomName the {@link i5.las2peer.services.chatService.ChatRoom} the user is currently in
	 * 
	 * @return An array of Strings containing all new messages. Or an array of size one with the error message.
	 */
	public String[] getNewChatRoomMessages(String chatRoomName){
		String[] returnArray;
		UserAgent requestingAgent = (UserAgent) getContext().getMainAgent();
		
		ChatRoom chatRoom = findChatRoom(chatRoomName);
		if(chatRoom == null){
			returnArray = new String[1];
			returnArray[0] = "Chatroom " + chatRoomName + " does not exist!";
			return returnArray;
		}
		if(!chatRoom.isMember(requestingAgent)){
			returnArray = new String[1];
			returnArray[0] = "You are no member of chatroom " + chatRoomName + "!";
			return returnArray;
		}
		 try {
			 Mediator mediator = getContext().getLocalNode().getOrRegisterLocalMediator(requestingAgent);
			 if(mediator.hasMessages()){
				int messageCount = mediator.getNumberOfWaiting();
				List<String> returnMessages = new ArrayList<String>();
				for(int i = 0; i < messageCount; i++){
					Message get = mediator.getNextMessage();
					get.open(getContext().getLocalNode());
					ChatRoomMessage chatRoomMessage = (ChatRoomMessage) get.getContent();
					//This point marks a design decision: Message sending is only
					//allowed in the current chatroom, not across chatrooms.
					//Or in other words: A user can only be in one chatroom at a time.
					if(chatRoomMessage.getInChatRoom().equals(chatRoomName)){
						String returnMessage;
						//If needed, these can be filtered out by the front-end and
						//be replaced with some nicer formatting, of course;-)
						if(chatRoomMessage.isPrivate()){
							returnMessage = "<font color='#FF3333'>";
							returnMessage += "<i>(private)</i> ";
						}
						else
							returnMessage = "<font color='#000033'>";
						returnMessage += new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss").format(chatRoomMessage.getTimestamp());
						returnMessage += " ";
						UserAgent sendingUser = (UserAgent) getContext().getLocalNode().getAgent(chatRoomMessage.getSendById());
						returnMessage += sendingUser.getLoginName();
						returnMessage += ": ";
						returnMessage += chatRoomMessage.getContent();
						returnMessage += "</font>";
						returnMessages.add(returnMessage);
						L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_7, ""+get.getId());
					}
				}
				if(!returnMessages.isEmpty()){
					return returnMessages.toArray(new String[0]);
				}
				else{ //Only received "wrong" messages, meaning not for this chatroom.
					//Should usually be avoided at front-end stage.
					returnArray = new String[1];
					returnArray[0] = "No new messages!";
					return returnArray;
				}
			}
			else{
				returnArray = new String[1];
				returnArray[0] = "No new messages!";
				return returnArray;
			}
		} catch (L2pSecurityException | AgentException e) {
			e.printStackTrace();
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Error receiving message! Exception: " + e.toString());
			returnArray = new String[1];
			returnArray[0] = "Error receiving message! Exception: " + e.toString();
			return returnArray;
		}
	}
	
	
	/**
	 * Returns a list of all public {@link i5.las2peer.services.chatService.ChatRoom}s.
	 * 
	 * @return An array of Strings containing all public {@link i5.las2peer.services.chatService.ChatRoom}s.
	 * Or an array of size one with the message, that no public chatrooms exist.
	 */
	public String[] getPublicChatRooms(){
		ArrayList<String> chatRoomNames = getChatRoomNamesFromNetwork();
		ArrayList<String> publicChatRooms = new ArrayList<String>();
		if(chatRoomNames != null){
			Iterator<String> iterator = chatRoomNames.iterator();
			while(iterator.hasNext()){
				String chatRoomName = iterator.next();
				ChatRoom chatRoom = findChatRoom(chatRoomName);
				if(!chatRoom.isPrivate())
					publicChatRooms.add(chatRoomName);
			}
		}
		else{
			publicChatRooms.add("No public chatrooms created yet!");
		}
		if(publicChatRooms.isEmpty()) //Only private chatrooms..
			publicChatRooms.add("No public chatrooms created yet!");
	    return publicChatRooms.toArray(new String[publicChatRooms.size()]);
	}
	
	
	/**
	 * Returns a list of all private {@link i5.las2peer.services.chatService.ChatRoom}s
	 * the user has been invited to.
	 * 
	 * @return An array of Strings containing all private {@link i5.las2peer.services.chatService.ChatRoom}s.
	 * Or an array of size one with the message, that no private chatrooms exist.
	 */
	public String[] getPrivateChatRooms(){
		UserAgent requestingAgent = (UserAgent) getContext().getMainAgent();
		ArrayList<String> chatRoomNames = getChatRoomNamesFromNetwork();
		ArrayList<String> privateChatRooms = new ArrayList<String>();
		if(chatRoomNames != null){
			Iterator<String> iterator = chatRoomNames.iterator();
			while(iterator.hasNext()){
				String chatRoomName = iterator.next();
				ChatRoom chatRoom = findChatRoom(chatRoomName);
				if(chatRoom.isPrivate() && chatRoom.isInvited(requestingAgent)){
					privateChatRooms.add(chatRoomName);
				}
			}
		}
		else{
			privateChatRooms.add("You have no invites!"); //No chatRooms
		}
		if(privateChatRooms.isEmpty()) //No invites
			privateChatRooms.add("You have no invites!");
	    return privateChatRooms.toArray(new String[privateChatRooms.size()]);
	}
	
	
	private boolean updateChatRoom(ChatRoom chatRoom) {
		try {
			long randomLong = new Random().nextLong(); //To be able to match chatroom search and found pairs
			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_5, ""+randomLong);
			Envelope chatRoomEnvelope = getContext().getStoredObject(ChatRoom[].class, getEnvelopeId (chatRoom.getRoomName()));
			chatRoomEnvelope.open(getAgent());
			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_6, ""+randomLong);
			ChatRoom[] chatRoomArray = new ChatRoom[1];
			chatRoomArray[0] = chatRoom;
			chatRoomEnvelope.updateContent ( chatRoomArray );
			chatRoomEnvelope.addSignature(getAgent());
			chatRoomEnvelope.store();
			L2pLogger.logEvent(Event.SERVICE_MESSAGE, "Updated chatroom " + chatRoom.getRoomName());
			return true;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Error updating chatroom! " + e);
			e.printStackTrace();
			return false;
		}
	}
	
	
	private ChatRoom findChatRoom(String chatRoomName) {
		try {
			long randomLong = new Random().nextLong(); //To be able to match chatroom search and found pairs
			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_5, ""+randomLong);
			Envelope chatRoomEnvelope = getContext().getStoredObject(ChatRoom[].class, getEnvelopeId (chatRoomName));
			chatRoomEnvelope.open(getAgent());
			ChatRoom[] chatRoomArray = chatRoomEnvelope.getContent(ChatRoom[].class);
			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_6, ""+randomLong);
			return chatRoomArray[0];
		} catch ( Exception e ) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, "No chatroom with name " + chatRoomName + " exists!");
			return null;
		}
	}
	
	
	private boolean addChatRoomToNetwork(ChatRoom chatRoom) {
		ChatRoom[] chatRoomArray = new ChatRoom[1];
		chatRoomArray[0] = chatRoom;
		try {
			Envelope chatRoomEnvelope = Envelope.createClassIdEnvelope(new ChatRoom[0], getEnvelopeId(chatRoom.getRoomName()), getAgent());
			chatRoomEnvelope.open(getAgent());
			chatRoomEnvelope.updateContent ( chatRoomArray );
			chatRoomEnvelope.addSignature(getAgent());
			chatRoomEnvelope.store();
			if(chatRoom.isPrivate())
				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_3, chatRoom.getRoomName());
			else
				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_4, chatRoom.getRoomName());
			return true;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Error storing chatroom! " + e);
			e.printStackTrace();
			return false;
		}
	}
	
	
	private ArrayList<String> getChatRoomNamesFromNetwork(){
		ArrayList<String> chatRooms = new ArrayList<String>();
		try {
			Envelope knownChatRoomsEnvelope = getContext().getStoredObject(ArrayList[].class, knownChatRoomsIdentifier);
			knownChatRoomsEnvelope.open(getAgent());
			@SuppressWarnings("unchecked")
			ArrayList<String>[] chatRoomNamesArray = knownChatRoomsEnvelope.getContent(ArrayList[].class);
			chatRooms = chatRoomNamesArray[0];
			return chatRooms;
		} catch (Exception e) {
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Chatroomlist does not yet exist!");
			return null;
		}
	}
	
	
	private boolean addChatRoomNameToNetwork(String chatRoomName) {
		ArrayList<String> chatRooms = getChatRoomNamesFromNetwork();
		//Adding a new list to the network
		if(chatRooms == null){
			chatRooms = new ArrayList<String>();
			chatRooms.add(chatRoomName);
			@SuppressWarnings("unchecked")
			ArrayList<String>[] chatRoomNamesArray = new ArrayList[1];
			chatRoomNamesArray[0] = chatRooms;
			L2pLogger.logEvent(Event.SERVICE_MESSAGE, "Adding new chatroomlist!");
			try {
				Envelope knownChatRoomsEnvelope = Envelope.createClassIdEnvelope(chatRoomNamesArray, knownChatRoomsIdentifier, getAgent());
				knownChatRoomsEnvelope.open(getAgent());
				knownChatRoomsEnvelope.addSignature(getAgent());
				knownChatRoomsEnvelope.store();
				L2pLogger.logEvent(Event.SERVICE_MESSAGE, "Stored new chatroomlist!");
				return true;
			} catch (Exception e) {
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Error storing new chatroomlist! " + e);
				e.printStackTrace();
				return false;
			}
		}
		//Update list with new name
		else{
			try {
				Envelope knownChatRoomsEnvelope = getContext().getStoredObject(ArrayList[].class, knownChatRoomsIdentifier);
				knownChatRoomsEnvelope.open(getAgent());
				@SuppressWarnings("unchecked")
				ArrayList<String>[] chatRoomNamesArray = knownChatRoomsEnvelope.getContent(ArrayList[].class);
				chatRooms = chatRoomNamesArray[0];
				//Not a complete solution for duplicate chatroom creation but better than nothing
				//Removes duplicate entries
				HashSet<String> h = new HashSet<String>(chatRooms);
				chatRooms.clear();
				chatRooms.addAll(h);
				chatRooms.add(chatRoomName);
				chatRoomNamesArray[0] = chatRooms;
				knownChatRoomsEnvelope.updateContent ( chatRoomNamesArray );
				knownChatRoomsEnvelope.addSignature(getAgent());
				knownChatRoomsEnvelope.store();
				L2pLogger.logEvent(Event.SERVICE_MESSAGE, "Updated chatroomlist!");
				return true;
			} catch (Exception e) {
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Error updating chatroomlist! " + e);
				e.printStackTrace();
				return false;
			}
		}
	}
	
	
	private String getEnvelopeId(String roomName) {
		return "ChatService-"+roomName;
	}
}