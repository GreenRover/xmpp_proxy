/*
 * Tcp server
 */
package ch.mst.tcp;

/**
 *
 * @author henning@mst.ch Heiko Henning
 */

import ch.mst.xmpp_reverse_proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.logging.Level;

public class Server implements Runnable {

    protected int          serverPort   = 5222;
    protected ServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;

    public Server(int port){
        this.serverPort = port;
    }

    @Override
    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        
        openServerSocket();
        while(! isStopped()){
            Socket serverInstanceSocket = null;
            try {
                serverInstanceSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped.") ;
                    return;
                }
                throw new RuntimeException(
                    "Error accepting client connection", e);
            }
            
            try {
                new Thread(
                    new ServerInstance(serverInstanceSocket)
                ).start();
            } catch (IOException e) {
                // Error while client handling.
            }
        }
        System.out.println("Server Stopped.") ;
    }


    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + this.serverPort, e);
        }
        
        xmpp_reverse_proxy.log(Level.INFO, "Start listening on port: " + Integer.toString(this.serverPort)); 
    }

}