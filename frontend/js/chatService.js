/**
* Chat Service Application Script
* @author Peter de Lange (lange@dbis.rwth-aachen.de)
*/

	//Login Form
var loginFormNode              = document.getElementById("cs_loginform"),
	usernameNode               = document.getElementById("cs_username"),
	passwordNode               = document.getElementById("cs_password"),
	
	//Chatroom Selection Perspective
	chatRoomSelectionNode = document.getElementById("cs_chatRoomSelection"),
	publicChatRoomListNode = document.getElementById("cs_publicChatRoomList"),
	privateChatRoomListNode = document.getElementById("cs_privateChatRoomList"),
	newChatRoomFormNode = document.getElementById("cs_createChatRoomForm"),
	newChatRoomNameNode = document.getElementById("cs_chatRoomName"),
	newChatRoomIsPrivateNode = document.getElementById("cs_isPrivate"),
	
	//MainView
	mainViewNode = document.getElementById("cs_mainView"),
	leftSideNode = document.getElementById("cs_leftSide"),
	rightUpSideNode = document.getElementById("cs_rightSideUp"),
	rightDownSideNode = document.getElementById("cs_rightSideDown"),
	inviteUserSectionNode = document.getElementById("cs_invite_user"),
	userToBeInvitedNode = document.getElementById("cs_user_to_be_invited");
	
	//Message Input
	messageNode = document.getElementById("cs_message");
	
	//Currently active chatroom
	currentChatRoom = "";
	currentChatRoomIsPrivate = false;
	//Refresh variable for update of the "main" chatting perspective
	chatRoomRefresh = 0;
	//Refresh variable for update of the chat room selection perspective
	roomSelectionRefresh = 0;
	//Login Name
	storedUserName = "";
var csLibrary = new CS.ChatService();


/**
* Shows the Login Form
*/
var show_login_form = function(){
	roomSelectionRefresh = window.clearInterval(roomSelectionRefresh);
	$(loginFormNode).show();
	$(mainViewNode).hide();
	$(chatRoomSelectionNode).hide();
};


/**
* Shows the chatroom selection perspective.
*/
var show_chat_room_selection = function(){
	chatRoomRefresh = window.clearInterval(chatRoomRefresh);
	roomSelectionRefresh = self.setInterval(function(){update_selection_view(currentChatRoom)},3000); //3 sec refresh rate
	$(loginFormNode).hide();
	$(mainViewNode).hide();
	$(chatRoomSelectionNode).show();
};


/**
* Shows the main "chatting" perspective.
* Activates the refresh countdown.
*/
var show_main_perspective = function(){
	roomSelectionRefresh = window.clearInterval(roomSelectionRefresh);
	chatRoomRefresh = self.setInterval(function(){update_chat_room(currentChatRoom)},3000); //3 sec refresh rate
	$(loginFormNode).hide();
	$(chatRoomSelectionNode).hide();
	$(mainViewNode).show();
	if(currentChatRoomIsPrivate == true){
		$(inviteUserSectionNode).show();
	}
	else{
		$(inviteUserSectionNode).hide();
	}
	leftSideNode.innerHTML = "Welcome to Chatroom \"" + currentChatRoom + "\"!<br>";
	rightUpSideNode.innerHTML = "";
	rightDownSideNode.innerHTML = "";
};


/**
* Handles submission of the Login Form. Tries to login to LAS2peer and shows the chatroom selection on success.
*/
var login_form_submit = function(){
	var username = usernameNode.value,
		password = passwordNode.value;
	if(username != ""){
		csLibrary.login(username,password,function(){
			show_chat_room_selection();
			storedUserName = usernameNode.value;
		});
	}
};


