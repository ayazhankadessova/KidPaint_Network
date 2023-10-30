import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
	ServerSocket serverSocket;
	
	ArrayList<Socket> list = new ArrayList();
	
	
	public Server() throws IOException
	{
		int port =12345;
		serverSocket = new ServerSocket(port);
		
		System.out.println("Listening at port : " + port);
		while(true)
		{
			Socket clientSocket = serverSocket.accept();
			
			Thread t = new Thread (()->{
				
				synchronized(list)
				{
					list.add(clientSocket);
				}
				try {
					serve(clientSocket);
				}
				catch(IOException ex)
				{
					
				}
				synchronized(list)
				{
					list.remove(clientSocket);
				}
				
			});
			t.start();
		}
		
		
		
	}
	
	private void serve(Socket clientSocket) throws IOException
	{
		DataInputStream in = new DataInputStream(clientSocket.getInputStream());
		
		byte[] buffer = new byte[1024];
		while(true)
		{
			int type = in.readInt(); // type represents the message type

			int len = in.readInt();
			in.read(buffer,0,len);
			System.out.println(new String(buffer,0,len));
			synchronized(list)
			{
				for(int i = 0; i<list.size();i++)
				{
					try{
						Socket s = list.get(i);
						DataOutputStream out = new DataOutputStream(s.getOutputStream());
						out.writeInt(type);
						out.writeInt(len);
						out.write(buffer,0,len);
						out.flush();
					} catch(IOException ex)
					{
						System.out.println("Client already disconnected");
					}
					
				}
			}
			
		}
	}
		
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		new Server();

	}

}
