import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;

public class McGillServer {


  public static void main(String args[]) throws IOException {

    Logger logger = Utilities.setupLogger(Logger.getLogger("MCGServerlog"), "MCGServer.log");
    String registryURL;
    DatagramSocket socket = new DatagramSocket(LibConstants.UDP_MCG_PORT);
    byte[] buf = new byte[256];
    try {
      int RMIPortNum = LibConstants.MCG_PORT;
      McGillRemoteServiceImpl exportedObj = new McGillRemoteServiceImpl();
      Registry registry =
          LocateRegistry.createRegistry(RMIPortNum);
      registry.bind(LibConstants.MCG_REG, exportedObj);
      System.out.println("Server Started " + " Rmi Port Number " + RMIPortNum + " Look Up "
          + LibConstants.MCG_REG);
      logger.info("Server ready.");
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          boolean running = true;
          System.out.println("UDP Server is listening on port" + LibConstants.UDP_MCG_PORT);
          DatagramPacket reponsePacket = null;
          while (running) {
            DatagramPacket packet
                = new DatagramPacket(buf, buf.length);
            try {
              socket.receive(packet);
            } catch (IOException e) {
              e.printStackTrace();
            }

            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            packet = new DatagramPacket(buf, buf.length, address, port);
            String received
                = new String(packet.getData(), 0, packet.getLength());
            byte[] data = packet.getData();
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ObjectInputStream is = null;
            try {
              is = new ObjectInputStream(in);
            } catch (IOException e) {
              e.printStackTrace();
            }
            try {
              assert is != null;
              UdpRequestModel request = (UdpRequestModel) is.readObject();
              logger.info(request.getMethodName() + " is called by " + address + ":" + port);
              String response = null;
              reponsePacket = getDatagramPacket(reponsePacket, address, port, request, response,
                  exportedObj);
              logger.info("sending response " + reponsePacket);

            } catch (ClassNotFoundException e) {
              e.printStackTrace();
            } catch (IOException e) {
              e.printStackTrace();
            }
            try {
              socket.send(reponsePacket);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
          socket.close();
        }
      };
      runnable.run();

    } catch (Exception re) {
      logger.info("Exception " + re);
      re.printStackTrace();
    } finally {

    }
  }

  private static DatagramPacket getDatagramPacket(DatagramPacket reponsePacket,
      InetAddress address,
      int port, UdpRequestModel request, String response, McGillRemoteServiceImpl exportedObj) {
    if (request.getMethodName().equalsIgnoreCase("findItem")) {
      response = exportedObj.findItem(request.getItemName(), false);
    }
    if (request.getMethodName().equalsIgnoreCase("borrowItem")) {
      response = exportedObj.performBorrowItemOperation(request.getItemId(), request.getUserId(),
          request.getNumberOfDays());
    }
    if (request.getMethodName().equalsIgnoreCase(LibConstants.OPR_WAIT_LIST)) {
      response = exportedObj
          .addUserInWaitList(request.getItemId(), request.getUserId(), request.getNumberOfDays(),
              false);
    }
    System.out.println("Response to send from udp is " + response);

    if (response != null && response.length() > 0) {
      reponsePacket = new DatagramPacket(response.getBytes(), response.getBytes().length,
          address, port);
    } else {
      reponsePacket = new DatagramPacket("No Data".getBytes(), "No Data".getBytes().length,
          address, port);
    }
    return reponsePacket;
  }


}
