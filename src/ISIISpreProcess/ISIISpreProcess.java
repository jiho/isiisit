package ISIISpreProcess;

// ImageJ functionality
import ij.io.*;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.*;
import ij.plugin.*;
// file listing
import java.io.File;
import java.io.FilenameFilter;
// date computation
import java.util.Date;
import java.text.SimpleDateFormat;

public class ISIISpreProcess {

    /**
     * @param args the command line arguments
     */
    public static void main (String[] args) throws Exception
    {
        
        // Options
        // print progress messages
        final boolean verbose = true;
        // print debug messages
        final boolean debug = true;
        

        // Read arguments
        if ( args.length != 2 ) {
            Exception e = new Exception("Two arguments required");
            throw e;
        }
        // first argument: working directory (where the avi files are)
        String workDirName = args[0];
        // String workDirName = "/Users/faillettaz/Desktop/ZOOPROCESS/Stacks/";
        // second argument: destination directory (where the resulting images should be written)
        String destDirName = args[1];


        // List AVI files in working directory
        // create filename filter for AVI files
        FilenameFilter aviFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if(name.lastIndexOf('.')>0) {
                    // get extension
                    int lastIndex = name.lastIndexOf('.');
                    String str = name.substring(lastIndex);
                    // match avi extension
                    if(str.equals(".avi")) {
                        return true;
                    }
                }
                return false;
            }
        };
        // convert workding directoy name to Java File object
        File workDir = new File(workDirName);
        // list all avi files
        File[] aviFiles = workDir.listFiles(aviFilter);
        int nbOfAviFiles = aviFiles.length;
        if ( verbose ) { System.out.println("Processing " + nbOfAviFiles + " AVI stacks"); }
        
 
        
      
        //// CREATE THE TABLE WITH CUMSUM AND NB OF FRAMES ////
        
        // get the number of files in the folder to create the array
        int fileNumber = new File(path).listFiles(aviFilter).length;
        // create the array of the length = nb of files and 2 columns (nb of image per stack and cumsum)
        int [][] dataTable = new int[fileNumber][2];
        System.out.println("filenumber= "+fileNumber);

        
        int aMax = fileNumber-1; // -1 to pass it as array index
        System.out.println("aMax= "+aMax);
   
        
        // loop over avi files          
        for (int a = 0; a < aMax; a++) 
                
                //for (File avi:aviFiles) 
                {

            
            
            
                    // Create the directory to receive the files
        
                    String dirFin = args[1]; //"../../media/raid/unstacked/" ;
                    // System.out.println(dirFin);
        
                    String nameFolder = dirFin+"HDR"+aviFiles[0].getName().substring(0, 14); 

                    //System.out.println(nameFolder);

                    File directory = new File(nameFolder);
                    directory.mkdir();

                    
                    
                    // print the a index
                    System.out.print(a);

                    // get path to file
                    String aviFile = aviFiles[a].getAbsolutePath();
                    System.out.println(aviFile);

                    // open file
                    ImagePlus imp;
                    imp = AVI_Reader.openVirtual(aviFile);
                    // NB: if we use a non-virtual stack (second arg = FALSE) and run
                    //     out of memory, AVI_reader will silently stop loading images
                    //     and won't load the whole stack (only 1st GB)

                    // get the image as a stack
                    ImageStack stack;
                    stack = imp.getStack();

                    // get stack properties
                    int nMax = stack.getSize(); // number of images

                    //nMax = 150;
                    int w = stack.getWidth();   // image width in pixels
                    int h = stack.getHeight();  // image height in pixels
                    int dim = w * h;

                    //System.out.println("dim="+dim);



                    // store nMax of each stacks in an array and calculate the cumsum
                    // fill with the stack size
                    dataTable[a][0] = nMax; 

                    // get the cumsum 
                    if (a == 0) {
                         dataTable[a][1] = 0;
                    }
                    else {
                         dataTable[a][1] = dataTable[a-1][1]+nMax;
                    }


                   //System.out.println(dataTable[0][0]);
                   //System.out.println(dataTable[1][0]);
                   //System.out.println(dataTable[0][1]);
                   //System.out.println(dataTable[1][1]);
                   //System.out.println(dataTable[2][1]);




                    //// CREATE THE MOVING WINDOWS TO SUBSTRACT THE BACKGROUND ////
                    int n = 50;
                    // make it odd if necessary
                    if (n % 2 == 0) {
                        n = n + 1;
                    }
                    // make sure we have enough slices
                    //nMax = dataTable[a][0];
                    if (n > nMax) {
                        n = nMax;
                    }
                    //System.out.println("n="+n);

                    // storage for all pixels of n slices
                    byte[][] allPixels;
                    allPixels = new byte[dim][n];
                    // NB: we want to store the pixels of each slice as a column so that
                    //     it is then easy to access simultaneously the array with the
                    //     values of the first pixel of each slice (which will be
                    //     allPixels[1])

                    // storage for pixels of one slice
                    byte[] pixels;

                    // prime allPixels with the values at the begining of the stack
                    for (int i = 0; i < n; i++) {
                        // get the pixels from each slice
                        pixels = (byte[]) stack.getPixels(1+i);
                        // store them
                        for (int j = 0; j < dim; j++) {
                            allPixels[j][i] = pixels[j];
                        }
                    }

                    // loop over all slices and for each:
                    // . get pixel data from the surrounding slices
                    // . compute the background (mean, median, ...)
                    // . substract the background from the focal slice

     

                    //pixels = (byte[]) stack.getPixels(430);
                    System.out.println("n= "+n);






                    //String date_string = "2012-10-27 23:45:01.656";
                    // dates of the current stack and the next one 
                    String name_ini = aviFiles[a].getName();
                    String name_end = aviFiles[a+1].getName();
                    //System.out.println("name ini= "+name_ini);
                    //System.out.println("name end= "+name_end);

                    // Get parent path to save them at the end
                    //String parent_path = avi.getParent(); 
                    // Don't forget to add a "/" after it
                    //System.out.println("path= "+parent_path); 

                    // Split the stack names
                    String name_ini_split[]= name_ini.split("\\.");
                    //System.out.println(java.util.Arrays.toString(name_ini.split("\\.")));

                    String name_end_split[]= name_end.split("\\.");
                    //System.out.println(java.util.Arrays.toString(name_end.split("\\.")));

                    // Get the 1st (datetime) and 2nd parts (millisec) 
                    String date_ini = name_ini_split[0];
                    String ms_ini = name_ini_split[1];

                    String date_end = name_end_split[0];
                    String ms_end = name_end_split[1];
                    //String format = name_end_split[2]; //Should always be "avi"

                    //System.out.println("date ini= "+date_ini);
                    //System.out.println("sec ini= "+ms_ini);
                    //System.out.println("date end= "+date_end);
                    //System.out.println("sec end= "+ms_end);
                    //System.out.println("format= "+format);

                    // Paste them back together in a string
                    String stack_name_ini = date_ini+ms_ini;
                    //System.out.println("stack ini= "+stack_name_ini);

                    String stack_name_end = date_end+ms_end;
                    //System.out.println("stack end= "+stack_name_end);

                    // define a format to parse these dates
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");


                    // LOOP OVER THE STACK FRAMES 
                    for (int i=dataTable[a][1]; i < dataTable[a][1]+dataTable[a][0]; i++) {


                        System.out.println("image "+(i+1-(dataTable[a][1])));
                        System.out.println("i= "+i); // Needs to be 1, so add +1
                        pixels = (byte[]) stack.getPixels(i+1-(dataTable[a][1])); // Needs to be 1 too

                        // compute position at which to store the current slice in the allPixels array
                        // no more than n slices should be stored at the same time
                        // when slice n+1 is reached, it should be stored at position 1, this way, 
                        // slice 1 is deleted because it was more than n slices away and the new data comes in
                        int ii = i % n; //(i-(dataTable[a][1])) % n; 
                        //System.out.println("position "+ii); 

                    byte[] background;
                    background = new byte[dim];

                    float[] result;
                    result = new float[dim];

                    //int[] result;
                    //result = new int[dim];

                        // Run the loop over the stack pixels
                        for (int j = 0; j < dim; j++) {
                            allPixels[j][ii] = pixels[j];

                            //background[j] = Calc.median(allPixels[j]);
                            // TODO problem with the computation of the background here, should not change for the first n slices but it does.
                            background[j] = Calc.mean(allPixels[j]); // Results from mean are slightly better than median but also MUCH FASTER

//                            // background subtraction from Adam's script using the CalculatorPlus plugin
//                            float v1 = pixels[j];
//                            float v2 = background[j];
//                            v2 = (float) (v2!=0.0?v1/v2:0.0);
//                            float k1 = (float) 235.0;
//                            float k2 = (float) 0.0;
//                            v2 = v2*k1 + k2;
//                            result[j] = v2;
//                        
//                            //result[j] = pixels[j] - background[j];

                        }


                      // compute correction
                      ByteProcessor ip;
                      ByteProcessor back;
                      ip = new ByteProcessor(w, h, pixels);
                      back = new ByteProcessor(w, h, background);





                      // Pass the dates to a date format and parse them
                      Date d_ini = formatter.parse(stack_name_ini);
                      Date d_end = formatter.parse(stack_name_end);

                      // convert this Date into milliseconds from 1970-01-01
                      long millisec_ini = d_ini.getTime();
                      long millisec_end = d_end.getTime();

                      //System.out.println("ms.d= "+millisec_ini);

                      // calculate the time step 
                      long millisec_frame = millisec_ini+                   //time stack start
                              (i-(dataTable[a][1]))*                        //iteration from 1 to stack length  
                              (millisec_end-millisec_ini)/dataTable[a][0];  // time between start and end
                      //System.out.println("ms_frame= "+millisec_frame);


                      // reconvert those milliseconds into a Date object
                      Date d2 = new Date(millisec_frame);
                      //System.out.println("ms400= "+ms2);
                      // and format it into a character string
                      String datetime_frame = formatter.format(d2);

                      // print both strings
                      //System.out.println("date orig= "+d_ini);
                      //System.out.println("date + 400ms = "+datetime_frame);

                        
                      String date = datetime_frame.substring(0, 14);
                      String ms = datetime_frame.substring(14, 17);
                      //System.out.println("date= "+date);
                      //System.out.println("ms ="+ms);
                      

                      ImagePlus cImg;
                      FileSaver fs;

                      //cImg = new ImagePlus("orig", ip);
                      //fs = new FileSaver(cImg);
                      //fs.saveAsJpeg(avi.getParent()+"/"+datetime_frame+"-orig.bmp");


                      //cImg = new ImagePlus("back", back);
                      //fs = new FileSaver(cImg);
                      //fs.saveAsJpeg(avi.getParent()+"/"+datetime_frame+"-back.bmp");


                      // Substracted the back from the original image
                      back.copyBits(ip, 0, 0, Blitter.SUBTRACT);
                      //ip.invert();
                      //back.threshold(35);

                      // TEST WITH INVERTED SRC (=ip) AND DEST (=back) --> better !
                      cImg = new ImagePlus("back", back);
                      fs = new FileSaver(cImg);
                      //fs.saveAsBmp(aviFiles[a].getParent()+"/unstacked/"+date+"_"+ms+".bmp");
                      //System.out.println(stack.getSize());
                      fs.saveAsBmp(nameFolder+"/"+date+"_"+ms+".bmp");


                      } // loop over stack frames
         } // loop over avi files
    }
}





            // // add the median as the last slice
            // System.out.println(stack.getSize());
            // stack.addSlice("Median", stackMedian);
            // System.out.println(stack.getSize());
            // // move to the last slice
            // imp.setSliceWithoutUpdate(stack.getSize());
            
//            ByteProcessor bp;
//            bp = new ByteProcessor(w, h, background);
//            
//            ImagePlus backgroundImg;
//            backgroundImg = new ImagePlus(" image", bp);
//            
//            // ImageProcessor ip;
//            // ip = new ImageProcessor
//            
//         
//                 
//            // move to the slice
//            // imp.setSliceWithoutUpdate(i);
//
//
//             // export slice as individual image
//              FileSaver fs;
//              fs = new FileSaver(backgroundImg);
//              fs.saveAsTiff(avi.getAbsolutePath()+"-"+"back"+".tif");
                
  