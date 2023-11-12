import java.awt.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
// import java.awt.event;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

enum PaintMode {
  Pixel,
  Area,
}

public class UI extends JFrame {

  // DatagramSocket udpSocket;
  Socket socket;
  DataInputStream in;
  DataOutputStream out;

  private JTextField msgField;
  private JTextArea chatArea;
  private JPanel pnlColorPicker;
  private JPanel paintPanel;
  private JToggleButton tglPen;
  private JToggleButton tglBucket;
  private static JLabel password1, label;
  // private static JTextField username;
  private String user;
  // private static JButton button;

  private static UI instance;
  private int selectedColor = -543230; // golden

  DatagramSocket udpSocket = new DatagramSocket(12349);
  // udpSocket.setBroadcast(true);

  int[][] data = new int[50][50]; // pixel color data array
  int blockSize = 16;
  PaintMode paintMode = PaintMode.Pixel;

  /**
   * get the instance of UI. Singleton design pattern.
   *
   * @return
   */
  public static UI getInstance() throws IOException {
    if (instance == null) instance = new UI();

    return instance;
  }

  private void receive(DataInputStream in) throws IOException {
    try {
      while (true) {
        int type = in.readInt();

        switch (type) {
          case 0:
            //receive text
            receiveTextMessage(in);
            break;
          case 1:
            //receive pixel message
            receivePixelMessage(in);
            break;
          case 2:
            receiveBucketMessage(in);
            break;
          default:
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private void receiveTextMessage(DataInputStream in) throws IOException {
    byte[] buffer = new byte[1024];

    int len = in.readInt();
    in.read(buffer, 0, len);

    String msg = new String(buffer, 0, len);
    System.out.println(msg);

    SwingUtilities.invokeLater(() -> {
      chatArea.append(msg + "\n");
    });
  }

  private void receivePixelMessage(DataInputStream in) throws IOException {
    int color = in.readInt();
    int x = in.readInt();
    int y = in.readInt();
    paintPixel(color, x, y);
    //TODO: Update the screen
  }

  private void receiveBucketMessage(DataInputStream in) throws IOException {
    int size = in.readInt();
    int color = in.readInt();

    for (int i = 0; i < size; i++) {
      int x = in.readInt();
      int y = in.readInt();
      paintPixel(color, x, y);
    }
  }

  private void broadcastMessage(String message) throws IOException {
    // Broadcast the provided message
    byte[] request = message.getBytes();
    DatagramPacket requestPacket = new DatagramPacket(
      request,
      request.length,
      InetAddress.getByName("255.255.255.255"),
      5555
    );
    udpSocket.send(requestPacket);

    byte[] buffer = new byte[1024];
    DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
    udpSocket.receive(responsePacket);
    String serverInfo = new String(
      responsePacket.getData(),
      0,
      responsePacket.getLength()
    );
    String[] parts = serverInfo.split(":");
    System.out.println("Server's IP address: " + parts[0]);
    String serverIP = parts[0];
    int serverPort = Integer.parseInt(parts[1]);

    // Establish TCP connection to the server
    Socket socket = new Socket(serverIP, serverPort);

    in = new DataInputStream(socket.getInputStream());
    out = new DataOutputStream(socket.getOutputStream());

    Thread t = new Thread(() -> {
      try {
        receive(in);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    });

    t.start();
  }

  /**
   * private constructor. To create an instance of UI, call UI.getInstance()
   * instead.
   */
  private UI() throws IOException {
    setTitle("KidPaint");

    // Create login panel
    // JPanel loginPanel = new JPanel();
    JPanel basePanel = new JPanel();
    JPanel msgPanel = new JPanel();

    JPanel loginPanel = new JPanel();
    loginPanel.setLayout(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();

    JLabel usernameLabel = new JLabel("Username: ");
    JTextField username = new JTextField(20);

    constraints.gridx = 0;
    constraints.gridy = 0;
    loginPanel.add(usernameLabel, constraints);

    constraints.gridx = 1;
    constraints.gridy = 0;
    loginPanel.add(username, constraints);

    JButton button = new JButton("Login");

    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.gridwidth = 2;
    loginPanel.add(button, constraints);

    // Add action listener to the login button
    button.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          user = username.getText();
          // Check the username and password.
          if (user.length() >= 3 && user.length() <= 10) {
            // If login is successful, remove the login panel and add the drawing panel.
            getContentPane().remove(loginPanel);

            // send username

            try {
              System.out.println("Sending username");
              broadcastMessage(user);
            } catch (IOException e1) {
              // TODO Auto-generated catch block
              e1.printStackTrace();
            }

            getContentPane().add(basePanel, BorderLayout.CENTER);
            getContentPane().add(msgPanel, BorderLayout.EAST);
            validate();
            repaint();
          } else {
            // If login fails, show an error message.
            JOptionPane.showMessageDialog(
              UI.this,
              "Invalid username. Username must be between 3 and 10 characters",
              "Login Failed",
              JOptionPane.ERROR_MESSAGE
            );
          }
        }
      }
    );

    // // Add the login panel to the frame
    getContentPane().add(loginPanel, BorderLayout.CENTER);

    // JPanel basePanel = new JPanel();
    // getContentPane().add(basePanel, BorderLayout.CENTER);
    basePanel.setLayout(new BorderLayout(0, 0));

    paintPanel =
      new JPanel() {
        // refresh the paint panel
        @Override
        public void paint(Graphics g) {
          super.paint(g);

          Graphics2D g2 = (Graphics2D) g; // Graphics2D provides the setRenderingHints method

          // enable anti-aliasing
          RenderingHints rh = new RenderingHints(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
          );
          g2.setRenderingHints(rh);

          // clear the paint panel using black
          g2.setColor(Color.black);
          g2.fillRect(0, 0, this.getWidth(), this.getHeight());

          // draw and fill circles with the specific colors stored in the data array
          for (int x = 0; x < data.length; x++) {
            for (int y = 0; y < data[0].length; y++) {
              g2.setColor(new Color(data[x][y]));
              g2.fillArc(
                blockSize * x,
                blockSize * y,
                blockSize,
                blockSize,
                0,
                360
              );
              g2.setColor(Color.darkGray);
              g2.drawArc(
                blockSize * x,
                blockSize * y,
                blockSize,
                blockSize,
                0,
                360
              );
            }
          }
        }
      };

    paintPanel.addMouseListener(
      new MouseListener() {
        @Override
        public void mouseClicked(MouseEvent e) {}

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {}

        @Override
        public void mousePressed(MouseEvent e) {}

        // handle the mouse-up event of the paint panel
        @Override
        public void mouseReleased(MouseEvent e) {
          if (
            paintMode == PaintMode.Area && e.getX() >= 0 && e.getY() >= 0
          ) paintArea(e.getX() / blockSize, e.getY() / blockSize);
        }
      }
    );

    paintPanel.addMouseMotionListener(
      new MouseMotionListener() {
        @Override
        public void mouseDragged(MouseEvent e) {
          if (
            paintMode == PaintMode.Pixel && e.getX() >= 0 && e.getY() >= 0
          ) try { //					paintPixel(e.getX() / blockSize, e.getY() / blockSize);
            // send data to the server instead of updating the screen
            out.writeInt(1);
            out.writeInt(selectedColor);
            out.writeInt(e.getX() / blockSize);
            out.writeInt(e.getY() / blockSize);
            out.flush();
          } catch (IOException ex) {
            ex.printStackTrace(); // for debugging, remove it in production stage
          }
        }

        @Override
        public void mouseMoved(MouseEvent e) {}
      }
    );

    paintPanel.setPreferredSize(
      new Dimension(data.length * blockSize, data[0].length * blockSize)
    );

    JScrollPane scrollPaneLeft = new JScrollPane(
      paintPanel,
      JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
      JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    );

    basePanel.add(scrollPaneLeft, BorderLayout.CENTER);

    JPanel toolPanel = new JPanel();
    basePanel.add(toolPanel, BorderLayout.NORTH);
    toolPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

    pnlColorPicker = new JPanel();
    pnlColorPicker.setPreferredSize(new Dimension(24, 24));
    pnlColorPicker.setBackground(new Color(selectedColor));
    pnlColorPicker.setBorder(new LineBorder(new Color(0, 0, 0)));

    // show the color picker
    pnlColorPicker.addMouseListener(
      new MouseListener() {
        @Override
        public void mouseClicked(MouseEvent e) {}

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {}

        @Override
        public void mousePressed(MouseEvent e) {}

        @Override
        public void mouseReleased(MouseEvent e) {
          ColorPicker picker = ColorPicker.getInstance(UI.instance);
          Point location = pnlColorPicker.getLocationOnScreen();
          location.y += pnlColorPicker.getHeight();
          picker.setLocation(location);
          picker.setVisible(true);
        }
      }
    );

    toolPanel.add(pnlColorPicker);

    tglPen = new JToggleButton("Pen");
    tglPen.setSelected(true);
    toolPanel.add(tglPen);

    tglBucket = new JToggleButton("Bucket");
    toolPanel.add(tglBucket);

    JButton saveButton = new JButton("Save Sketch");
    toolPanel.add(saveButton);

    // change the paint mode to PIXEL mode
    tglPen.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
          tglPen.setSelected(true);
          tglBucket.setSelected(false);
          paintMode = PaintMode.Pixel;
        }
      }
    );

    // change the paint mode to AREA mode
    tglBucket.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
          tglPen.setSelected(false);
          tglBucket.setSelected(true);
          paintMode = PaintMode.Area;
        }
      }
    );

    /*new BufferedImage is created with the same dimensions as the component. 
    The paint method is called with the Graphics2D object of the BufferedImage to draw the sketch onto the image. 
    Then, the image is written to the selected file in PNG format using ImageIO.write.  */

    saveButton.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JFileChooser fileChooser = new JFileChooser();
          if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            BufferedImage image = new BufferedImage(
              getWidth(),
              getHeight(),
              BufferedImage.TYPE_INT_RGB
            );
            Graphics2D g2 = image.createGraphics();
            paint(g2);
            try {
              ImageIO.write(image, "png", file);
            } catch (IOException ex) {
              ex.printStackTrace();
            }
            g2.dispose();
          }
        }
      }
    );

    msgPanel.setLayout(new BorderLayout(0, 0));

    msgField = new JTextField(); // text field for inputting message

    msgPanel.add(msgField, BorderLayout.SOUTH);

    // handle key-input event of the message field
    msgField.addKeyListener(
      new KeyListener() {
        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {}

        @Override
        public void keyReleased(KeyEvent e) {
          if (e.getKeyCode() == 10) { // if the user press ENTER
            String fullMsg = user + ": " + msgField.getText();
            onTextInputted(fullMsg);
            msgField.setText("");
          }
        }
      }
    );

    chatArea = new JTextArea(); // the read only text area for showing messages
    chatArea.setEditable(false);
    chatArea.setLineWrap(true);

    JScrollPane scrollPaneRight = new JScrollPane(
      chatArea,
      JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
      JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    );
    scrollPaneRight.setPreferredSize(new Dimension(300, this.getHeight()));
    msgPanel.add(scrollPaneRight, BorderLayout.CENTER);

    this.setSize(new Dimension(800, 600));
    this.setDefaultCloseOperation(EXIT_ON_CLOSE);
  }

  /**
   * it will be invoked if the user selected the specific color through the color
   * picker
   *
   * @param colorValue - the selected color
   */
  public void selectColor(int colorValue) {
    SwingUtilities.invokeLater(() -> {
      selectedColor = colorValue;
      pnlColorPicker.setBackground(new Color(colorValue));
    });
  }

  /**
   * it will be invoked if the user inputted text in the message field
   *
   * @param text - user inputted text
   */
  private void onTextInputted(String text) {
    // chatArea.setText(chatArea.getText() + text + "\n");
    try {
      out.writeInt(0); // 0 means this is a chat message

      out.writeInt(text.length());
      out.write(text.getBytes());
      out.flush();
      System.out.println(0);
    } catch (IOException e) {}
  }

  /**
   * change the color of a specific pixel
   *
   * @param col, row - the position of the selected pixel
   */
  public void paintPixel(int col, int row) {
    if (col >= data.length || row >= data[0].length) return;

    data[col][row] = selectedColor;
    paintPanel.repaint(col * blockSize, row * blockSize, blockSize, blockSize);
  }

  public void paintPixel(int color, int col, int row) {
    if (col >= data.length || row >= data[0].length) return;

    data[col][row] = color;
    paintPanel.repaint(col * blockSize, row * blockSize, blockSize, blockSize);
  }

  /**
   * change the color of a specific area
   *
   * @param col, row - the position of the selected pixel
   * @return a list of modified pixels
   */
  public List paintArea(int col, int row) {
    LinkedList<Point> filledPixels = new LinkedList<Point>();

    if (col >= data.length || row >= data[0].length) return filledPixels;

    int[][] dataCopy = new int[data.length][];
    for (int i = 0; i < data.length; i++) {
      dataCopy[i] = data[i].clone();
    }

    int oriColor = dataCopy[col][row];
    LinkedList<Point> buffer = new LinkedList<Point>();

    if (oriColor != selectedColor) {
      buffer.add(new Point(col, row));

      while (!buffer.isEmpty()) {
        Point p = buffer.removeFirst();
        int x = p.x;
        int y = p.y;

        if (dataCopy[x][y] != oriColor) continue;

        dataCopy[x][y] = selectedColor;

        filledPixels.add(p);
        System.out.println("Painting " + p.x + ", " + p.y);
        if (x > 0 && dataCopy[x - 1][y] == oriColor) buffer.add(
          new Point(x - 1, y)
        );
        if (
          x < dataCopy.length - 1 && dataCopy[x + 1][y] == oriColor
        ) buffer.add(new Point(x + 1, y));
        if (y > 0 && dataCopy[x][y - 1] == oriColor) buffer.add(
          new Point(x, y - 1)
        );
        if (
          y < dataCopy[0].length - 1 && dataCopy[x][y + 1] == oriColor
        ) buffer.add(new Point(x, y + 1));
      }
    }

    // Send the list of painted pixels, their color, and type to the server
    try {
      System.out.println("Sending filled pixels");
      out.writeInt(2); // Type
      out.writeInt(filledPixels.size()); // Size of the list
      out.writeInt(selectedColor); // Color

      for (Point p : filledPixels) {
        System.out.println("Sending " + p.x + ", " + p.y);
        out.writeInt(p.x);
        out.writeInt(p.y);
      }
      out.flush();
      System.out.println("Sent filled pixels");
    } catch (IOException e) {
      e.printStackTrace();
    }

    return filledPixels;
  }

  /**
   * set pixel data and block size
   *
   * @param data
   * @param blockSize
   */
  public void setData(int[][] data, int blockSize) {
    this.data = data;
    this.blockSize = blockSize;
    paintPanel.setPreferredSize(
      new Dimension(data.length * blockSize, data[0].length * blockSize)
    );
    paintPanel.repaint();
  }
}
