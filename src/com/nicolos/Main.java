package com.nicolos;

import java.util.Scanner;

public class Main {
      public static void main(String[] args) {
            Scanner input = new Scanner(System.in);

            System.out.print("""
                    Enter one of the following commands:
                    start -r -ti\t-\tSpecify number of retries and timeout tolerance in seconds with -r. Like so: "start -r=5 -ti=5"
                    start\t-\tDefault - Retries: 5, Timeout tolerance: 10 seconds
                    quit\t-\tExits the application.
                    """);
            new Thread(() -> {
                  while (true) {
                        System.out.println(Thread.currentThread().getName() + ": Waiting for your input..");

                        String inputString = input.nextLine();

                        switch (inputString) {
                              case "start" -> {
                                    System.out.println("Starting..");
                                    Server server = new Server(10, 5);
                                    server.AcceptConnection();
                                    server.CloseConnection();
                              }
                              case "quit" -> {
                                    System.err.println("Exiting..");
                                    return;
                              }
                              default -> {
                                    if (inputString.matches("start\\s-r=\\d*\\s-ti=\\d*")) {
                                          int r = Integer.parseInt(inputString.trim().substring(9, inputString.indexOf("ti") - 2));
                                          int ti = Integer.parseInt(inputString.trim().substring(inputString.indexOf("ti") + 3, inputString.length()));
                                          Server server = new Server(r, ti);
                                          System.out.println("Starting..");
                                          server.AcceptConnection();
                                          server.CloseConnection();
                                    } else {
                                          System.err.println("Something went wrong, try again.");
                                    }
                              }
                        }
                  }
            }).start();
      }
}
