import java.io.IOException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;

public class MonServer {

  public static void main(String args[]) throws IOException {

    Logger logger = Utilities.setupLogger(Logger.getLogger("MONServerlog"), "MONServer.log");
    String registryURL;
    try {
      int RMIPortNum = 8082;
      MonRemoteServiceImpl exportedObj = new MonRemoteServiceImpl();
      Registry registry =
          LocateRegistry.createRegistry(RMIPortNum);
      registry.bind("MON", exportedObj);
      System.out.println("Server Started");
      logger.info("Server ready.");
    } catch (Exception re) {
      logger.info("Exception " + re);
    } finally {

    }
  }
}
