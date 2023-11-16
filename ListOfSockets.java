import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {

  ServerSocket serverSocket;

  // map of studios and their sketch data
  Map<Integer, List<Integer>> studios = new HashMap<>();

  // map of studios and their clients
  Map<Integer, List<Socket>> studioClients = new HashMap<>();

  public Server() throws IOException {
    int tcpPort = 12345;

    serverSocket = new ServerSocket(tcpPort);

    System.out.println("Listening at port : " + tcpPort);

    while (true) {
      Socket clientSocket = serverSocket.accept();

      Thread t = new Thread(() -> {
        try {
          // Send studio list
          sendStudioList(clientSocket);

          // Receive studio selection
          DataInputStream in = new DataInputStream(
            clientSocket.getInputStream()
          );
          int studio = in.readInt();

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
          studioClients.get(studio).remove(clientSocket);
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
