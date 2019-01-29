import java.io.IOException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;

public class McGillServer {


  public static void main(String args[]) throws IOException {

    Logger logger = Utilities.setupLogger(Logger.getLogger("MCGServerlog"), "MCGServer.log");
    String registryURL;
    try {
      int RMIPortNum = 8081;
      McGillRemoteServiceImpl exportedObj = new McGillRemoteServiceImpl();
      Registry registry =
          LocateRegistry.createRegistry(RMIPortNum);
      registry.bind("MCG", exportedObj);
      System.out.println("Server Started");
      logger.info("Server ready.");
    } catch (Exception re) {
      logger.info("Exception " + re);
    } finally {

    }
  }


}
