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

// 1. ForwardDrawingMessage

public class Server {

  ServerSocket serverSocket;
  DatagramSocket udpSocket;

  // list of clients
  ArrayList<Socket> list = new ArrayList<>();

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
          if (message.equals("Is anyone here?")) {
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
          }
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
          serve(clientSocket);
        } catch (IOException ex) {}
        synchronized (list) {
          list.remove(clientSocket);
        }
      });
      t.start();
    }
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

  private void forwardDrawingMessage(DataInputStream in) throws IOException {
    int color = in.readInt();
    int x = in.readInt();
    int y = in.readInt();
    System.out.printf("%d @(%d, %d)\n", color, x, y);

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