/**
* Handles submission of the message form. Tries to send a message to the current chat room.
*/
var message_form_submit = function(){
	var message = messageNode.value;
	var filter = /^[a-zA-Z0-9 !?.,]*$/;
	messageNode.value = "sending..";
	var check = message.search("\\[Send private message to ");
	if(message != "" && currentChatRoom!=""){
		if (check == 0){
			var position = message.search("\\]: ");
			var recipient = message.substring(25, position);
			var message = message.substring(position+3);
			if(recipient != storedUserName && filter.test(message)){ //Well..technically this is possible. But...;-)
				csLibrary.sendPrivateMessage(message, currentChatRoom, recipient, function(result){
					messageNode.value = "";
					//Add the private message to frontend (since it will not be received of course..)
					var d = new Date();
					var curr_date = d.getDate();
					var curr_month = d.getMonth();
					curr_month++;
					var curr_year = d.getFullYear();
					var curr_hour = d.getHours();
					var curr_min = d.getMinutes();
					var curr_sec = d.getSeconds();
					
					if(curr_month < 10)
						curr_month = "0"+curr_month;
					if(curr_date < 10)
						curr_date = "0"+curr_date;
					if(curr_hour < 10)
						curr_hour = "0"+curr_hour;
					if(curr_min < 10)
						curr_min = "0"+curr_min;
					if(curr_sec < 10)
						curr_sec = "0"+curr_sec;
					var toAdd = "<br><font color='#FF3333'><i>(to " + recipient + ") ";
						toAdd += curr_date + "/" + curr_month + "/" + curr_year + ", " + curr_hour + ":" + curr_min + ":" + curr_sec;
						toAdd += ": " + message +"</i></font>";
					leftSideNode.innerHTML += toAdd;
				});
			}
			else{
				messageNode.value = "Sorry, only a-z, A-Z, 0-9 and !?., are allowed";
			}
		}
		else if(filter.test(message)){
			csLibrary.sendMessage(message, currentChatRoom, function(result){
				messageNode.value = "";
			});
		}
		else{
			messageNode.value = "Sorry, only a-z, A-Z, 0-9 and !?., are allowed";
		}
	}
};


/**
* Handles submission of the message form. Tries to send a message to the current chat room.
*/
var create_chat_room_form_submit = function(){
	var chatRoomName = newChatRoomNameNode.value;
	var isPrivate = newChatRoomIsPrivateNode.checked;
	var filter = /^[a-zA-Z0-9 ]*$/;
	if(chatRoomName != "" && isPrivate && filter.test(chatRoomName)){
		csLibrary.createPrivateChatRoom(chatRoomName, function(result){
			var check = result.search("was already taken");
			if (check == -1){
				currentChatRoomIsPrivate = true;
				currentChatRoom = chatRoomName;
				show_main_perspective();
			}
		});
	}
	else if(chatRoomName != "" && filter.test(chatRoomName)){
		csLibrary.createChatRoom(chatRoomName, function(result){
			var check = result.search("was already taken");
			if (check == -1){
				currentChatRoomIsPrivate = false;
				currentChatRoom = chatRoomName;
				show_main_perspective();
			}
		});
	}
	if(!filter.test(chatRoomName)){
		newChatRoomNameNode.value = "Sorry, only a-z, A-Z, 0-9 are allowed";
	}
};


/**
* Handles submission of the invite user form. Tries to invite the user with the given login to the current chatroom.
*/
var invite_user_form_submit = function(){
	if(currentChatRoom != "" && currentChatRoomIsPrivate){ //double check;-)
		var userToBeInvited = userToBeInvitedNode.value;
		var filter = /^[a-zA-Z0-9 ]*$/;
		if(filter.test(userToBeInvited)){
			csLibrary.inviteUser(currentChatRoom, userToBeInvited, function(result){
				if(result == "User with login " + userToBeInvited + " invited!"){
					leftSideNode.innerHTML += "<br>" + result;
					$("#cs_leftSide").scrollTop($("#cs_leftSide")[0].scrollHeight);
				}
				else{
					leftSideNode.innerHTML += "<br>" + "Problems with inviting user (check spelling?)";
					$("#cs_leftSide").scrollTop($("#cs_leftSide")[0].scrollHeight);
				}
			});
		}
		else{
			leftSideNode.innerHTML += "<br>" + "Problems with inviting user (check spelling?)";
			$("#cs_leftSide").scrollTop($("#cs_leftSide")[0].scrollHeight);
		}
	}
	userToBeInvitedNode.value = "";
}


/**
* Tries to join a chat room. Calls the main perspective if successful.
*/
var join_chat_room = function(chatRoom, isPrivate){
	csLibrary.addMember(chatRoom, storedUserName, function(result){
		if (result == "User with login " + storedUserName + " added!" || result == "User is already member!"){
			currentChatRoom = chatRoom;
			currentChatRoomIsPrivate = isPrivate;
			show_main_perspective();
		}
	});
};


/**
* Tries to leave chat room. Calls the chat room selection perspective if successful.
*/
var leave_chat_room = function(){
	if(currentChatRoom!=""){
		csLibrary.removeFromChatRoom(currentChatRoom, storedUserName, function(result){
		if (result == "User Agent with login " + storedUserName + " removed!"
				|| result == "User is no member of chatroom!" || "Chatroom " + currentChatRoom + " does not exist!"){ //Workaround for slow LAS2peer shared storage
			currentChatRoom = "";
			chatRoomRefresh = window.clearInterval(chatRoomRefresh);
			newChatRoomNameNode.value = "";
			show_chat_room_selection();
		}
	});
	}
};


