/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ISIISpreProcess;

import java.util.Arrays;

/**
 *
 * @author jiho
 */
public class Calc {
  
  public static byte median(byte[] l) {
    
    Arrays.sort(l);
    int middle = l.length / 2;
    if (l.length % 2 == 0)
    {
      byte left = l[middle - 1];
      byte right = l[middle];
      return (byte) ((left + right) / 2);
    }
    else
    {
      return l[middle];
    }
  }
  
  public static byte mean(byte[] l) {
    
      int sum = 0;
      for (int i = 0; i < l.length; i++) {
          sum += 0xff & l[i];
      }
      byte m;
      m = (byte) ((sum/l.length) & 0xff);
      return m;
  }
}
