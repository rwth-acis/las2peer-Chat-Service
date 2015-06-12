cd ..
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*;"

java -cp %CLASSPATH% i5.las2peer.testing.L2pNodeLauncher --windows-shell -p 9010 uploadStartupDirectory startService('i5.las2peer.services.chatService.ChatService','chatServicePass') startHttpConnector interactive
pause
