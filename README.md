LAS2peer-Chat-Service
=====================

The folder "frontend" contains a web frontend to interact with the service (to "chat").  
One can either connect with an "empty" node to a running LAS2peer instance hosting this service
or create an own service instance, in both ways a user can join the common "chatroom"-lobby.  
The folder scripts contains start scripts that use the following ports (make sure they are free to use):  
a: starts a new network at port 9010 without starting an observer (use this if you are just interested in testing the service without the monitoring stuff)  
b-0: 9012 (connects to 9010)  
b-1: 9013 (connects to 9010)  
b-2: 9014 (connects to 9010)  
b-3: 9015 (connects to 9010)  
b-4: 9016 (connects to 9010)  
The "b" scripts have to be executed in the correct order to work properly.  
Build Status: [![Build Status](https://api.travis-ci.org/PedeLa/LAS2peer-Chat-Service.png)](https://travis-ci.org/PedeLa/LAS2peer-Chat-Service)