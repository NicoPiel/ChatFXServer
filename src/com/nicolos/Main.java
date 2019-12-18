package com.nicolos;

import java.util.Scanner;

/**
 * <p>
 * A custom CLI to control the server.
 * It will create a new thread to listen to your commands.
 * If you want to create your own, look at this one first.
 * </p>
 */
public class Main {
      /**
       * <p>
       * Will start and close the server for you.
       * If you want more advanced functionality, write your own CLI.
       * </p>
       * @param args Arguments to start the <code>main</code> function with. In this case usually empty.
       */
      public static void main(String[] args) {
            Scanner input = new Scanner(System.in);

            System.out.println(
                    """
                                    Enter one of the following commands:
                                    start\t\t\t-\tStarts the server.
                                    quit\t\t\t-\tExits the application.
                            """
            );
            while (true) {
                  System.out.println(String.format("<%s>: Waiting for your input..", Thread.currentThread().getName()));

                  String inputString = input.nextLine();

                  switch (inputString) {
                        case "start" -> {
                              new Thread(() -> {
                                    System.out.println("Starting..");
                                    System.out.println("You can exit at any time.");
                                    Server server = new Server();
                                    server.AcceptConnection();
                                    Server.CloseConnection();
                              }).start();
                        }
                        case "quit" -> {
                              System.err.println("Exiting..");
                              Server.CloseConnection();
                              System.exit(0);
                              return;
                        }
                        default -> System.out.println("Try again.");
                  }
            }
      }
}
