# KidPaint_Network

KidPaint is a paint app for kids. A kid can use the pen or bucket function to draw and paint something on the sketchpad.

## Features: Basic client-server approach

1. A server runs in the same subnet. The server-side program does not require any GUI.
2. The original KidPaint program must be run as a client-side program.
3. When KidPaint (client) has just been launched, it shows a GUI for inputting the username. After inputting the name, the client broadcasts a request to the network using UDP.
4. When the server receives the request, it sends a UDP packet with its IP address and port number back to the client.
5. Once the client receives a reply from the server, the client establishes a TCP connection to the server and downloads the sketch data. The sketch will then be rendered in the sketchpad of the client.
   Page 2
6. The kid does not need to input anything related to the network setting including the IP address and port number of the server.
7. The client sends TCP packets with differential updates to the server if the kid drew on the sketchpad.
8. The client will receive TCP packets with differential updates from the server if other kids (other clients) drew on the sketchpads. Then, the client applies the updates to its sketch.
9. The client sends a TCP package with a message to the server if the kid typed a message in the message field and pressed ENTER.
10. The client will receive a TCP packet with a message from the server if one of the kids typed a message in the message field and pressed ENTER.
11. The message with the sender’s name will be displayed in the chat area immediately.
12. A new button should be added to the client-side program for saving the sketch data into a local file.
13. A new button should be added to the client-side program for loading the sketch data from a local file. The sketch data must be sent to the server, and the sketchpads of all connected clients must be updated then.
14. With this approach, all kids draw on the SAME sketch.
