import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Logger;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;

public class MonServer extends Thread {

  private DatagramSocket socket;
  static MonRemoteServiceImpl exportedObj;
  private DatagramPacket packet;

  public MonServer(DatagramSocket socket, DatagramPacket packet) {
    this.socket = socket;
    this.packet = packet;
  }

  public static void main(String args[]) throws IOException {

    Logger logger = ServerUtils
        .setupLogger(Logger.getLogger("MONServerlog"), "MONServer.log", true);
    DatagramSocket socket = new DatagramSocket(LibConstants.UDP_MON_PORT);
    byte[] buf = new byte[256];
    try {

      ORB orb = ORB.init(args, null);
      //get reference to rootpoa & activate the POAManager
      POA rootpoa =
          (POA) orb.resolve_initial_references("RootPOA");
      rootpoa.the_POAManager().activate();
      exportedObj = new MonRemoteServiceImpl(logger);
      exportedObj.setORB(orb);
      // get object reference from the servant
      org.omg.CORBA.Object ref =
          rootpoa.servant_to_reference(exportedObj);
      // and cast the reference to a CORBA reference
      LibraryService href = LibraryServiceHelper.narrow(ref);

      // get the root naming context
      // NameService invokes the transient name service
      org.omg.CORBA.Object objRef =
          orb.resolve_initial_references("NameService");
      // Use NamingContextExt, which is part of the
      // Interoperable Naming Service (INS) specification.
      NamingContextExt ncRef =
          NamingContextExtHelper.narrow(objRef);

      // bind the Object Reference in Naming
      String name = LibConstants.MON_REG;
      NameComponent path[] = ncRef.to_name(name);
      ncRef.rebind(path, href);
      logger.info("Server ready.");
      boolean running = true;
      System.out.println("UDP Server is listening on port" + LibConstants.MON_PORT);
      while (running) {
        DatagramPacket packet
            = new DatagramPacket(buf, buf.length);
        try {
          socket.receive(packet);
          MonServer monServer = new MonServer(socket, packet);
          monServer.run();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      orb.run();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  @Override
  public void run() {
    System.out.println(this.getId());
    DatagramPacket reponsePacket = null;
    InetAddress address = packet.getAddress();
    int port = packet.getPort();
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
      exportedObj.logger.info(request.getMethodName() + " is called by " + address + ":" + port);
      String response = null;
      reponsePacket = getDatagramPacket(reponsePacket, address, port, request, response,
          exportedObj, exportedObj.logger);
      exportedObj.logger.info("sending response " + reponsePacket.getData().toString());

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

  private static synchronized DatagramPacket getDatagramPacket(DatagramPacket reponsePacket,
      InetAddress address,
      int port, UdpRequestModel request, String response, MonRemoteServiceImpl exportedObj,
      Logger logger) {
    if (request.getMethodName().equalsIgnoreCase("findItem")) {
      response = getFindItemResponse(request, exportedObj, logger);
    } else if (request.getMethodName().equalsIgnoreCase("borrowItem")) {
      response = getBorrowItemResponse(request, exportedObj, logger);
    } else if (request.getMethodName().equalsIgnoreCase(LibConstants.OPR_WAIT_LIST)) {
      response = getWaitListResponse(request, exportedObj, logger);
    } else if (request.getMethodName().equalsIgnoreCase("returnItem")) {
      response = getReturnItemResponse(request, exportedObj, logger);
    } else if (request.getMethodName().equalsIgnoreCase(LibConstants.USER_BORROWED_ITEMS)) {
      response = exportedObj
          .isUsereligibleToGetbook(request.getUserId(), request.getItemId(), false);
    } else if (request.getMethodName().equalsIgnoreCase("validateBorrow")) {
      response = String.valueOf(
          exportedObj.isItemAvailableToBorrow(request.getItemId(), request.getUserId(),
              request.getNumberOfDays()));
    } else if (request.getMethodName().equalsIgnoreCase("validateReturn")) {
      response = String.valueOf(
          exportedObj.isValidReturn(request.getUserId(), request.getItemId()));
    }
    System.out.println("Response to send from udp is " + response);

    if (response != null && response.length() > 0) {
      reponsePacket = new DatagramPacket(response.getBytes(), response.getBytes().length,
          address, port);
    } else {
      String noResponse = "No Data Found";
      reponsePacket = new DatagramPacket(noResponse.getBytes(), noResponse.getBytes().length,
          address, port);
    }
    return reponsePacket;
  }

  private static String getReturnItemResponse(UdpRequestModel request,
      MonRemoteServiceImpl exportedObj, Logger logger) {
    String response;
    logger.info(
        "Return item is Called :   Item is " + request.getItemId() + " User " + request
            .getUserId());
    response = exportedObj
        .performReturnItemOperation(request.getUserId(), request.getItemId(), false);
    logger.info("Response is" + response);
    return response;
  }

  private static String getWaitListResponse(UdpRequestModel request,
      MonRemoteServiceImpl exportedObj, Logger logger) {
    String response;
    logger.info(
        "User Selected to be in Wait List For item" + request.getItemId() + " User " + request
            .getUserId());
    response = exportedObj
        .addUserInWaitList(request.getItemId(), request.getUserId(), request.getNumberOfDays(),
            false);
    logger.info("Response is" + response);
    return response;
  }

  private static String getBorrowItemResponse(UdpRequestModel request,
      MonRemoteServiceImpl exportedObj, Logger logger) {
    String response;
    logger.info(
        "Borrow item is Called : Requested Item is " + request.getItemId() + " User " + request
            .getUserId() + " for days " + request.getNumberOfDays());

    response = exportedObj.performBorrowItemOperation(request.getItemId(), request.getUserId(),
        request.getNumberOfDays());

    logger.info("Response is" + response);
    return response;
  }

  private static String getFindItemResponse(UdpRequestModel request,
      MonRemoteServiceImpl exportedObj, Logger logger) {
    String response;
    logger.info("FindItem is Called : Requested Item is " + request.getItemName());
    response = exportedObj.findItem(request.getItemName(), false);
    logger.info("Response is  " + response);
    return response;
  }
}
