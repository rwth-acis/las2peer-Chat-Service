package i5.las2peer.services.chatService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i5.las2peer.httpConnector.HttpConnector;
import i5.las2peer.httpConnector.client.Client;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ChatServiceTest {
	private static final String HTTP_ADDRESS = "localhost";
	private static final int HTTP_PORT = 8080;
	
	private LocalNode node;
	private HttpConnector connector;
	private ByteArrayOutputStream logStream;
	private UserAgent adam = null;
	private UserAgent eve = null;
	
	private static final String adamsPass = "adamspass";
	private static final String evesPass = "evespass";
	private static final String testServiceClass = "i5.las2peer.services.chatService.ChatService";
	
	@Before
	public void startServer() throws Exception {
		// start Node
		node = LocalNode.newNode();
		
		adam = MockAgentFactory.getAdam();
		eve  = MockAgentFactory.getEve();
		
		node.storeAgent(adam);
		node.storeAgent(eve);
		
		node.launch();
		
		ServiceAgent testService = ServiceAgent.generateNewAgent(
				testServiceClass, "a pass");
		testService.unlockPrivateKey("a pass");
		
		node.registerReceiver(testService);
		
		// start connector
		
		logStream = new ByteArrayOutputStream();
		connector = new HttpConnector();
		connector.setSocketTimeout(10000);
		connector.setLogStream(new PrintStream(logStream));
		connector.start(node);
	}
	
	@After
	public void shutDownServer() throws Exception {
		connector.stop();
		node.shutDown();
		
		connector = null;
		node = null;
		
		LocalNode.reset();
		
		System.out.println("Connector-Log:");
		System.out.println("--------------");
		
		System.out.println(logStream.toString());
	}

	@Test
	public void testChatRoomCreation() {
		//First, login as Adam and create a chatroom (success), then create it again (fail)
		//Then we create a private chatroom (success) and logout
		
		Client c = new Client(HTTP_ADDRESS, HTTP_PORT, adam.getLoginName(), adamsPass);
		
		try {
			c.connect();
			
			Object result = c.invoke(testServiceClass, "addChatRoom","TestChatRoom", "false");
			assertEquals("Chatroom TestChatRoom was created!", result);
			
			result = c.invoke(testServiceClass, "addChatRoom","TestChatRoom", "false");
			assertEquals("Chatroom name TestChatRoom was already taken!", result);
			
			result = c.invoke(testServiceClass, "addChatRoom","TestChatRoom2", "true");
			assertEquals("Chatroom TestChatRoom2 was created!", result);
			
			c.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
		
		//Login as Eve and try to get the chatroom information of the public room (success)
		//Then we ask for the information of the private room (fail) and try to get a non existing chatroom (fail)
		c = new Client(HTTP_ADDRESS, HTTP_PORT, eve.getLoginName(), evesPass);
		
		try {
		
		Object result = c.invoke(testServiceClass, "getChatRoomInfo","TestChatRoom");
		String[] resultArray = (String[]) result;
		assertEquals(3, resultArray.length);
		assertEquals("TestChatRoom", resultArray[0]);
		assertEquals("adam", resultArray[1]);
		assertEquals("public" , resultArray[2]);
		
		result = c.invoke(testServiceClass, "getChatRoomInfo","TestChatRoom2");
		resultArray = (String[]) result;
		assertEquals(1, resultArray.length);
		assertEquals("Chatroom TestChatRoom2 is private!", resultArray[0]);
		c.disconnect();
		
		result = c.invoke(testServiceClass, "getChatRoomInfo","TestChatRoom3");
		resultArray = (String[]) result;
		assertEquals(1, resultArray.length);
		assertEquals("Chatroom TestChatRoom3 does not exist!", resultArray[0]);
		
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}
	
	@Test
	public void testAddMember() {
		//1. login as Adam and create a chatroom (success)
		//2. we create a private chatroom (success)
		//3. we create another private chatroom (success) 
		//4. and invite eve to it (success)
		Client c = new Client(HTTP_ADDRESS, HTTP_PORT, adam.getLoginName(), adamsPass);
		
		try {
			c.connect();

			Object result = c.invoke(testServiceClass, "addChatRoom","TestChatRoom", "false");
			assertEquals("Chatroom TestChatRoom was created!", result);
			
			result = c.invoke(testServiceClass, "addChatRoom","TestChatRoom2", "true");
			assertEquals("Chatroom TestChatRoom2 was created!", result);
			
			result = c.invoke(testServiceClass, "addChatRoom","TestChatRoom3", "true");
			assertEquals("Chatroom TestChatRoom3 was created!", result);
			
			result = c.invoke(testServiceClass, "inviteUser", "TestChatRoom3", eve.getLoginName());
			assertEquals("User with login eve1st invited!", result);
			
			c.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
		
		//1. login as Eve and try add herself to the public chatroom (success)
		//2. get the information of the chatroom (success)
		//3. we try to add herself to the private room she has not been invited to (fail)
		//4. we try to add herself to the private room she has been invited to (success)
		
		c = new Client(HTTP_ADDRESS, HTTP_PORT, eve.getLoginName(), evesPass);
		
		try {
		
		Object result = c.invoke(testServiceClass, "addMember", "TestChatRoom", eve.getLoginName());
		assertEquals("User with login eve1st added!", result);
		
		result = c.invoke(testServiceClass, "getChatRoomInfo","TestChatRoom");
		String[] resultArray = (String[]) result;
		assertEquals(3, resultArray.length);
		assertEquals("TestChatRoom", resultArray[0]);
		assertEquals("adam", resultArray[1]);
		assertEquals("public" , resultArray[2]);
		
		result = c.invoke(testServiceClass, "addMember", "TestChatRoom2", eve.getLoginName());
		assertEquals("This chatroom is private, you have to be invited!", result);		
		
		result = c.invoke(testServiceClass, "addMember", "TestChatRoom3", eve.getLoginName());
		assertEquals("User with login eve1st added!", result);		
		
		c.disconnect();
		
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}
	
	
	@Test
	public void testGroupMessages() {
		
		Client c = new Client(HTTP_ADDRESS, HTTP_PORT, adam.getLoginName(), adamsPass);
		Client c2 = new Client(HTTP_ADDRESS, HTTP_PORT, eve.getLoginName(), evesPass);
		
		try {
			//Login as both Adam and Eve
			c.connect();
			c2.connect();
			
			//Adam: 
			//1.create a chatroom (success)
			//2. create another chatroom (success)
			//3. add eve to the first chatroom (as eve!)(success)
			//4. add a message to the first chatroom (success)
			//5. add another message to the first chatroom (success)
			//6. fetch first chatroom messages (success)
			Object result = c.invoke(testServiceClass, "addChatRoom","TestChatRoom", "false");
			assertEquals("Chatroom TestChatRoom was created!", result);
			
			result = c.invoke(testServiceClass, "addChatRoom","TestChatRoom2", "false");
			assertEquals("Chatroom TestChatRoom2 was created!", result);
			
			result = c2.invoke(testServiceClass, "addMember", "TestChatRoom", eve.getLoginName());
			assertEquals("User with login eve1st added!", result);
			
			result = c.invoke(testServiceClass, "sendChatRoomMessage", "Hello World!", "TestChatRoom");
			assertEquals("Message sent!", result);
			
			result = c.invoke(testServiceClass, "sendChatRoomMessage", "another message", "TestChatRoom");
			assertEquals("Message sent!", result);
			Thread.sleep(2000); //To ensure that message sending has finished
			
			result = c.invoke(testServiceClass, "getNewChatRoomMessages", "TestChatRoom");
			String[] resultArray = (String[]) result;
			assertEquals(2, resultArray.length);
			assertTrue(resultArray[0].contains("Hello World!"));
			assertTrue(resultArray[1].contains("another message"));
			
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
		
		
		try {
		//Eve:
		//1. try to fetch the messages of the first chatroom (success)
		//2. Fetch the messages again (fail)
		//3. try to fetch a message of the second chatroom (fail)
		Object result = c2.invoke(testServiceClass, "getNewChatRoomMessages", "TestChatRoom");
		String[] resultArray = (String[]) result;
		assertEquals(2, resultArray.length);
		assertTrue(resultArray[0].contains("Hello World!"));
		assertTrue(resultArray[1].contains("another message"));
		
		result = c2.invoke(testServiceClass, "getNewChatRoomMessages", "TestChatRoom");
		resultArray =(String[]) result;
		assertEquals(1, resultArray.length);
		assertEquals("No new messages!", resultArray[0]);
		
		result = c2.invoke(testServiceClass, "getNewChatRoomMessages", "TestChatRoom2");
		resultArray = (String[]) result;
		assertEquals(1, resultArray.length);
		assertEquals("You are no member of chatroom TestChatRoom2!", resultArray[0]);
		
		//and logout both Agents
		c.disconnect();
		c2.disconnect();
		
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}
	
	@Test
	public void testFetchPublicChatrooms() {
		//1. login as Adam and search for public chatrooms, should contain none
		//2. create a public chatroom
		//3. fetch the chatroomlist, should contain one chatroom
		//And Logout
		Client c = new Client(HTTP_ADDRESS, HTTP_PORT, adam.getLoginName(), adamsPass);
		
		try {
			c.connect();
			
			Object result = c.invoke(testServiceClass, "getPublicChatRooms");
			String[] resultArray =(String[]) result;
			assertEquals(resultArray.length, 1);
			assertEquals("No public chatrooms created yet!", resultArray[0]);
			
			result = c.invoke(testServiceClass, "addChatRoom","TestChatRoom", "false");
			assertEquals("Chatroom TestChatRoom was created!", result);
			
			result = c.invoke(testServiceClass, "getPublicChatRooms");
			resultArray = (String[]) result;
			assertEquals(resultArray.length, 1);
			assertEquals("TestChatRoom", resultArray[0]);
			
			c.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}
}
