import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.ArrayList;
import java.util.List;

public class Server {

  ServerSocket serverSocket;
  DatagramSocket udpSocket;

  // map of studios and their sketch data
  Map<Integer, List<Integer>> studios = new HashMap<>();

  // map of studios and their clients
  Map<Integer, List<Socket>> studioClients = new HashMap<>();

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
        int studio = -1; // Initialize studio outside of try block
        try {
          // Send studio list
          sendStudioList(clientSocket);

          // Receive studio selection
          DataInputStream in = new DataInputStream(
            clientSocket.getInputStream()
          );
          studio = in.readInt();
          System.out.println("Studio: " + studio);

          // Add client to studio
          studioClients
            .computeIfAbsent(studio, k -> new ArrayList<>())
            .add(clientSocket);

          // Send sketch data
          if (studios.containsKey(studio)) {
            sendSketchData(clientSocket, studios.get(studio));
          }

          // Serve client
          serve(clientSocket, studio);
        } catch (IOException ex) {
          ex.printStackTrace();
        } finally {
          // Remove client from studio
          if (studio != -1 && studioClients.containsKey(studio)) {
            studioClients.get(studio).remove(clientSocket);
          }
        }
      });
      t.start();
    }
  }

  private void sendStudioList(Socket clientSocket) throws IOException {
    DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
    out.writeInt(studios.size()); // number of studios
    for (int studio : studios.keySet()) {
      out.writeInt(studio);
    }
    out.flush();
  }

  private void sendSketchData(Socket clientSocket, List<Integer> sketchData)
    throws IOException {
    DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
    out.writeInt(sketchData.size() / 3); // number of messages
    for (int i = 0; i < sketchData.size(); i += 3) {
      out.writeInt(1); // message type for drawing message
      out.writeInt(sketchData.get(i)); // color
      out.writeInt(sketchData.get(i + 1)); // x
      out.writeInt(sketchData.get(i + 2)); // y
    }
    out.flush();
  }

  private void serve(Socket clientSocket, int studio) throws IOException {
    DataInputStream in = new DataInputStream(clientSocket.getInputStream());

    while (true) {
      int type = in.readInt(); // message type

      switch (type) {
        case 1: // drawing message
          forwardDrawingMessage(in, studio);
          break;
        // handle other message types...
      }
    }
  }

  private void forwardDrawingMessage(DataInputStream in, int studio)
    throws IOException {
    int color = in.readInt();
    int x = in.readInt();
    int y = in.readInt();

    System.out.printf("RECEIVING %d @(%d, %d)\n", color, x, y);
    // Store the sketch data
    studios
      .computeIfAbsent(studio, k -> new ArrayList<>())
      .addAll(Arrays.asList(color, x, y));

    // Forward the message to all clients in the same studio
    for (Socket s : studioClients.get(studio)) {
      DataOutputStream out = new DataOutputStream(s.getOutputStream());
      out.writeInt(1); // message type
      out.writeInt(color);
      out.writeInt(x);
      out.writeInt(y);
      out.flush();
    }
  }

  public static void main(String[] args) throws IOException {
    new Server();
  }
}
