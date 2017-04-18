# miniFT
A mini secure file transfer client by Qiao Rui and Wu Jinglian


## How to run client: 

1. Open the client
2. Type in the server ip in the textfield
3. Drag in the file
4. Select confidentiality protocol in "Settings"
5. Press "send"

More help details are available in "help" panel

Dependencies (must be in the same directory with the client) : 

* CA.crt

## How to run server:

$ javac FTServer.java

$ java FTServer

Dependencies (must be in the same directory with server program) : 

* 1001442.crt
* privateServer.der

Note: 
Please don't run server in the same directory as the client
