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

/**
 * This class represents a 'server' object.
 * Use constructor as advised.
 */
public class Server {
      /**
       * The server's default port.
       * Do not change.
       */
      static final int SERVER_PORT = 45126;
      /**
       * A sequence of characters used by server and client to identify keep-alive-requests.
       * Do not change.
       */
      final String REQUEST_STRING = "/#req#/";
      
      /**
       * How long the server should wait before retrying a connection. Currently pretty much useless.
       */
      int serverTimeoutTolerance; //in ms
      int serverTimeoutToleranceInSeconds; // in s
      /**
       * How often the server should try listening to incoming connection after timing out. Currently pretty much useless.
       */
      int maxRetries;
      
      /**
       * The server's socket, operating on port 45126 by default.
       */
      static ServerSocket serverSocket;
      
      /**
       * An ArrayList containing every message sent to server by clients.
       */
      static ArrayList<String> messagelist;
      
      /**
       * The thread pool used by the server for various things that should not be handled by the main thread.
       */
      ExecutorService executor;
      
      /**
       * Creates a new Server object with an ExecutorService using a cached thread pool.
       * Server Socket is created on construction.
       * The Server object does not do anything by itself, use
       * Throws IOException if anything goes wrong during construction.
       * @param _serverTimeoutToleranceInSeconds Mandatory. Currently useless. How long the server should wait before reattempting a connection.
       * @param _maxRetriesOnConnectionFailure Mandatory. Currently useless. How often the server should try listening to incoming connections after timing out. Currently, the server will not stop listening.
       */
      
      //TODO make retries useful or deprecate.
      public Server(int _serverTimeoutToleranceInSeconds, int _maxRetriesOnConnectionFailure) {
            try {
                  // Creates a fresh cached thread pool for the server object
                  executor = Executors.newCachedThreadPool();

                  // See JavaDoc
                  this.serverTimeoutTolerance = _serverTimeoutToleranceInSeconds * 1000;
                  this.serverTimeoutToleranceInSeconds = serverTimeoutTolerance;
                  this.maxRetries = _maxRetriesOnConnectionFailure;
                  
                  // Build the server's socket on port 45126
                  serverSocket = new ServerSocket(SERVER_PORT);
                  System.out.println(String.format("<%s>: Retrieved server socket at %d", Thread.currentThread().getName(), SERVER_PORT));
                  System.out.println("Timeout tolerance is " + serverTimeoutToleranceInSeconds + " seconds.");
                  
                  // Create the server's chat history.
                  messagelist = new ArrayList<>();
            } catch (IOException e) {
                  e.printStackTrace();
            }
      }
      
      /**
       * Will recursively check for incoming connection. Each listener uses its own thread in the cached thread pool.
       * Must be ended manually, will otherwise run forever.
       */
      public void AcceptConnection() {
            // Timestamp for incoming messages
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

            // Create a client socket with incoming request.
            Socket client = WaitForConnection();
            

            executor.execute(() -> {
                  try {
                        if (client != null) {
                              Scanner in = new Scanner(client.getInputStream());
                              PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);

                              if (in.hasNext()) {
                                    String msg = in.nextLine(); // 1

                                    if (msg.contains("#1" + REQUEST_STRING)) {
                                          out.println(GetChatHistoryLength());
                                    } else if (msg.contains(REQUEST_STRING)) {
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
      
      /**
       * 
       * @param _clientChatLength
       * @return
       */
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
            return SERVER_PORT;
      }

      public ArrayList<String> GetMessageList() {
            return messagelist;
      }
}
