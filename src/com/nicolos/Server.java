package com.nicolos;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
      static final int serverPort = 45126;
      int serverTimeoutTolerance; //in ms
      int serverTimeoutToleranceInSeconds; // in s
      int maxRetries;
      static ServerSocket serverSocket;

      static ArrayList<String> messagelist;

      ExecutorService executor;

      public Server(int _serverTimeoutTolerance, int _maxRetriesOnConnectionFailure) {
            try {
                  executor = Executors.newCachedThreadPool();

                  this.serverTimeoutTolerance = _serverTimeoutTolerance * 1000;
                  this.serverTimeoutToleranceInSeconds = serverTimeoutTolerance / 1000;
                  this.maxRetries = _maxRetriesOnConnectionFailure;
                  serverSocket = new ServerSocket(serverPort);
                  System.out.println(String.format("<%s>: Retrieved server socket at %d", Thread.currentThread().getName(), serverPort));
                  System.out.println("Timeout tolerance is " + serverTimeoutToleranceInSeconds + " seconds.");
                  messagelist = new ArrayList<>();
            } catch (IOException e) {
                  e.printStackTrace();
            }
      }

      public void AcceptConnection() {
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

            Socket client = WaitForConnection();

            String requestString = "/#req#/";

            executor.execute(() -> {
                  try {
                        if (client != null) {
                              Scanner in = new Scanner(client.getInputStream());
                              PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);

                              if (in.hasNext()) {
                                    String msg = in.nextLine(); // 1

                                    if (msg.contains("#1" + requestString)) {
                                          out.println(GetChatHistoryLength());
                                    } else if (msg.contains(requestString)) {
                                          System.out.println(String.format("<%s>: %s poked", Thread.currentThread().getName(), client.getInetAddress().toString()));

                                          out.println(GetChatHistoryLength()); // 2

                                          if (in.hasNext()) {
                                                boolean needsNewChat = in.nextBoolean(); // 3

                                                if (needsNewChat) {
                                                      System.out.println(String.format("<%s>: Writing chat to %s", Thread.currentThread().getName(), client.getInetAddress().toString()));

                                                      if (in.hasNext()) {
                                                            int clientChatLength = in.nextInt(); // 4
                                                            String output = WriteChat(clientChatLength);
                                                            System.out.println("String to write: " + output);
                                                            out.println(output); // 5
                                                      }
                                                } else {
                                                      System.out.println(client.getInetAddress() + "'s chat history is up-to-date.");
                                                }
                                          }
                                    } else {
                                          messagelist.add(String.format("<%s> - %s: %s", dateFormat.format(Date.from(Instant.now())), client.getInetAddress().getHostName() , msg));
                                          System.out.println(Thread.currentThread().getName() + ": " + msg);
                                    }
                              } else {
                                    System.err.println(Thread.currentThread().getName() + ": Didn't get the message.");
                              }
                        }
                  } catch (IOException e) {
                        e.printStackTrace();
                  } finally {
                        try {
                              if (client != null) {
                                    client.close();
                              }
                        } catch (IOException e) {
                              e.printStackTrace();
                        }
                  }


            });

            AcceptConnection();
      }

      private String WriteChat(int _clientChatLength) {
            ArrayList<String> newMessagelist = new ArrayList<>();
            StringBuilder sb = new StringBuilder();

            for (int i = GetChatHistoryLength()-1; i >= _clientChatLength; i--) {
                  newMessagelist.add(messagelist.get(i));
            }

            for (String s : newMessagelist) {
                  sb.append(s);
                  sb.append("\n");
            }

            return sb.toString();
      }

      private Socket WaitForConnection() {
            Socket client = null;

            try {
                  serverSocket.setSoTimeout(this.serverTimeoutTolerance);

                  System.out.println("Retries: " + maxRetries);

                  int count = 1;

                  while (client == null && count <= maxRetries) {
                        try {
                              System.out.println(String.format("<%s>: Listening..", Thread.currentThread().getName()));
                              client = serverSocket.accept();
                        } catch (SocketTimeoutException e) {
                              System.err.println(String.format("<%s>: Timeout after %d seconds.", Thread.currentThread().getName(), serverTimeoutToleranceInSeconds));
                              System.err.println(String.format("<%s>: Retrying.. %d/%d", Thread.currentThread().getName(), count, maxRetries));
                              count++;
                        } catch (IOException e) {
                              e.printStackTrace();
                        }
                  }
            } catch (SocketException e) {
                  e.printStackTrace();
            }

            return client;
      }

      public boolean CloseConnection() {
            try {
                  serverSocket.close();
                  System.out.println("Connection has been closed.");
                  return true;
            } catch (IOException e) {
                  System.err.println("Socket couldn't be closed.");
                  e.printStackTrace();
            }

            return false;
      }

      int GetChatHistoryLength() {
            return messagelist.size();
      }

      public int GetServerSocket() {
            return serverPort;
      }

      public ArrayList<String> GetMessageList() {
            return messagelist;
      }
}