/**
* Logs out and shows the Login Form.
*/
var logout = function(){
	csLibrary.logout();
	roomSelectionRefresh = window.clearInterval(roomSelectionRefresh);
	storedUserName = "";
	show_login_form();
};


/**
* Helper function to write the private message tag into the message submit form.
* Called by onclick event of member list.
*/
var add_private_message_tag = function(userName){
	if(userName != storedUserName){
		messageNode.value = "\[Send private message to ";
		messageNode.value += userName;
		messageNode.value += "\]: "
	}
}


/**
* Updates the available chat room list.
*/
var update_selection_view = function(){
	if(newChatRoomNameNode.value == "Sorry, only a-z, A-Z, 0-9 are allowed"){
		newChatRoomNameNode.value = "";
	}
	csLibrary.getPublicChatRooms(function(result){
		if($.isArray(result)){ //To ensure that no error message is processed
			if (result[0] != "No public chatrooms created yet!"){
				publicChatRoomListNode.innerHTML = ""; //Clear "old" content
				for (var i = 0; i < result.length; i++) {
					publicChatRoomListNode.innerHTML +=  "<input type=\"submit\"  style=\"width:70%\" value=\"" + result[i] + "\" onclick=\"javascript:join_chat_room('" + result[i] + "', false)\"/><br>";
				}
			}
			else {
				publicChatRoomListNode.innerHTML = "No public chatrooms created yet!<br>Be the first to create a chatroom!";
			}
		}
		//Same as with the main perspective update, always wait for the callback, even if you don't need the result.
		csLibrary.getPrivateChatRooms(function(result){
			if($.isArray(result)){ //To ensure that no error message is processed
				if (result[0] != "You have no invites!"){
					privateChatRoomListNode.innerHTML = ""; //Clear "old" content
					for (var i = 0; i < result.length; i++) {
						privateChatRoomListNode.innerHTML +=  "<input type=\"submit\"  style=\"width:70%\" value=\"" + result[i] + "\" onclick=\"javascript:join_chat_room('" + result[i] + "', true)\"/><br>";
					}
				}
				else {
					privateChatRoomListNode.innerHTML = "Currently, you have no invitations!";
				}
			}

		});
	});
}


/**
* Fetches new messages and updates the right side of the "chatting" perspective.
*/
var update_chat_room = function(){
	if(messageNode.value == "Sorry, only a-z, A-Z, 0-9 and !?., are allowed"){
		messageNode.value = "";
	}
	if(currentChatRoom != ""){
		//Important, always wait for the last result, otherwise it is "way to fast" for
		//LAS2peer storage facilities..
		csLibrary.updateMessages(currentChatRoom, function(result){
			if($.isArray(result)){ //Ensure no (Ajax Client) error message is processed
				if (result[0] != "You are no member of chatroom " + currentChatRoom + "!" && result[0] !=  "Chatroom " + currentChatRoom + " does not exist!"){
					if (result[0] != "No new messages!"){
						var check = result[0].search("Error receiving message!");
						if(check == -1){
							for (var i = 0; i < result.length; i++) {
								leftSideNode.innerHTML = leftSideNode.innerHTML + "<br>" + result[i];
								$("#cs_leftSide").scrollTop($("#cs_leftSide")[0].scrollHeight);
							}
						}
					}
				}
				else{
					leave_chat_room(); //We were to fast with joining and leaving (or anybody else was)..
					//anyways..lets leave and try again some other time..;-)
					return;
				}
			}
			csLibrary.getChatRoomInfo(currentChatRoom, function(result){
				if($.isArray(result)){ //Ensure no (Ajax Client) error message is processed
					rightUpSideNode.innerHTML ="";
					rightUpSideNode.innerHTML += "<h2 align=\"center\">Chatroom<br>\"" + result[0] + "\"</h2>";
					rightUpSideNode.innerHTML += "<p>Administrator: " + result[1] + "</p>";
					if(result[2]=="public"){
						rightUpSideNode.innerHTML += "<p>This chatroom is public!</p>";
					}
					else{
						rightUpSideNode.innerHTML += "<p>This chatroom is private!</p>";
					}
				}
				csLibrary.getChatRoomMembers(currentChatRoom, function(result){
					if($.isArray(result)){ //Ensure no (Ajax Client) error message is processed
						rightDownSideNode.innerHTML = "<ul>";
						for (var i = 0; i < result.length; i++) {
							rightDownSideNode.innerHTML += "<li onclick=\"javascript:add_private_message_tag('" + result[i] + "')\"/>" + result[i] + "</li>";
						}
						rightDownSideNode.innerHTML += "</ul>";
					}
				});
			});
		});
	}
};


//Show Login Form by default
show_login_form();