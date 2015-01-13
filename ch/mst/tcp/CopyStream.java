/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.mst.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Stram copy thread.
 * 
 * @author henning@mst.ch Heiko Henning
 */
abstract public class CopyStream implements Runnable {
    public static final int BUF_SIZE = 512;
    
    protected InputStream in;
    protected OutputStream out;
    
    
    public CopyStream(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }
    
    @Override
    public void run() {
        byte[] buf = new byte[BUF_SIZE];
        int count;
 
        try {
            while ((count = in.read(buf)) != -1) {
                out.write(buf, 0, count);
            }
        } catch (IOException ex) {
            this.onError(ex);
        }
    }
    
    /**
     * Will called in error case.
     * 
     * @param ex 
     */
    abstract public void onError(IOException ex);
}
