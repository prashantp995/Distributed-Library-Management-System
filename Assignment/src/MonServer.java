import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;

public class MonServer {

  public static void main(String args[]) throws IOException {

    Logger logger = Utilities.setupLogger(Logger.getLogger("MONServerlog"), "MONServer.log");
    try {
      int RMIPortNum = LibConstants.MON_PORT;
      MonRemoteServiceImpl exportedObj = new MonRemoteServiceImpl();
      Registry registry =
          LocateRegistry.createRegistry(RMIPortNum);
      registry.bind(LibConstants.MON_REG, exportedObj);
      System.out.println("Server Started " + " Rmi Port Number " + RMIPortNum + " Look Up "
          + LibConstants.MON_REG);
      logger.info("Server ready.");
    } catch (Exception re) {
      logger.info("Exception " + re);
    } finally {
      Utilities.closeLoggerHandlers(logger);
    }
  }
}
