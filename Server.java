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
          System.out.println("Studio connected: " + studio);

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
    out.writeInt(3);
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

      int color = sketchData.get(i);
      int x = sketchData.get(i + 1);
      int y = sketchData.get(i + 2);

      out.writeInt(color); // color
      out.writeInt(x); // x
      out.writeInt(y); // y

      System.out.printf("SENDINNNNGGG %d @(%d, %d)\n", color, x, y);
    }
    out.flush();
  }

  private void serve(Socket clientSocket, int studio) throws IOException {
    DataInputStream in = new DataInputStream(clientSocket.getInputStream());

    while (true) {
      int type = in.readInt(); // message type

      switch (type) {
        case 0:
          // text message
          // TODO: forwardTextMessage
          forwardTextMessage(in, studio);
          break;
        case 1: // drawing message
          forwardDrawingMessage(in, studio);
          break;
        case 2:
          forwardBucketMessage(in, studio);
          break;
        case 3:
          forwardSketchData(in, studio);
          break;
        default:
          System.out.println("Unknown message type");
      }
    }
  }

  private void forwardTextMessage(DataInputStream in, int studio)
    throws IOException {
    byte[] buffer = new byte[1024];
    // read the msg into the buffer
    // changed the type -> text msg -> 0
    int len = in.readInt();
    in.read(buffer, 0, len);
    String message = new String(buffer, 0, len);
    System.out.println(message);

    // Get the list of clients for this studio
    List<Socket> clients = studioClients.get(studio);

    System.out.println("Forwarding to " + clients.size() + " clients");

    synchronized (clients) {
      Iterator<Socket> iterator = clients.iterator();
      while (iterator.hasNext()) {
        Socket s = iterator.next();
        if (s.isClosed()) {
          iterator.remove();
          continue;
        }
        try {
          DataOutputStream out = new DataOutputStream(s.getOutputStream());
          out.writeInt(0); // message type
          out.writeInt(len);
          out.write(buffer, 0, len);
          out.flush();
        } catch (IOException ex) {
          System.out.println("Client already disconnected");
          iterator.remove();
        }
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
    List<Socket> clients = studioClients.get(studio);

    System.out.println("Forwarding to " + clients.size() + " clients");

    synchronized (clients) {
      Iterator<Socket> iterator = clients.iterator();
      while (iterator.hasNext()) {
        Socket s = iterator.next();
        if (s.isClosed()) {
          iterator.remove();
          continue;
        }
        try {
          DataOutputStream out = new DataOutputStream(s.getOutputStream());
          out.writeInt(1); // message type
          out.writeInt(color);
          out.writeInt(x);
          out.writeInt(y);
          out.flush();
        } catch (IOException e) {
          e.printStackTrace();
          iterator.remove();
        }
      }
    }
  }

  private void forwardBucketMessage(DataInputStream in, int studio)
    throws IOException {
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
      studios
        .computeIfAbsent(studio, k -> new ArrayList<>())
        .addAll(Arrays.asList(color, x, y));
      System.out.println("Received Bucket message: " + x + " " + y);
    }

    // Get the list of clients for this studio
    List<Socket> clients = studioClients.get(studio);

    System.out.println("Forwarding to " + clients.size() + " clients");

    synchronized (clients) {
      Iterator<Socket> iterator = clients.iterator();
      while (iterator.hasNext()) {
        Socket s = iterator.next();
        if (s.isClosed()) {
          iterator.remove();
          continue;
        }
        try {
          DataOutputStream out = new DataOutputStream(s.getOutputStream());
          out.writeInt(2); // message type
          out.writeInt(numberOfPixels);
          out.writeInt(color);
          for (int i = 0; i < numberOfPixels; i++) {
            out.writeInt(xCoordinates[i]);
            out.writeInt(yCoordinates[i]);
          }
          out.flush();
        } catch (IOException ex) {
          System.out.println("Client already disconnected");
          iterator.remove();
        }
      }
    }
  }

  private void forwardSketchData(DataInputStream in, int studio)
    throws IOException {
    System.out.println("Forwarding sketch data");
    List<Integer> sketchData = studios.computeIfAbsent(
      studio,
      k -> new ArrayList<>()
    );
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

    // Get the list of clients for this studio
    List<Socket> clients = studioClients.get(studio);

    System.out.println("Forwarding to " + clients.size() + " clients");

    synchronized (clients) {
      Iterator<Socket> iterator = clients.iterator();
      while (iterator.hasNext()) {
        Socket s = iterator.next();
        if (s.isClosed()) {
          iterator.remove();
          continue;
        }
        try {
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

            System.out.printf(
              "Sending Sketch data: %d @(%d, %d)\n",
              color,
              x,
              y
            );
          }
          out.flush();
        } catch (IOException ex) {
          System.out.println("Client already disconnected");
          iterator.remove();
        }
      }
    }
  }

  public static void main(String[] args) throws IOException {
    new Server();
  }
}
