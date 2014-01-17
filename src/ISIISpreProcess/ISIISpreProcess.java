package ISIISpreProcess;

// ImageJ functionality
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.*;    // for ImageProcessor and subclasses
import ij.plugin.*;     // for AVI_Reader
// import ij.io.*;         // for FileSaver
// file listing
import java.io.File;
import java.io.FilenameFilter;
// date computation
import java.util.Date;
import java.text.SimpleDateFormat;
// utility functions
import ISIISutils.Calc;
import ISIISutils.Message;

public class ISIISpreProcess {

    /**
     * Here we:
     * List all avi stacks in the working directory and considers them as a continuous string of images
     * Compute a moving average of the values of pixels along this string of images to compute a background
     * Remove the backgroud from the last image of the window
     * Compute the time of each image of the stack, getting the start time of the stack from its name
     * Save the modified image in a bmp file named according to its time
     *
     * @param args the command line arguments
     *        args[0] : source directory
     *        args[1] : destination directory
     */
    public static void main (String[] args) throws Exception {

        // Options
        //-------------------------------------------------------------------------
        // print progress messages
        final boolean verbose = true;
        // print debug messages
        final boolean debug = true;
        // moving window size
        int windowSize = 50;


        // Read arguments
        //-------------------------------------------------------------------------
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
        //-------------------------------------------------------------------------
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
        if ( aviFiles == null ) {
            Exception e = new Exception("Cannot list avi files in " + workDir.getAbsolutePath() );
            throw e;
        }
        int nbOfAviFiles = aviFiles.length;
        if ( verbose ) { System.out.println("Processing " + nbOfAviFiles + " AVI stacks"); }


        // Create the destination directory, if it does not exist already
        File destDir = new File(destDirName);
        if ( ! destDir.exists() ) {
            boolean success = destDir.mkdir();
            if ( ! success ) {
                Exception e = new Exception("Cannot create destination directory");
                throw e;
            }
        }

        // Initialize the moving window
        //-------------------------------------------------------------------------
        if ( verbose ) { System.out.println("Initializing background image"); }

        // make the window size odd if necessary
        if (windowSize % 2 == 0) {
            windowSize = windowSize + 1;
        }

        // open first avi file in a virtual stack
        // NB: if we use a non-virtual stack and run out of memory, AVI_reader will silently stop loading images and won't load the whole stack (only 1st GB)
        ImagePlus imp = AVI_Reader.openVirtual( aviFiles[0].getAbsolutePath() );
        ImageStack stack = imp.getStack();

        // make sure we have enough slices in the stack to fit the window
        int nbOfImagesInStack = stack.getSize(); // number of images in stack
        if ( windowSize > nbOfImagesInStack ) {
            Message.warning("reducing moving window size to fit in stack");
            windowSize = nbOfImagesInStack;
        }
        if ( debug ) { Message.debug("windowSize = " + windowSize); }

        // get size of images
        int w = stack.getWidth();   // image width in pixels
        int h = stack.getHeight();  // image height in pixels
        int imageSize = w * h;      // nb of pixels in image

        // prepare storage for all pixels in the window
        byte[][] windowPixels = new byte[imageSize][windowSize];
        // NB: we want to store the pixels of each slice as a column so that it is then easy to access simultaneously the array with the values of the first pixel of each slice (which will be windowPixels[0])

        // storage for pixels of one slice
        byte[] pixels;

        // prime windowPixels with the values at the begining of the first stack
        for (int i = 0; i < windowSize; i++) {
            // get the pixels from each slice
            pixels = (byte[]) stack.getPixels(1+i);
            // store them
            for (int j = 0; j < imageSize; j++) {
                windowPixels[j][i] = pixels[j];
            }
        }


        // Loop over all images (in all AVI stacks)
        //-------------------------------------------------------------------------

        // initialise a counter for the total number of images
        int imgCount = 0;
        // NB: using an int here is more practical because all other indexes are ints
        //     but it limits the number of images that can be treated in one pass to 2^32

        // loop over avi files
        for (int a = 0; a < (nbOfAviFiles - 1); a++) {
            // NB: stop at the before-the-last stack because we cannot compute the time of the images in the last stack

            // Open avi file in a virtual stack
            String aviName_current = aviFiles[a].getName();
            String aviPath_current = aviFiles[a].getAbsolutePath();
            if ( verbose ) { System.out.println("Processing stack " + aviName_current); }

            imp = AVI_Reader.openVirtual( aviPath_current );
            float[] ctable = imp.getCalibration().getCTable();

            stack = imp.getStack();

            nbOfImagesInStack = stack.getSize();


            // Compute the time difference between each slice in the stack
            // get the start time of the current and next stacks from their names

            String aviName_next = aviFiles[a+1].getName();
            // NB: this is why we stop at nbOfAviFiles - 1

            SimpleDateFormat dateFormatInName = new SimpleDateFormat("yyyyMMddHHmmss.SSS'.avi'");
            Date startTime_current = dateFormatInName.parse(aviName_current);
            Date startTime_next    = dateFormatInName.parse(aviName_next);

            // compute elapsed time in milliseconds
            long startTimeMsec_current = startTime_current.getTime();
            long startTimeMsec_next    = startTime_next.getTime();
            long elapsedMillisec = startTimeMsec_next - startTimeMsec_current;
            if ( debug ) { Message.debug("elapsedMillisec = " + elapsedMillisec); }

            // compute time increment per picture in milliseconds
            long timeStep = elapsedMillisec / nbOfImagesInStack;
            if ( debug ) { Message.debug("timeStep = " + timeStep); }


            // Loop over all slices of the stack
            //---------------------------------------------------------------------
            for (int i = 0; i < nbOfImagesInStack; i++) {
                if ( verbose ) {
                    System.out.println("image " + i + " / " + nbOfImagesInStack + " ; " + imgCount + " in total");
                }

                // compute time of this image (used to create the output name)
                long currentTimeMsec = startTimeMsec_current + i * timeStep;
                // reconvert those milliseconds into a Date object
                Date currentTime = new Date(currentTimeMsec);
                // and format it into a character string
                SimpleDateFormat dateFormatOutName = new SimpleDateFormat("yyyyMMddHHmmss_SSS");
                String outName = dateFormatOutName.format(currentTime);
                outName = destDirName + "/" + outName;
                if ( debug ) { Message.debug("outName = " + outName); }


                // get the pixels of the current slice
                pixels = (byte[]) stack.getPixels(i+1);
                // NB: stack slices indexes start at 1

                // compute position at which to store the current slice in the windowPixels array
                // no more than `windowSize` slices should be stored at the same time
                // when slice `windowSize`+1 is reached, it should be stored at position 1, this way, slice 1 is deleted because it was more than `windowSize` slices away and the new data comes in
                int ii = imgCount % windowSize;

                // prepare storage of the background image and result of background removal
                byte[] background = new byte[imageSize];
                float[] result = new float[imageSize];

                // loop over pixels of the current slice
                for (int j = 0; j < imageSize; j++) {
                    // store the pixel in the moving window array
                    windowPixels[j][ii] = pixels[j];

                    // compute background
                    // background[j] = Calc.median(windowPixels[j]);
                    // TODO: problem with the computation of the median background here, should not change for the first n slices but it does.
                    background[j] = Calc.mean(windowPixels[j]);
                    // results from mean are slightly better than median but also MUCH FASTER

                    // remove background
                    // method from the CalculatorPlus plugin, modified from Adam Greer (RSMAS)
                    // in macro language:
                    // run("Calculator Plus", "i1=stackname i2=ff operation=[Divide: i2 = (i1/i2) x k1 + k2] k1=235 k2=0 create");
                    float v1 = (float) (pixels[j] & 0xff);
                    float v2 = (float) (background[j] & 0xff);
                    v2 = (float) (v2!=0.0?v1/v2:0.0);
                    float k1 = (float) 255.0;
                    float k2 = (float) 0.0;
                    v2 = v2 * k1 + k2;
                    result[j] = v2;
                }

                // store pixels in an ImageProcessor
                FloatProcessor resultFP = new FloatProcessor(w, h, result);
                ImageProcessor resultIP = resultFP.convertToByte(false);
                // NB: false => do not scale when converting to bytes => cut lower grey levels to white
                ImagePlus resultIMG = new ImagePlus("result", resultIP);
                if ( debug ) {
                    ByteProcessor bp;
                    ImagePlus ip;

                    // save background
                    bp = new ByteProcessor(w, h, background);
                    ip = new ImagePlus("background", bp);
                    IJ.save(ip, outName + "-0-background.jpg");

                    // save orignal image
                    bp = new ByteProcessor(w, h, pixels);
                    ip = new ImagePlus("orig", bp);
                    IJ.save(ip, outName + "-1-orig.jpg");

                    // save resulting image
                    IJ.save(resultIMG, outName + "-2-divided.jpg");
                }

                // invert image
                // resultIMG.getProcessor().invert();
                // if ( debug ) { IJ.save(resultIMG, outName + "-3-inverted.jpg"); }

                // normalize image (make gray level range from 0 to 255 for all images)
                // add a tolerance (saturate a given proportion of pixels)
                // IJ.run(resultIMG, "Enhance Contrast...", "saturated=0.1 normalize");
                // or
                ContrastEnhancer ce = new ContrastEnhancer();
                ce.setNormalize(true);
                ce.stretchHistogram(resultIMG, 0.005);
                if ( debug ) { IJ.save(resultIMG, outName + "-4-normalised.jpg"); }

                // save result
                IJ.save(resultIMG, outName + ".bmp");
                // or
                // FileSaver fs = new FileSaver(resultIMG);
                // fs.saveAsBmp(outName + ".bmp");
                // not sure there is performance advantage to this solution although there should be

                // increase counter
                imgCount = imgCount + 1;

            } // loop over stack frames
        } // loop over avi files

    }

}
