/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui;

import ucar.ui.util.Resource;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;

/**
 *
 */
public class ToolsSplashScreen extends JWindow {
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static ToolsSplashScreen instance;

  /**
   *
   */
  public static ToolsSplashScreen getSharedInstance() {
    if (instance == null) {
      instance = new ToolsSplashScreen();
    }

    return instance;
  }

  /**
   *
   */
  private ToolsSplashScreen() {
    Image image = Resource.getImage("/resources/ui/pix/ring2.jpg");

    if (image != null) {
      ImageIcon icon = new ImageIcon(image);
      JLabel iconLabel = new JLabel(icon);

      getContentPane().add(iconLabel);

      pack();

      int width = icon.getIconWidth();
      int height = icon.getIconHeight();

      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      GraphicsDevice gd = ge.getDefaultScreenDevice();
      GraphicsConfiguration gc = gd.getDefaultConfiguration();

      Point location;

      if (gc != null) {
        Rectangle gcrect = gc.getBounds();

        location = new Point(gcrect.x + gcrect.width / 2 - (width / 2), gcrect.y + gcrect.height / 2 - (height / 2));
      } else {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        location = new Point(screenSize.width / 2 - (width / 2), screenSize.height / 2 - (height / 2));
      }

      setLocation(location);

      // Any click on the window hides it.
      addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          setVisible(false);
        }
      });
    }
  }
}
