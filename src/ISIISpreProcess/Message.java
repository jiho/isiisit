/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ISIISpreProcess;

/**
 *
 * @author jiho
 */
public class Message {
    public static void debug(String s) {
        System.out.println("DEBUG " + s);
    }

    public static void warning(String s) {
        System.out.println("WARNING " + s);
    }

    public static void error(String s) {
        System.out.println("ERROR " + s);
        System.err.println("ERROR " + s);
    }
}
