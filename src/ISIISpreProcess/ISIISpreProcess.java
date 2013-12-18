package ISIISpreProcess;

import ij.IJ;
import ij.io.*;
import ij.ImagePlus;
import ij.ImageStack;
import java.io.File;
import java.io.FilenameFilter;
import ij.process.*;
import ij.gui.*;
import ij.plugin.Duplicator;
import java.awt.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import static java.lang.Boolean.*;
import java.util.Arrays;
import java.util.Date;
import java.text.SimpleDateFormat;



public class ISIISpreProcess {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // get path to directory as first argument
        // String path = args[0];
        // hard code it for now
        String path = "/Users/faillettaz/Desktop/ZOOPROCESS/Stacks/Test";
        File dir = new File(path);

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
                
        // list all avi files
        File[] aviFiles = dir.listFiles(aviFilter);
        
        // loop over avi files
        for (File avi:aviFiles) {
            
            // get path to file
            String aviFile = avi.getAbsolutePath();
            // System.out.println(aviFile);
            
            // open file
            ImagePlus imp;
            imp = AVI_Reader.open(aviFile, TRUE);
            // NB: if we use a non-virtual stack (second arg = FALSE) and run
            //     out of memory, AVI_reader will silently stop loading images
            //     and won't load the whole stack

            // get the image as a stack
            ImageStack stack;
            stack = imp.getStack();
            
            // get stack properties
            int nMax = stack.getSize(); // number of images
            nMax = 150;
            int w = stack.getWidth();   // image width in pixels
            int h = stack.getHeight();  // image height in pixels
            int dim = w * h;
        
            // // check type of image
            // // 0 = GRAY8
            // // 1 = GRAY16
            // // 4 = COLOR_RGB
            // int type = imp.getType();
            // System.out.println(type);
            
            // prepare array to store all pixels of the stack
            // number of slices to work with
            int n = 50;
            // make it odd if necessary
            if (n % 2 == 0) {
                n = n + 1;
            }
            // make sure we have enough slices
            if (n > nMax) {
                n = nMax;
            }
            System.out.println("n="+n);
            
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
            
            byte[] background;
            background = new byte[dim];
            
//            float[] result;
//            result = new float[dim];

            int[] result;
            result = new int[dim];

            for (int i=0; i < nMax; i++) {
                System.out.println("image "+i);
                pixels = (byte[]) stack.getPixels(1+i);

                // compute position at which to store the current slice in the allPixels array
                // no more than n slices should be stored at the same time
                // when slice n+1 is reached, it should be stored at position 1, this way, 
                // slice 1 is deleted because it was more than n slices away and the new data comes in
                int ii = i % n;
//                System.out.println("position "+ii);
        
        //// CREATE THE TABLE WITH CUMSUM AND NB OF FRAMES ////
        
        // get the number of files in the folder to create the array
        int fileNumber = new File(path).listFiles(aviFilter).length;
        
        // create the array of the length = nb of files and 2 columns (nb of image per stack and cumsum)
        int [][] dataTable = new int[fileNumber][2];
        System.out.println("filenumber= "+fileNumber);
                
        int aMax = fileNumber-1;
        System.out.println("aMax= "+aMax);

       
        
        for (int a = 0; a < aMax; a++) 
                
                for (int j = 0; j < dim; j++) {
                    allPixels[j][ii] = pixels[j];
                //for (File avi:aviFiles) 
                {

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
                    //     and won't load the whole stack

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

     
                    // prepare array to store all pixels of the stack
                    // number of slices to work with
                    int n = 50;
                    // make it odd if necessary
                    if (n % 2 == 0) {
                        n = n + 1;
                    }
                    // make sure we have enough slices
                    if (n > nMax) {
                        n = nMax;
                    }
                    System.out.println("n="+n);

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

                    byte[] background;
                    background = new byte[dim];
                    
//                    background[j] = Calc.median(allPixels[j]);
                    // TODO problem with the computation of the background here, should not change for the first n slices but it does.
                    background[j] = Calc.mean(allPixels[j]);
                
                    // background subtraction from Adam's script using the CalculatorPlus plugin
//                    float v1 = pixels[j];
//                    float v2 = background[j];
//                    v2 = (float) (v2!=0.0?v1/v2:0.0);
//                    float k1 = (float) 120.0;
//                    float k2 = (float) 0.0;
//                    v2 = v2*k1 + k2;
//                    result[j] = v2;
                
                    //result[j] = pixels[j] - background[j];
                }
                
                // compute correction
                ByteProcessor ip;
                ByteProcessor back;
                ip = new ByteProcessor(w, h, pixels);
                back = new ByteProcessor(w, h, background);
                
                ImagePlus cImg;
                FileSaver fs;

                cImg = new ImagePlus("orig", ip);
                fs = new FileSaver(cImg);
                fs.saveAsJpeg(avi.getAbsolutePath()+"-"+(1+i)+"-orig.jpg");

                cImg = new ImagePlus("back", back);
                fs = new FileSaver(cImg);
                fs.saveAsJpeg(avi.getAbsolutePath()+"-"+(1+i)+"-back.jpg");
                
                ip.copyBits(back, 0, 0, Blitter.SUBTRACT);
                ip.invert();
//                ip.threshold(125);
                
                // export the corrected image
//                ByteProcessor bp;
//                bp = new ByteProcessor(w, h, pixels);
//                FloatProcessor fp;
//                fp = new FloatProcessor(w, h, result);
                cImg = new ImagePlus("sub", ip);
                fs = new FileSaver(cImg);
                fs.saveAsJpeg(avi.getAbsolutePath()+"-"+(1+i)+"-sub.jpg");
                    for (int i=0; i < nMax; i++) {
                        System.out.println("image "+i);
                        pixels = (byte[]) stack.getPixels(1+i);

                        // compute position at which to store the current slice in the allPixels array
                        // no more than n slices should be stored at the same time
                        // when slice n+1 is reached, it should be stored at position 1, this way, 
                        // slice 1 is deleted because it was more than n slices away and the new data comes in
                        int ii = i % n;
                        //System.out.println("position "+ii);


                        for (int j = 0; j < dim; j++) {
                            allPixels[j][ii] = pixels[j];

                            
                            // Compute background from the mean
                            background[j] = Calc.mean(allPixels[j]);
                            
                            // from the median -> much slower and not any better
                            //background[j] = Calc.median(allPixels[j]);
                            
                            // background subtraction from Adam's script using the CalculatorPlus plugin -> same than with mean, not faster
                            //float v1 = pixels[j];
                            //float v2 = background[j];
                            //v2 = (float) (v2!=0.0?v1/v2:0.0);
                            //float k1 = (float) 120.0;
                            //float k2 = (float) 0.0;
                            //v2 = v2*k1 + k2;
                            //result[j] = v2;

                            //result[j] = pixels[j] - background[j];
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
        }
    }
}


// imp = new Duplicator().run(imp);
// IJ.setAutoThreshold(imp, "Triangle");
// IJ.run(imp, "Convert to Mask", "method=Triangle background=Light calculate");
// IJ.run("Set Measurements...", "area mean standard modal min centroid center perimeter bounding fit shape feret's integrated median skewness kurtosis area_fraction stack redirect="+avi.getName()+" decimal=3");
// IJ.run(imp, "Analyze Particles...", "size=10-Infinity circularity=0.00-1.00 show=Nothing display clear record in_situ stack");
// IJ.saveAs("Results", avi.getName()+".txt");







    /**
     * @param args the command line arguments
     */
//     public static void printRow(int[] row) {
//        for (int i : row) {
//            System.out.print(i);
//            System.out.print("\t");
//        }
//        System.out.println();
//    }
// 