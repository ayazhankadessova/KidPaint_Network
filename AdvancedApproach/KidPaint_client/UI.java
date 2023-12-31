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
// import java.awt.event;f
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
  private int penSize = 1;
  // private static JTextField username;
  private String user;
  private String shape = "Triangle";

  // private static JButton button;
  // add array of integers
  private List<Integer> studios;
  private static UI instance;
  private int selectedColor = -543230; // golden
  // private static int studioChoice = 2;

  DatagramSocket udpSocket = new DatagramSocket(12348);

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
          case 3:
            receiveStudios(in);
          case 4:
            // clearSketch(in);
            clear();
            repaint();
            break;
          case 5:
            receiveFileMessage(in);
            break;
          default:
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  // TEST
  private void receiveStudios(DataInputStream in) throws IOException {
    int numberOfStudios = in.readInt();
    studios = new ArrayList<>();
    for (int i = 0; i < numberOfStudios; i++) {
      studios.add(in.readInt());
    }
    System.out.println("Studios size:" + studios.size());
  }

  // private void clearSketch(DataInputStream in) throws IOException {
  //   int numberOfStudios = in.readInt();
  //   studios = new ArrayList<>();
  //   for (int i = 0; i < numberOfStudios; i++) {
  //     studios.add(in.readInt());
  //   }
  //   System.out.println("Studios size:" + studios.size());
  // }

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

  private void receiveFileMessage(DataInputStream in) throws IOException {
    byte[] buffer = new byte[1024];
    // read the file size
    int fileSize = in.readInt();

    // Save the file
    File receivedFile = new File("receivedFile_new");
    FileOutputStream fos = new FileOutputStream(receivedFile);

    int read = 0;
    while (read < fileSize) {
      int actual = in.read(buffer, 0, Math.min(1024, fileSize - read));
      fos.write(buffer, 0, actual);
      read += actual;
    }

    fos.close();

    String msg = "Received a file: " + receivedFile.getAbsolutePath();
    System.out.println(msg);

    SwingUtilities.invokeLater(() -> {
      chatArea.append(msg + "\n");
    });
  }

  private void receivePixelMessage(DataInputStream in) throws IOException {
    int color = in.readInt();
    int x = in.readInt();
    int y = in.readInt();

    System.out.println("Receiving pixel message");
    paintPixel(color, x, y);
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

    udpSocket.close();

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
            // If login is successful, remove the login panel and add the studio selection panel.
            getContentPane().remove(loginPanel);

            // send username
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
              @Override
              protected Void doInBackground() throws Exception {
                System.out.println("Sending username");
                broadcastMessage(user);

                // sleep for 10 seconds
                try {
                  Thread.sleep(5000);
                } catch (InterruptedException ex) {
                  ex.printStackTrace();
                }

                return null;
              }

              @Override
              protected void done() {
                // This method will be called on the EDT after doInBackground() has finished
                // Check if studios is null
                // if (studios == null) {
                //   studios = new ArrayList<>(Arrays.asList(1, 2, 3));
                //   System.out.println("Studios is null");
                // }

                // Create studio selection panel
                JPanel studioPanel = new JPanel();
                studioPanel.setLayout(new FlowLayout()); // Change layout to BorderLayout

                JLabel studioLabel = new JLabel("Select Studio: \n");
                JTextField studioField = new JTextField(3); // Create a text field for input
                studioField.setPreferredSize(new Dimension(50, 100));

                // Add the studioField to the NORTH position
                studioPanel.add(studioField);

                // Create a panel for the studios and set its layout to BoxLayout
                JPanel studiosPanel = new JPanel();
                studiosPanel.setLayout(
                  new BoxLayout(studiosPanel, BoxLayout.PAGE_AXIS)
                );

                studiosPanel.add(studioLabel);
                studiosPanel.add(Box.createVerticalStrut(10)); // Add vertical strut to create space

                for (int i = 1; i <= studios.size(); i++) {
                  JLabel studioOptionLabel = new JLabel("Studio " + i);
                  studiosPanel.add(studioOptionLabel);
                }

                // Add the studiosPanel to the CENTER position
                studioPanel.add(studiosPanel, BorderLayout.CENTER);

                JButton okButton = new JButton("OK");
                studiosPanel.add(Box.createVerticalStrut(10)); // Add another strut for space before the button
                studiosPanel.add(okButton);

                // Add action listener to the OK button
                okButton.addActionListener(
                  new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                      String studioInput = studioField.getText();
                      try {
                        int selectedStudio = Integer.parseInt(studioInput);
                        if (
                          selectedStudio < 1 || selectedStudio > studios.size()
                        ) {
                          JOptionPane.showMessageDialog(
                            null,
                            "Invalid studio number. Number or studios: " +
                            studios.size(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                          );
                        } else {
                          out.writeInt(selectedStudio);

                          // Remove studio selection panel and add base and message panels
                          getContentPane().remove(studioPanel);
                          getContentPane().add(basePanel, BorderLayout.CENTER);
                          getContentPane().add(msgPanel, BorderLayout.EAST);
                          validate();
                          repaint();
                        }
                      } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(
                          null,
                          "Invalid input. Please enter a number.",
                          "Error",
                          JOptionPane.ERROR_MESSAGE
                        );
                      } catch (IOException ex) {
                        ex.printStackTrace();
                      }
                    }
                  }
                );

                // Create choice panel
                JPanel choicePanel = new JPanel();

                // Create "Join Existing Studio" button
                JButton joinButton = new JButton("Join Existing Studio");
                joinButton.addActionListener(
                  new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                      // Show studioPanel
                      getContentPane().remove(choicePanel);
                      getContentPane().add(studioPanel, BorderLayout.CENTER);
                      validate();
                      repaint();
                    }
                  }
                );
                choicePanel.add(joinButton);

                // Create "Create New Studio" button
                JButton createButton = new JButton("Create New Studio");
                createButton.addActionListener(
                  new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                      System.out.println(
                        "Sending studio choice" + (studios.size() + 1)
                      );
                      // Send nextStudio to the server
                      try {
                        out.writeInt(studios.size() + 1);
                      } catch (IOException ex) {
                        ex.printStackTrace();
                      }

                      // Show dialog to enter width and height
                      String width = JOptionPane.showInputDialog(
                        "Enter width:"
                      );
                      String height = JOptionPane.showInputDialog(
                        "Enter height:"
                      );

                      // Create a new data array with the specified width and height
                      int[][] newData = new int[Integer.parseInt(
                        width
                      )][Integer.parseInt(height)];

                      // Replace the old data array with the new one
                      data = newData;

                      // Send width and height to the server
                      // try {
                      //   out.writeInt(Integer.parseInt(width));
                      //   out.writeInt(Integer.parseInt(height));
                      // } catch (IOException ex) {
                      //   ex.printStackTrace();
                      // }

                      // Show basePanel and msgPanel
                      getContentPane().remove(choicePanel);
                      getContentPane().add(basePanel, BorderLayout.CENTER);
                      getContentPane().add(msgPanel, BorderLayout.EAST);
                      validate();
                      repaint();
                    }
                  }
                );
                choicePanel.add(createButton);
                // Add the studio selection panel to the frame
                getContentPane().add(choicePanel, BorderLayout.CENTER);
                validate();
                repaint();
              }
            };

            worker.execute();
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

    // Add the login panel to the frame
    getContentPane().add(loginPanel, BorderLayout.CENTER);

    basePanel.setLayout(new BorderLayout(0, 0));

    paintPanel =
      new JPanel() {
        // refresh the paint panel
        @Override
        public void paint(Graphics g) {
          super.paint(g);

          // create g2 object - better + more functionality
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
              // fill the circle
              g2.fillArc(
                blockSize * x,
                blockSize * y,
                blockSize,
                blockSize,
                0,
                360
              );
              g2.setColor(Color.darkGray);
              // draw the outline of the circle
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
        public void mouseClicked(MouseEvent e) {
          if (SwingUtilities.isRightMouseButton(e)) {
            int x = e.getX() / blockSize;
            int y = e.getY() / blockSize;
            int originalColor = data[x][y];

            for (int i = 0; i < data.length; i++) {
              for (int j = 0; j < data[0].length; j++) {
                if (data[i][j] == originalColor) {
                  for (int dx = 0; dx < penSize; dx++) {
                    if (i + dx < data.length && j < data[0].length) {
                      data[i + dx][j] = selectedColor;

                      // send data to the server
                      try {
                        out.writeInt(1);
                        out.writeInt(selectedColor);
                        out.writeInt(i + dx);
                        out.writeInt(j);
                        System.out.println("Sending " + (i + dx) + ", " + j);
                        out.flush();
                      } catch (IOException ex) {
                        ex.printStackTrace(); // for debugging, remove it in production stage
                      }
                    }
                  }
                }
              }
            }
          }
        }

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
          if (paintMode == PaintMode.Pixel && e.getX() >= 0 && e.getY() >= 0) {
            int x = e.getX() / blockSize;
            int y = e.getY() / blockSize;

            if (shape.equals("Circle")) {
              // send coordinates for a circle
              sendCoordinates(x, y);
            } else if (shape.equals("Square")) {
              // send coordinates for a larger square
              for (int i = 0; i <= 3; i++) {
                for (int j = 0; j <= 3; j++) {
                  sendCoordinates(x + i, y + j);
                }
              }
            } else if (shape.equals("Triangle")) {
              // send coordinates for a larger triangle
              for (int i = 0; i <= 3; i++) {
                for (int j = 0; j <= i; j++) {
                  sendCoordinates(x + j, y + i);
                }
              }
            } else if (shape.equals("Diamond")) {
              // send coordinates for a diamond
              sendCoordinates(x, y); // left corner
              sendCoordinates(x + 3, y); // top corner
              sendCoordinates(x + 6, y); // right corner
              sendCoordinates(x + 3, y + 3); // bottom corner
            }
          }
        }

        // private void sendCoordinates(int x, int y) {
        //   if (x < data.length && y < data[0].length) {
        //     try {
        //       out.writeInt(1);
        //       out.writeInt(selectedColor);
        //       out.writeInt(x);
        //       out.writeInt(y);
        //       out.flush();
        //     } catch (IOException ex) {
        //       ex.printStackTrace(); // for debugging, remove it in production stage
        //     }
        //   }
        // }

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

    // Create an erase button
    JButton eraseButton = new JButton("Erase");

    // Add an action listener to the erase button
    eraseButton.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          // Set the selected color to black when the erase button is clicked
          selectedColor = Color.BLACK.getRGB();
        }
      }
    );

    // Add the erase button to the tool panel
    toolPanel.add(eraseButton);

    String[] shapes = { "Square", "Triangle", "Circle", "Diamond" };
    JComboBox<String> shapeList = new JComboBox<>(shapes);
    shapeList.setSelectedIndex(0);
    shapeList.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          JComboBox cb = (JComboBox) e.getSource();
          shape = (String) cb.getSelectedItem();
          // set your shape variable here
        }
      }
    );
    toolPanel.add(shapeList);

    tglBucket = new JToggleButton("Bucket");
    toolPanel.add(tglBucket);

    JButton saveButton = new JButton("Save");
    toolPanel.add(saveButton);

    JButton LoadButton = new JButton("Load");
    toolPanel.add(LoadButton);

    JButton clear = new JButton("Clear");
    // clear.setPreferredSize(new Dimension(50, 25)); // set preferred size
    toolPanel.add(clear);

    // Create a slider with a range from 1 to 3
    JSlider penSizeSlider = new JSlider(1, 3);

    // Set the initial value
    penSizeSlider.setValue(1);

    // Add labels to the slider
    penSizeSlider.setPaintLabels(true);

    // Add the slider to the tool panel
    toolPanel.add(new JLabel("Pen Size:"));
    toolPanel.add(penSizeSlider);

    // Add a change listener to the slider to update the pen size when the slider is moved
    penSizeSlider.addChangeListener(
      new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          penSize = ((JSlider) e.getSource()).getValue();
          System.out.println("Pen size: " + penSize);
        }
      }
    );

    clear.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          try {
            out.writeInt(4);
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        }
      }
    );

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

    // create a Buffered Image & use Graphics2D to draw on it
    // save the image to a file

    saveButton.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JFileChooser fileChooser = new JFileChooser();
          if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            System.out.println("Saving to file: " + file.getAbsolutePath()); // print the file path

            BufferedImage image = new BufferedImage(
              data.length * blockSize,
              data[0].length * blockSize,
              BufferedImage.TYPE_INT_RGB
            );

            // create graphics so new tool understands
            Graphics2D g2 = image.createGraphics();

            // enable anti-aliasing
            RenderingHints rh = new RenderingHints(
              RenderingHints.KEY_ANTIALIASING,
              RenderingHints.VALUE_ANTIALIAS_ON
            );
            g2.setRenderingHints(rh);

            // clear the image using black
            g2.setColor(Color.black);
            g2.fillRect(0, 0, image.getWidth(), image.getHeight());

            // draw and fill circles with the specific colors stored in the data array
            for (int x = 0; x < data.length; x++) {
              for (int y = 0; y < data[0].length; y++) {
                g2.setColor(new Color(data[x][y]));
                // fill the circle
                g2.fillArc(
                  blockSize * x,
                  blockSize * y,
                  blockSize,
                  blockSize,
                  0,
                  360
                );
                g2.setColor(Color.darkGray);
                // draw the outline of the circle
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

    LoadButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          JFileChooser fileChooser = new JFileChooser();
          int returnValue = fileChooser.showOpenDialog(null);
          if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
              BufferedImage base = ImageIO.read(selectedFile);

              out.writeInt(3);
              out.writeInt(base.getWidth() / blockSize);
              out.writeInt(base.getHeight() / blockSize);

              // Iterate over the pixels of the image
              for (int x = 0; x < base.getWidth(); x += blockSize) {
                for (int y = 0; y < base.getHeight(); y += blockSize) {
                  // Get the RGB value of the center pixel of the block
                  int centerX = x + blockSize / 2;
                  int centerY = y + blockSize / 2;
                  if (centerX < base.getWidth() && centerY < base.getHeight()) {
                    int color = base.getRGB(centerX, centerY);

                    // Send the color and coordinates to the server
                    out.writeInt(color); // color
                    out.writeInt(x / blockSize); // x
                    out.writeInt(y / blockSize); // y

                    // Print the color and coordinates
                    System.out.println(
                      "Sending " +
                      color +
                      " " +
                      x /
                      blockSize +
                      " " +
                      y /
                      blockSize
                    );
                  }
                }
              }

              out.flush();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }
    );

    msgPanel.setLayout(new BorderLayout(0, 0));

    msgField = new JTextField(); // text field for inputting message

    msgPanel.add(msgField, BorderLayout.SOUTH);

    // Add a button for choosing a file
    JButton fileButton = new JButton("Choose File");
    msgPanel.add(fileButton, BorderLayout.EAST);

    // handle button click event of the file button
    fileButton.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JFileChooser fileChooser = new JFileChooser();
          int returnValue = fileChooser.showOpenDialog(null);
          if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            onFileSelected(selectedFile);
          }
        }
      }
    );

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

  private void onFileSelected(File file) {
    try {
      FileInputStream fis = new FileInputStream(file);
      byte[] buffer = new byte[1024];

      out.writeInt(5); // 5 means this is a file
      out.writeInt((int) file.length());

      int count;
      while ((count = fis.read(buffer)) > 0) {
        out.write(buffer, 0, count);
      }

      out.flush();
      fis.close();
    } catch (IOException e) {
      e.printStackTrace(); // for debugging, remove it in production stage
    }
  }

  private void sendCoordinates(int x, int y) {
    if (x < data.length && y < data[0].length) {
      try {
        out.writeInt(1);
        out.writeInt(selectedColor);
        out.writeInt(x);
        out.writeInt(y);
        System.out.println("Sending " + x + ", " + y);
        out.flush();
      } catch (IOException ex) {
        ex.printStackTrace(); // for debugging, remove it in production stage
      }
    }
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

  // public void paintPixel(int color, int col, int row, int penSize) {
  //   if (col >= data.length || row >= data[0].length) return;

  //   for (int i = col; i < col + penSize && i < data.length; i++) {
  //     for (int j = row; j < row + penSize && j < data[0].length; j++) {
  //       data[i][j] = color;
  //     }
  //   }

  //   paintPanel.repaint(
  //     col * blockSize,
  //     row * blockSize,
  //     penSize * blockSize,
  //     penSize * blockSize
  //   );
  // }

  public void clear() {
    int blackColor = Color.BLACK.getRGB();
    for (int col = 0; col < data.length; col++) {
      for (int row = 0; row < data[col].length; row++) {
        data[col][row] = blackColor; // Set to background color
      }
    }
    paintPanel.repaint(); // Repaint the entire panel
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
