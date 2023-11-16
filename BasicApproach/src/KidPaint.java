import java.io.IOException;

public class KidPaint {

  public static void main(String[] args) throws IOException {
    UI ui = UI.getInstance(); // get the instance of UI
    ui.setData(new int[50][50], 20); // set the data array and block size. comment this statement to use the default data array and block size.
    ui.setVisible(true); // set the ui
  }
}
