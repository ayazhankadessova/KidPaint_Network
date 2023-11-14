import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

// 1. ForwardDrawingMessage

public class Server {

  ServerSocket serverSocket;
  DatagramSocket udpSocket;

  // list of clients
  ArrayList<Socket> list = new ArrayList<>();

  // list of sketch data
  List<Integer> sketchData = new ArrayList<>();

  public Server() throws IOException {
    int tcpPort = 12345;
    int udpPort = 5555; // choose a port for UDP

    serverSocket = new ServerSocket(tcpPort);
    udpSocket = new DatagramSocket(udpPort);

    System.out.println("Listening at port : " + tcpPort);
    System.out.println("Listening for UDP at port : " + udpPort);

    // Start a thread to listen for UDP packets
    new Thread(() -> {
      byte[] buffer = new byte[1024];
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
      while (true) {
        try {
          udpSocket.receive(packet);
          String message = new String(packet.getData(), 0, packet.getLength());
          System.out.println("Receive message: " + message);
          System.out.println("Someone is asking my IP address");
          byte[] response =
            (
              InetAddress.getLocalHost().getHostAddress() + ":" + tcpPort
            ).getBytes();
          DatagramPacket responsePacket = new DatagramPacket(
            response,
            response.length,
            packet.getAddress(),
            packet.getPort()
          );
          udpSocket.send(responsePacket);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    })
      .start();

    while (true) {
      Socket clientSocket = serverSocket.accept();

      Thread t = new Thread(() -> {
        synchronized (list) {
          list.add(clientSocket);
        }
        try {
          if (!sketchData.isEmpty()) {
            sendSketchData(clientSocket);
          }
          serve(clientSocket);
        } catch (IOException ex) {}
        synchronized (list) {
          list.remove(clientSocket);
        }
      });
      t.start();
    }
  }

  private void sendSketchData(Socket clientSocket) throws IOException {
    System.out.println(
      "Sending sketch data to " + clientSocket.getInetAddress()
    );
    DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
    int numberOfMessages = sketchData.size() / 3;
    out.writeInt(numberOfMessages);
    for (int i = 0; i < numberOfMessages; i++) {
      out.writeInt(1); // message type for drawing message

      int color = sketchData.get(i * 3);
      int x = sketchData.get(i * 3 + 1);
      int y = sketchData.get(i * 3 + 2);

      out.writeInt(color); // color
      out.writeInt(x); // x
      out.writeInt(y); // y

      System.out.printf("SENDINNNNGGG %d @(%d, %d)\n", color, x, y);
    }
    out.flush();
  }

  private void serve(Socket clientSocket) throws IOException {
    DataInputStream in = new DataInputStream(clientSocket.getInputStream());

    // no need for the buffer
    // byte[] buffer = new byte[1024];
    while (true) {
      int type = in.readInt(); // type represents the message type

      // check the type

      switch (type) {
        case 0:
          // text message
          // TODO: forwardTextMessage
          forwardTextMessage(in);
          break;
        case 1:
          // TODO: forwardDrawingMessage
          // drawing message
          forwardDrawingMessage(in);
          break;
        case 2:
          forwardBucketMessage(in);
          break;
        case 3:
          forwardSketchData(in);
        default:
        // others

      }
    }
  }

  private void forwardTextMessage(DataInputStream in) throws IOException {
    byte[] buffer = new byte[1024];
    // read the msg into the buffer
    // changed the type -> text msg -> 0
    int len = in.readInt();
    in.read(buffer, 0, len);
    System.out.println(new String(buffer, 0, len));
    synchronized (list) {
      for (int i = 0; i < list.size(); i++) {
        try {
          Socket s = list.get(i);
          DataOutputStream out = new DataOutputStream(s.getOutputStream());
          out.writeInt(0);
          out.writeInt(len);
          out.write(buffer, 0, len);
          out.flush();
        } catch (IOException ex) {
          System.out.println("Client already disconnected");
        }
      }
    }
  }

  private void forwardBucketMessage(DataInputStream in) throws IOException {
    int numberOfPixels = in.readInt();
    int color = in.readInt();

    int[] xCoordinates = new int[numberOfPixels];
    int[] yCoordinates = new int[numberOfPixels];

    for (int i = 0; i < numberOfPixels; i++) {
      int x = in.readInt();
      int y = in.readInt();
      xCoordinates[i] = x;
      yCoordinates[i] = y;
      // Store the sketch data
      sketchData.add(color);
      sketchData.add(x);
      sketchData.add(y);
      System.out.println("Received Bucket message: " + x + " " + y);
    }
    synchronized (list) {
      for (Socket s : list) {
        DataOutputStream out = new DataOutputStream(s.getOutputStream());
        out.writeInt(2);
        out.writeInt(numberOfPixels);
        out.writeInt(color);
        for (int i = 0; i < numberOfPixels; i++) {
          out.writeInt(xCoordinates[i]);
          out.writeInt(yCoordinates[i]);
        }
        out.flush();
      }
    }
  }

  private void forwardSketchData(DataInputStream in) throws IOException {
    System.out.println("Forwarding sketch data");
    sketchData.clear();
    int numberOfX = in.readInt();
    int numberOfY = in.readInt();
    int numberOfPixels = numberOfX * numberOfY;

    for (int i = 0; i < numberOfPixels; i++) {
      int color = in.readInt();
      int x = in.readInt();
      int y = in.readInt();

      sketchData.add(color);
      sketchData.add(x);
      sketchData.add(y);
      System.out.println("Received Sketch message: " + color + x + " " + y);
    }
    synchronized (list) {
      for (Socket s : list) {
        DataOutputStream out = new DataOutputStream(s.getOutputStream());

        int numberOfMessages = sketchData.size() / 3;
        for (int i = 0; i < numberOfPixels; i++) {
          out.writeInt(1); // message type for drawing message

          int color = sketchData.get(i * 3);
          int x = sketchData.get(i * 3 + 1);
          int y = sketchData.get(i * 3 + 2);

          out.writeInt(color); // color
          out.writeInt(x); // x
          out.writeInt(y); // y

          System.out.printf("Sending Sketch data: %d @(%d, %d)\n", color, x, y);
        }
        out.flush();
      }
    }
  }

  private void forwardDrawingMessage(DataInputStream in) throws IOException {
    int color = in.readInt();
    int x = in.readInt();
    int y = in.readInt();
    System.out.printf("%d @(%d, %d)\n", color, x, y);

    // Store the sketch data
    sketchData.add(color);
    sketchData.add(x);
    sketchData.add(y);

    synchronized (list) {
      for (int i = 0; i < list.size(); i++) {
        Socket s = list.get(i);
        DataOutputStream out = new DataOutputStream(s.getOutputStream());
        out.writeInt(1);
        out.writeInt(color);
        out.writeInt(x);
        out.writeInt(y);
        out.flush();
      }
    }
  }

  public static void main(String[] args) throws IOException {
    // TODO Auto-generated method stub
    new Server();
  }
}
