package com.nicolos;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * This class represents a 'server' object.
 * Use constructor as advised.
 * </p>
 *
 * @author Nico Piel
 * @version 0.1
 */
public class Server {
      /**
       * <p>
       * The server's default port.
       * Do not change.
       * </p>
       */
      static final int SERVER_PORT = 45126;
      /**
       * <p>
       * A sequence of characters used by server and client to identify keep-alive-requests.
       * Do not change.
       * </p>
       */
      final String REQUEST_STRING = "/#req#/";

      /**
       * The server's socket, operating on port 45126 by default.
       *
       * @see ServerSocket
       */
      static ServerSocket serverSocket;

      /**
       * An ArrayList containing every message sent to server by clients.
       *
       * @see ArrayList
       */
      static ArrayList<String> messagelist;

      /**
       * The thread pool used by the server for various things that should not be handled by the main thread.
       *
       * @see ExecutorService
       */
      ExecutorService executor;

      /**
       * <p>
       * Creates a new Server object with an ExecutorService using a cached thread pool.
       * Server Socket is created on construction.
       * The Server object does not do anything by itself, use the helper methods.
       * Throws IOException if anything goes wrong during construction.
       * </p>
       */

      //TODO make retries useful or deprecate.
      public Server() {
            try {
                  // Creates a fresh cached thread pool for the server object
                  executor = Executors.newCachedThreadPool();

                  // Build the server's socket on port 45126
                  serverSocket = new ServerSocket(SERVER_PORT);
                  System.out.println(String.format("<%s>: Retrieved server socket at %d", Thread.currentThread().getName(), SERVER_PORT));

                  // Create the server's chat history.
                  messagelist = new ArrayList<>();
            } catch (IOException e) {
                  e.printStackTrace();
            }
      }

      /**
       * <p>
       * Will recursively check for incoming connection. Each listener uses its own thread in the cached thread pool.
       * Must be ended manually, will otherwise run forever.
       * </p>
       * <p>
       * (1) Gets the client's input message
       * <p>
       * If (1) equals the input string defined in this class (plus some extra stuff at the beginning), only return the length of the chat history
       * This means that the client is connecting for the first time, not requesting a keep-alive
       * </p>
       * <p>
       * If (1) contains the input string defined in this class, the client would like to receive a keep-alive and its missing chat history
       * </p>
       * <p>
       * (2) Return the length of the server's current chat history, so the client can determine whether it is lacking something
       * (3) The client will then answer whether it needs new chat lines
       * (4) The client's chat history length will be saved in a variable
       * (5) The server will then proceed to transmit any missing chat to the client
       * </p>
       */
      public void AcceptConnection() {
            // Timestamp for incoming messages
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            // Create a client socket with incoming request.
            Socket client = WaitForConnection();

            // Uses an idle thread for each attempt to listen.
            executor.execute(() -> {
                  try {
                        if (client != null) {
                              // These two will listen to the client
                              Scanner in = new Scanner(client.getInputStream());
                              PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);

                              if (in.hasNext()) {
                                    // (1) Gets the client's input message
                                    String msg = in.nextLine();

                                    // If (1) equals the input string defined in this class (plus some extra stuff at the beginning), only return the length of the chat history
                                    // This means that the client is connecting for the first time, not requesting a keep-alive
                                    if (msg.contains("#1" + REQUEST_STRING)) {
                                          out.println(GetChatHistoryLength());
                                    }

                                    // If (1) contains the input string defined in this class, the client would like to receive a keep-alive and its missing chat history
                                    else if (msg.contains(REQUEST_STRING)) {
                                          System.out.println(String.format("<%s>: %s poked", Thread.currentThread().getName(), client.getInetAddress().toString()));

                                          // (2) Return the length of the server's current chat history, so the client can determine whether it is lacking something
                                          out.println(GetChatHistoryLength());

                                          if (in.hasNext()) {
                                                // (3) The client will then answer whether it needs new chat lines
                                                boolean needsNewChat = in.nextBoolean();

                                                // If so, the server will request the client's current chat history length
                                                if (needsNewChat) {
                                                      System.out.println(String.format("<%s>: Writing chat to %s", Thread.currentThread().getName(), client.getInetAddress().toString()));

                                                      if (in.hasNext()) {
                                                            // (4) The client's chat history length will be saved here
                                                            int clientChatLength = in.nextInt();
                                                            String output = WriteChat(clientChatLength);
                                                            System.out.println("String to write: " + output);
                                                            // (5) The server will then proceed to transmit any missing chat to the client
                                                            out.println(output);
                                                      }
                                                } else {
                                                      System.out.println(client.getInetAddress() + "'s chat history is up-to-date.");
                                                }
                                          }
                                          // If the client's message does not contain the request string, it means the client is actually submitting a new message to the server
                                    } else {
                                          // Save that message to the server's chat history
                                          messagelist.add(String.format("<%s> - %s: %s", dateFormat.format(Date.from(Instant.now())), client.getInetAddress().getHostName(), msg));
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

            // Recursively restarts the listener.
            AcceptConnection();
      }

      /**
       * Will look for missing chat lines in the server's chat history, then build a string containing them.
       *
       * @param _clientChatLength The client's current chat history length as transmitted in <code>Server#AcceptConnection()</code>
       * @return A string containing all missing chat lines
       * @see Server#AcceptConnection()
       */
      private String WriteChat(int _clientChatLength) {
            ArrayList<String> newMessagelist = new ArrayList<>();
            StringBuilder sb = new StringBuilder();

            for (int i = GetChatHistoryLength() - 1; i >= _clientChatLength; i--) {
                  newMessagelist.add(messagelist.get(i));
            }

            for (String s : newMessagelist) {
                  sb.append(s);
                  sb.append("\n");
            }

            return sb.toString();
      }

      /**
       * <p>
       * Will accept any client requesting a connection with the server.
       * For use in <code>Server#AcceptConnection()</code>
       * </p>
       *
       * @return The client's socket
       * @see Server#AcceptConnection()
       */
      private Socket WaitForConnection() {
            Socket client = null;
            // As long as there is no client connection and the server hasn't retried too many times
            while (client == null) {
                  try {
                        System.out.println(String.format("<%s>: Listening..", Thread.currentThread().getName()));
                        client = serverSocket.accept();
                  } catch (IOException e) {
                        e.printStackTrace();
                  }
            }

            return client;
      }

      /**
       * <p>
       * Will carefully terminate the server socket's connection to its ip and port.
       * Might not end the program running the server correctly.
       * </p>
       */
      public static void CloseConnection() {
            try {
                  if (serverSocket != null) {
                        serverSocket.close();
                        System.out.println("Connection has been closed.");
                  } else {
                        System.out.println("Socket either hasn't been created or has already been closed.");
                  }
            } catch (IOException e) {
                  System.err.println("Socket couldn't be closed.");
                  e.printStackTrace();
            }
      }

      /**
       * Returns the server's chat history length.
       *
       * @return The server's chat history length.
       */
      int GetChatHistoryLength() {
            return messagelist.size();
      }

      /**
       * Returns the server's port.
       *
       * @return The server's port.
       */
      public int GetServerPort() {
            return SERVER_PORT;
      }

      /**
       * Returns the server's chat history.
       *
       * @return The server's chat history.
       */
      public ArrayList<String> GetMessageList() {
            return messagelist;
      }
}
