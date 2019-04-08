package com.dsd.as3;

import java.rmi.*;


public interface CallbackClientInterface
    extends Remote {

  public int askForWaitingList()
      throws RemoteException;

} // end interface
