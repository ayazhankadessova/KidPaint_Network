# KidPaint_Network

KidPaint is a paint app for kids. A kid can use the pen or bucket function to draw and paint something on the sketchpad.

## Deadlines:

- Init setup: November, 4
- 50%: November, 9
- Finish Basic by : November, 15
- **Deadline: November, 26**

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

## Overview

1. Two types of the message:

- 0 - chat message
- 1 - drawing message

2. FOward drawing message with type and length that we read from buffer

## TODO:

[x] Create draft connection
[x] ask username
[x] make it so when we know the username, we can add it as in the beginning, to know who is messaging
[x] Fix assigning username
[x] BUcket Data
[x] Save Data
[x] Only save the image part

[ ] Load : Get all the pixels, colors etc and send
[ ] Can we make the method reusable?
[ ] How to save in PNG?
[ ] is it ok if users change the udp port ?
[ ] Does not show what 1st connected is painting

<!-- [ ] check username in Server -->

[x] Create Frame to ask for username
[ ] Send bucket data
[ ] Get original drawing -> KEEP ARRAY OF DATA and send to server
[ ] Keep mointoring UDP
[ ] Fix UI

## Issues:

Saves only 40x30, not 50x50

1. How to find a server?

- Use UDP to find where the server is
- send it to caller so that you know who to send
- TCP connection
- bucket info

2. drew smth already, for somebody who connected later. also get smth that was there earlier

- download current stash

## DO not

1. Dont implement undo (override, etc)

## Things implemented:

1. Once the client receives a reply from the server, the client establishes a TCP connection to the server and downloads the sketch data. The sketch will then be rendered in the sketchpad of the client.

- Server side:

added a List<Integer> sketchData to store the sketch data.

In the forwardDrawingMessage method, added code to store the color, x, and y values in sketchData.

added a sendSketchData method that sends the sketch data to a client when it connects (If there is something). This method is called in the thread that handles each client connection.

11. The message with the sender’s name will be displayed in the chat area immediately.

- CLient side:

Store username and add it when sending the text message.

sent bucket data,

i need to receive it in server,
forward it and receive it in client - yes

client side:

- find a list of pixels that i have painted
- loop thru the list and send out the size of the values
- send color
- send type

Server:

- Receive type
- receive values -> loop
- receive color

- go into another loop to get sockets and

## Tools:

- Graphics2D
