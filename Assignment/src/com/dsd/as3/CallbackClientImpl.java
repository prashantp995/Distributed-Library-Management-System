package com.dsd.as3;

import java.rmi.*;
import java.rmi.server.*;
import java.util.Scanner;


public class CallbackClientImpl extends UnicastRemoteObject
    implements CallbackClientInterface {

  public CallbackClientImpl() throws RemoteException {
    super();
  }


  @Override
  public int askForWaitingList() throws RemoteException {
    Scanner scanner = new Scanner(System.in);
    System.out.println(
        "Item is not available now , WaitList Possible, Do you wish to enroll your self in waitList "
            + "\n" +
            "Please enter 1 for yes" + "\n" +
            "Please enter 0 for No");
    int choice = scanner.nextInt();
    return choice;
  }
}// end CallbackClientImpl class
