import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ServerUtils {

  private static final String LOG_DIR = "C:/DSD/Git/DSD_Assignment_COMP_6231/Assignment/logs/";

  public static int getPortFromItemId(String itemID) {
    if (itemID.startsWith("CON")) {
      return LibConstants.UDP_CON_PORT;
    }
    if (itemID.startsWith("MCG")) {
      return LibConstants.UDP_MCG_PORT;
    }
    if (itemID.startsWith("MON")) {
      return LibConstants.UDP_MON_PORT;
    }
    return 0;
  }

  public static String callUDPServer(UdpRequestModel udpRequestModel, int udpPort, Logger logger) {
    logger.info("Calling  UDP Servers");
    StringBuilder response = new StringBuilder();
    byte[] buf;
    try {
      DatagramSocket socket = new DatagramSocket();
      InetAddress address = InetAddress.getByName("localhost");
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      ObjectOutputStream os = new ObjectOutputStream(outputStream);
      os.writeObject(udpRequestModel);
      buf = outputStream.toByteArray();
      DatagramPacket packet
          = new DatagramPacket(buf, buf.length, address, udpPort);
      socket.send(packet);
      packet = new DatagramPacket(buf, buf.length);
      socket.receive(packet);
      String received = new String(
          packet.getData(), 0, packet.getLength());
      response.append(received);
      System.out.println("Data Received " + received);
    } catch (SocketException e) {
      e.printStackTrace();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return response.toString();
  }

  public static Logger setupLogger(Logger logger, String fileName, boolean showlogsInConsole)
      throws IOException {

    FileHandler fh;

    try {
      if (!showlogsInConsole) {
        logger.setUseParentHandlers(false);
      }
      fh = new FileHandler(LOG_DIR + fileName, true);
      logger.addHandler(fh);
      SimpleFormatter formatter = new SimpleFormatter();
      fh.setFormatter(formatter);

    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return logger;
  }

  public static String[] getServerInfo(String serverName) {
    String serverInfo[] = new String[4];
    serverInfo[0] = "-ORBInitialHost";
    serverInfo[1] = "localhost";
    serverInfo[2] = "-ORBInitialPort";
    if (serverName.equalsIgnoreCase("CON")) {
      serverInfo[3] = String.valueOf(LibConstants.CON_PORT);
    } else if (serverName.equalsIgnoreCase("MCG")) {
      serverInfo[3] = String.valueOf(LibConstants.MCG_PORT);
    } else if (serverName.equalsIgnoreCase("MON")) {
      serverInfo[3] = String.valueOf(LibConstants.MON_PORT);
    } else if (serverName.equalsIgnoreCase("Naming")) {
      serverInfo[3] = String.valueOf(12);
    }
    return serverInfo;
  }

  public static String determineLibOfItem(String itemId) {
    if (itemId.startsWith(LibConstants.CON_REG)) {
      return LibConstants.CON_REG;
    } else if (itemId.startsWith(LibConstants.MON_REG)) {
      return LibConstants.MON_REG;
    } else if (itemId.startsWith(LibConstants.MCG_REG)) {
      return LibConstants.MCG_REG;
    }
    return null;
  }
}
