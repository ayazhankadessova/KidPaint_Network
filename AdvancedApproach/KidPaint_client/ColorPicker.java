import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog.ModalExclusionType;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Window.Type;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class ColorPicker extends JDialog {

  BufferedImage colorImage;
  int selectedColor = 0;

  private static ColorPicker instance;

  /**
   * get instance of ColorPicker. Singleton design pattern.
   * @param parent
   * @return
   */
  public static ColorPicker getInstance(JFrame parent) {
    if (instance == null) instance = new ColorPicker(parent);
    return instance;
  }

  /**
   * private constructor. To create an instance of ColorPicker, call getInstance() instead.
   * @param parent
   */
  private ColorPicker(JFrame parent) {
    super(parent, "Color Picker", true);
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

    setType(Type.POPUP);
    setResizable(false);

    JPanel panel = new JPanel() {
      @Override
      public void paint(Graphics g) {
        super.paint(g);
        g.drawImage(colorImage, 0, 0, this);
      }
    };

    this.setContentPane(panel);

    try {
      // load the color-spectrum image
      colorImage = ImageIO.read(new File("color-spectrum.jpg"));
      if (colorImage != null) {
        panel.setPreferredSize(
          new Dimension(colorImage.getWidth(), colorImage.getHeight())
        );
      } else throw new IOException("Unable to load color spectrum.");
    } catch (IOException e) {
      e.printStackTrace();
    }

    this.getContentPane()
      .addMouseListener(
        new MouseListener() {
          @Override
          public void mouseClicked(MouseEvent e) {}

          @Override
          public void mouseEntered(MouseEvent e) {}

          @Override
          public void mouseExited(MouseEvent e) {}

          @Override
          public void mousePressed(MouseEvent e) {}

          // handle the mouse-up event of the color picker
          @Override
          public void mouseReleased(MouseEvent e) {
            try {
              selectedColor = colorImage.getRGB(e.getX(), e.getY());
              try {
                UI.getInstance().selectColor(selectedColor);
              } catch (IOException ioException) {
                ioException.printStackTrace();
              }
            } catch (IndexOutOfBoundsException ex) {}
          }
        }
      );

    this.pack();
  }
}
