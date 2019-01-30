import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;

public class ConcordiaServer {

  public static void main(String args[]) throws IOException {

    Logger logger = Utilities.setupLogger(Logger.getLogger("CONServerlog"), "ConcordiaServer.log");
    String registryURL;
    try {
      int RMIPortNum = LibConstants.CON_PORT;
      ConcordiaRemoteServiceImpl exportedObj = new ConcordiaRemoteServiceImpl();
      Registry registry =
          LocateRegistry.createRegistry(RMIPortNum);
      registry.bind(LibConstants.CON_REG, exportedObj);
      System.out.println("Server Started " + " Rmi Port Number " + RMIPortNum + " Look Up "
          + LibConstants.CON_REG);
      logger.info("Server ready.");
    } catch (Exception re) {
      logger.info("Exception " + re);
    } finally {

    }
  }


}
