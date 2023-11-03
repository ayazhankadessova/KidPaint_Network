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

  // list of clients
  ArrayList<Socket> list = new ArrayList<>();

  public Server() throws IOException {
    int port = 12345;
    serverSocket = new ServerSocket(port);

    System.out.println("Listening at port : " + port);
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
    System.out.println("color: " + color + " x: " + x + " y: " + y);
    // TODO: finish from screenshot

    synchronized (list) {
      for (int i = 0; i < list.size(); i++) {
        Socket s = list.get(i);
        DataOutputStream out = new DataOutputStream(s.getOutputStream());
        out.writeInt(1);
        out.writeInt(color);
        out.writeInt(x);
        out.writeInt(y);
        out.flush();
        // catch (IOException ex) {
        //   System.out.println("Client already disconnected");
        // }
      }
    }
  }

  public static void main(String[] args) throws IOException {
    // TODO Auto-generated method stub
    new Server();
  }
}
