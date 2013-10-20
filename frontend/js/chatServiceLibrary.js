/**
* Chat Service Frontend Library
* @author Peter de Lange (lange@dbis.rwth-aachen.de)
*/

var CS = (function(CS){
	
	
	/**
	* The Chat Service Library
	* @return {Object}
	*/
	CS.ChatService = function(){
	
		//Private Porperties
		var LAS2PEERHOST = "http://localhost:8080/";
		var LAS2PEERSERVICENAME = "i5.las2peer.services.chatService.ChatService";
		
		var LAS2peerClient;
		var loginCallback = function(){};
		
		
		//Private Methods
			
		/**
		* Creates a new ChatRoom with the values specified in the request.
		* @param chatRoomName The name of the new chatroom
		* @param isPrivate Determines, if other members can join the chatroom
		* @param callback Callback function, called when the result has been retrieved. Has one paramter consisting of the return statement.
		*/
		var createChatRoom = function(chatRoomName, isPrivate, callback){
			if(LAS2peerClient.getStatus() == "loggedIn"){
				var params = [],
					paramChatRoomName = {},
					paramIsPrivate = {};
				
				paramChatRoomName.type = "String";
				paramChatRoomName.value = chatRoomName;
				paramIsPrivate.type = "String";
				paramIsPrivate.value = isPrivate;
				
				params.push(paramChatRoomName,paramIsPrivate);
				
				LAS2peerClient.invoke(LAS2PEERSERVICENAME, "addChatRoom", params, function(status, result) {
					if(status == 200 || status == 204) {
						callback(result.value);
					} else {
						callback("Error! Message: " + result);
					}
				});
			}
		};
		
		
		/**
		* Sends a message to the specified chatroom.
		* @param message The content of the message
		* @param chatRoomName The desired name of the chat room
		* @param callback Callback function, called when the result has been retrieved. Has one parameter consisting of the return statement.
		*/
		var sendMessage = function(message, chatRoomName, callback){
			if(LAS2peerClient.getStatus() == "loggedIn"){
				var params = [],
					paramMessage = {},
					paramChatRoomName = {};
				
				paramMessage.type = "String";
				paramMessage.value = message;
				paramChatRoomName.type = "String";
				paramChatRoomName.value = chatRoomName;
				
				params.push(paramMessage, paramChatRoomName);
				
				LAS2peerClient.invoke(LAS2PEERSERVICENAME, "sendChatRoomMessage", params, function(status, result) {
					if(status == 200 || status == 204) {
						callback(result.value);
					} else {
						callback("Error! Message: " + result);
					}
				});
			}
		};
		
		
		/**
		* Sends a private message to the specified recipient at the specified chatroom.
		* @param message The content of the message
		* @param chatRoomName The desired name of the chat room
		* @param recipient The recipient of the private message (has to be in the same chatroom)
		* @param callback Callback function, called when the result has been retrieved. Has one parameter consisting of the return statement.
		*/
		var sendPrivateMessage = function(message, chatRoomName, recipient, callback){
			if(LAS2peerClient.getStatus() == "loggedIn"){
				var params = [],
					paramMessage = {},
					paramChatRoomName = {};
					paramRecipient = {};
				
				paramMessage.type = "String";
				paramMessage.value = message;
				paramChatRoomName.type = "String";
				paramChatRoomName.value = chatRoomName;
				paramRecipient.type = "String";
				paramRecipient.value = recipient;
				
				params.push(paramMessage, paramChatRoomName, paramRecipient);
				
				LAS2peerClient.invoke(LAS2PEERSERVICENAME, "sendPrivateMessage", params, function(status, result) {
					if(status == 200 || status == 204) {
						callback(result.value);
					} else {
						callback("Error! Message: " + result);
					}
				});
			}
		};
		
		
		/**
		* Updates the chatroom messages with new messages.
		* @param chatRoomName The name of the chat room 
		* @param callback Callback function, called when the result has been retrieved. Has one parameter consisting of the retrieved data.
		*/
		var updateMessages = function(chatRoomName, callback){
			if(LAS2peerClient.getStatus() == "loggedIn"){
				var params = [],
					paramChatRoomName = {};
				
				paramChatRoomName.type = "String";
				paramChatRoomName.value = chatRoomName;
				
				params.push(paramChatRoomName);
				
				LAS2peerClient.invoke(LAS2PEERSERVICENAME, "getNewChatRoomMessages", params, function(status, result) {
					if(status == 200 || status == 204) {
						callback(result.value);
					} else {
						callback("Error! Message: " + result);
					}
				});
			}
		};
		
		
		/**
		* Gets information about the current chatroom.
		* @param chatRoomName The name of the chat room 
		* @param callback Callback function, called when the result has been retrieved. An array of new chatroom information.
		*/
		var getChatRoomInfo = function(chatRoomName, callback){
			if(LAS2peerClient.getStatus() == "loggedIn"){
				var params = [],
					paramChatRoomName = {};
				
				paramChatRoomName.type = "String";
				paramChatRoomName.value = chatRoomName;
				
				params.push(paramChatRoomName);
				
				LAS2peerClient.invoke(LAS2PEERSERVICENAME, "getChatRoomInfo", params, function(status, result) {
					if(status == 200 || status == 204) {
						callback(result.value);
					} else {
						callback("Error! Message: " + result);
					}
				});
			}
		};
		
		
		/**
		* Gets a list of members of the given chatroom.
		* @param chatRoomName The name of the chat room
		* @param callback Callback function, called when the result has been retrieved. An array of usernames.
		*/
		var getChatRoomMembers = function(chatRoomName, callback){
			if(LAS2peerClient.getStatus() == "loggedIn"){
				var params = [],
					paramChatRoomName = {};
				
				paramChatRoomName.type = "String";
				paramChatRoomName.value = chatRoomName;
				
				params.push(paramChatRoomName);
				
				LAS2peerClient.invoke(LAS2PEERSERVICENAME, "getMembersOfChatRoom", params, function(status, result) {
					if(status == 200 || status == 204) {
						callback(result.value);
					} else {
						callback("Error! Message: " + result);
					}
				});
			}
		};
		
		
		/**
		* Returns a list of available chatrooms to join.
		* @param callback Callback function, called when the result has been retrieved. An array of chatroom names.
		*/
		var getPublicChatrooms = function(callback){
			if(LAS2peerClient.getStatus() == "loggedIn"){
				LAS2peerClient.invoke(LAS2PEERSERVICENAME, "getPublicChatRooms", [], function(status, result) {
					if(status == 200 || status == 204) {
						callback(result.value);
					} else {
						callback("Error! Message: " + result);
					}
				});
			}
		};
		
		
		/**
		* Returns a list of chatrooms the user has been invited to.
		* @param callback Callback function, called when the result has been retrieved. An array of chatroom names.
		*/
		var getPrivateChatRooms = function(callback){
			if(LAS2peerClient.getStatus() == "loggedIn"){
				LAS2peerClient.invoke(LAS2PEERSERVICENAME, "getPrivateChatRooms", [], function(status, result) {
					if(status == 200 || status == 204) {
						callback(result.value);
					} else {
						callback("Error! Message: " + result);
					}
				});
			}
		};
		
		
		/**
		* Adds a member to the given chatroom.
		* @param chatRoom The chatroom to join
		* @param userLogin Login name of the user that should join
		* @param callback Callback function, called when the result has been retrieved. Has one paramter consisting of the return statement.
		*/
		var addMember = function(chatRoomName, userLogin, callback){
			if(LAS2peerClient.getStatus() == "loggedIn"){
				var params = [],
					paramChatRoomName = {};
					paramUserLoginName = {};
				
				paramChatRoomName.type = "String";
				paramChatRoomName.value = chatRoomName;
				paramUserLoginName.type = "String";
				paramUserLoginName.value = userLogin;
				
				params.push(paramChatRoomName, paramUserLoginName);
				
				LAS2peerClient.invoke(LAS2PEERSERVICENAME, "addMember", params, function(status, result) {
					if(status == 200 || status == 204) {
						callback(result.value);
					} else {
						callback("Error! Message: " + result);
					}
				});
			}
		};
		
		
		/**
		* Invites a member to the given private chatroom.
		* @param chatRoom The chatroom the user will be invited to
		* @param userLogin Login name of the user that should join
		* @param callback Callback function, called when the result has been retrieved. Has one parameter consisting of the return statement.
		*/
		var inviteUser = function(chatRoomName, userLogin, callback){
			if(LAS2peerClient.getStatus() == "loggedIn"){
				var params = [],
					paramChatRoomName = {};
					paramUserLoginName = {};
				
				paramChatRoomName.type = "String";
				paramChatRoomName.value = chatRoomName;
				paramUserLoginName.type = "String";
				paramUserLoginName.value = userLogin;
				
				params.push(paramChatRoomName, paramUserLoginName);
				
				LAS2peerClient.invoke(LAS2PEERSERVICENAME, "inviteUser", params, function(status, result) {
					if(status == 200 || status == 204) {
						callback(result.value);
					} else {
						callback("Error! Message: " + result);
					}
				});
			}
		};
		
		
		/**
		* Removes a member from the given chatroom.
		* @param chatRoom The chatroom the user will be removed from
		* @param userLogin Login name of the user that will be removed (must not necessarily be the currently logged in user)
		* @param callback Callback function, called when the result has been retrieved. Has one parameter consisting of the return statement.
		*/
		var removeFromChatRoom = function(chatRoomName, userLogin, callback){
			if(LAS2peerClient.getStatus() == "loggedIn"){
				var params = [],
					paramChatRoomName = {};
					paramUserLoginName = {};
				
				paramChatRoomName.type = "String";
				paramChatRoomName.value = chatRoomName;
				paramUserLoginName.type = "String";
				paramUserLoginName.value = userLogin;
				
				params.push(paramChatRoomName, paramUserLoginName);
				
				LAS2peerClient.invoke(LAS2PEERSERVICENAME, "removeMember", params, function(status, result) {
					if(status == 200 || status == 204) {
						callback(result.value);
					} else {
						callback("Error! Message: " + result);
					}
				});
			}
		};
		
		
		//Constructor
		LAS2peerClient = new LasAjaxClient("ChatService", function(statusCode, message) {
			switch(statusCode) {
				case Enums.Feedback.LoginSuccess:
					console.log("Login successful!");
					loginCallback();
					break;
				case Enums.Feedback.LogoutSuccess:
					console.log("Logout successful!");
					break;
				case Enums.Feedback.LoginError:
				case Enums.Feedback.LogoutError:
					console.log("Login error: " + statusCode + ", " + message);
					break;
				case Enums.Feedback.InvocationWorking:
				case Enums.Feedback.InvocationSuccess:
				case Enums.Feedback.Warning:
					break;
				case Enums.Feedback.PingSuccess:
					break;
				default:
					console.log("Unhandled Error: " + statusCode + ", " + message);
					break;
			}
		});
		
		
		//Public Methods
		return {
			
			/**
			* Logs in the passed LAS2peer user.
			* @param username LAS2peer login
			* @param password LAS2peer password
			* @param callback Callback, called when user has been logged in successfully.
			*/
			login: function(username, password, callback){
				if(typeof callback == "function"){
					loginCallback = callback;
				}
				if(LAS2peerClient.getStatus() == "loggedIn"){
					loginCallback();
				} else {
					LAS2peerClient.login(username, password, LAS2PEERHOST, "ChatServiceFrontend");
				}
			},
			
			
			/**
			* Creates a new open chatroom and sets the current user as admin.
			* @param chatRoomName The desired name of the chat room
			* @param callback Callback function, called when the result has been retrieved. Has one parameter consisting of the return statement.
			*/
			createChatRoom: function(chatRoomName, callback){
				createChatRoom(chatRoomName, false, callback);
			},
			
			
			/**
			* Creates a new private chatroom and sets the current user as admin.
			* @param chatRoomName The desired name of the chat room string
			* @param callback Callback function, called when the result has been retrieved. Has one parameter consisting of the return statement.
			*/
			createPrivateChatRoom: function(chatRoomName, callback){
				createChatRoom(chatRoomName, true, callback);
			},
			
			
			/**
			* Sends a message to the specified chatroom.
			* @param message The content of the message
			* @param chatRoomName The desired name of the chat room
			* @param callback Callback function, called when the result has been retrieved. Has one parameter consisting of the return statement.
			*/
			sendMessage: function(message, chatRoomName, callback){
				sendMessage(message, chatRoomName, callback);
			},
			
			/**
			* Sends a private message to the specified recipient at the specified chatroom.
			* @param message The content of the message
			* @param chatRoomName The desired name of the chat room
			* @param recipient The recipient of the private message (has to be in the same chatroom)
			* @param callback Callback function, called when the result has been retrieved. Has one parameter consisting of the return statement.
			*/
			sendPrivateMessage: function(message, chatRoomName, recipient, callback){
				sendPrivateMessage(message, chatRoomName, recipient, callback);
			},
			
			
			/**
			* Updates the chatroom messages with new messages.
			* @param chatRoomName The name of the chat room
			* @param callback Callback function, called when the result has been retrieved. An array of new messages.
			*/
			updateMessages: function(chatRoomName, callback){
				updateMessages(chatRoomName, callback);
			},
			
			
			/**
			* Gets information about the given chatroom.
			* @param chatRoomName The name of the chat room
			* @param callback Callback function, called when the result has been retrieved. An array of chatroom information.
			*/
			getChatRoomInfo: function(chatRoomName, callback){
				getChatRoomInfo(chatRoomName, callback);
			},
			
			
			/**
			* Gets a list of members of the given chatroom.
			* @param chatRoomName The name of the chat room
			* @param callback Callback function, called when the result has been retrieved. An array of usernames.
			*/
			getChatRoomMembers: function(chatRoomName, callback){
				getChatRoomMembers(chatRoomName, callback);
			},
			
			
			/**
			* Returns a list of available chatrooms to join.
			* @param callback Callback function, called when the result has been retrieved. An array of chatroom names.
			*/
			getPublicChatRooms: function(callback){
				getPublicChatrooms(callback);
			},
			
			
			/**
			* Returns a list of chatrooms the user has been invited to.
			* @param callback Callback function, called when the result has been retrieved. An array of chatroom names.
			*/
			getPrivateChatRooms: function(callback){
				getPrivateChatRooms(callback);
			},
			
			
			/**
			* Adds a member to the given chatroom.
			* @param chatRoom The chatroom the user will be invited to
			* @param userLogin Login name of the user that wants to join
			* @param callback Callback function, called when the result has been retrieved. Has one parameter consisting of the return statement.
			*/
			addMember: function(chatRoom, userLogin, callback){
				addMember(chatRoom, userLogin, callback);
			},
			
			
			/**
			* Adds a member to the given private chatroom.
			* @param chatRoom The chatroom to join
			* @param userLogin Login name of the user that wants to join
			* @param callback Callback function, called when the result has been retrieved. Has one parameter consisting of the return statement.
			*/
			inviteUser: function(chatRoom, userLogin, callback){
				inviteUser(chatRoom, userLogin, callback);
			},
			
			
			/**
			* Removes a member from the given chatroom.
			* @param chatRoom The chatroom the user will be removed from
			* @param userLogin Login name of the user that will be removed (must not necessarily be the currently logged in user)
			* @param callback Callback function, called when the result has been retrieved. Has one parameter consisting of the return statement.
			*/
			removeFromChatRoom: function(chatRoom, userLogin, callback){
				removeFromChatRoom(chatRoom, userLogin, callback);
			},
			
			
			/**
			* Logs out the user currently logged in
			*/
			logout: function(){
				LAS2peerClient.logout();
			}
		}

	};
	
	return CS;

})(CS || {});