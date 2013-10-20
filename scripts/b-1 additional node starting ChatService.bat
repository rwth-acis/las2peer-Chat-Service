cd ..
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*;"

java -cp %CLASSPATH% i5.las2peer.testing.L2pNodeLauncher windows_shell -s 9013 192.168.1.20:9010 startObserver startService('i5.las2peer.services.chatService.ChatService','chatServicePass') interactive
pause