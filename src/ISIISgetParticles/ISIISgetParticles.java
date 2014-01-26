package ISIISgetParticles;

// file listing
import java.io.File;
import java.io.FilenameFilter;
// utility functions
import ISIISutils.Message;
// ImageJ functionality
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.process.*;
import ij.process.ImageProcessor;
import static ij.measure.Measurements.*;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import static ij.process.ImageProcessor.BLACK_AND_WHITE_LUT;
import ij.io.*;         // for FileSaver


/**
 *
 * @author jiho
 */
public class ISIISgetParticles {

    /**
     * Here we:
     * List all BMP files in the working directory
     * Threshold each image
     * Measure particles characteristics and store the result in a text file
     * Extract an image containing each particle
     *
     * @param args the command line arguments
     *        args[0] : source directory
     *        args[1] : destination directory
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {

        // Options
        //-------------------------------------------------------------------------
        // print progress messages
        final boolean verbose = true;
        // print debug messages
        final boolean debug = true;
        // thresholding value
        int threshold = 205;


        // Read arguments
        //-------------------------------------------------------------------------
        if ( args.length != 2 ) {
            Exception e = new Exception("Not enough arguments");
            throw e;
        }
        // first argument: working directory (where the BMP files are)
        String workDirName = args[0];
        // second argument: destination directory (where the results should be written)
        String destDirName = args[1];
        // String thresholdString = args[2];
        // int threshold = Integer.parseInt(thresholdString);
        // if ( debug ) { Message.debug("threshold = " + threshold); }

        // List BMP files in working directory
        //-------------------------------------------------------------------------
        // create filename filter for bmp files
        FilenameFilter bmpFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if(name.lastIndexOf('.')>0) {
                    // get extension
                    int lastIndex = name.lastIndexOf('.');
                    String str = name.substring(lastIndex);
                    // match bmp extension
                    if(str.equals(".bmp")) {
                        return true;
                    }
                }
                return false;
            }
        };
        // convert workding directoy name to Java File object
        File workDir = new File(workDirName);
        // list all bmp files
        File[] bmpFiles = workDir.listFiles(bmpFilter);
        if ( bmpFiles == null ) {
            Exception e = new Exception("Cannot list bmp files in " + workDir.getAbsolutePath() );
            throw e;
        }
        int nbOfFiles = bmpFiles.length;
        if ( verbose ) { System.out.println("Processing " + nbOfFiles + " bmp images"); }

        // Create the destination directory, if it does not exist already
        File destDir = new File(destDirName);
        if ( ! destDir.exists() ) {
            boolean success = destDir.mkdir();
            if ( ! success ) {
                Exception e = new Exception("Cannot create destination directory");
                throw e;
            }
        }

        // Loop over all images
        //-------------------------------------------------------------------------

        // initialize results table
        ResultsTable rt = new ResultsTable();
        rt.reset();

        // initialize counters
        int n_previous = 1;
        int n_current = 1;

        for (File file:bmpFiles) {

            String fileName = file.getName();
            if ( verbose ) { System.out.print(fileName+" "); }

            // open file
    		ImagePlus imp = IJ.openImage(file.getAbsolutePath());

            // remove extension
            String fileNameNoExt = fileName.substring(0, fileName.lastIndexOf('.'));
            // prepare destination
            String outName = destDirName + "/" + fileNameNoExt;
            // save a copy of the original image
            if ( debug ) { IJ.save(imp, outName + "-0-orig.bmp"); }


            // threshold image
            ImagePlus imp2 = new Duplicator().run(imp);
            ImageProcessor ip2 = imp2.getProcessor();
            ip2.setThreshold(0, threshold, BLACK_AND_WHITE_LUT);
            if ( debug ) { IJ.save(imp2, outName + "-1-mask.bmp"); }

            // analyse particles
            int options = 0; // set all ParticleAanalyzer options to false
            // TODO investigate what those optins are
            int measurements = LABELS|AREA|AREA_FRACTION|CENTER_OF_MASS|CENTROID|CIRCULARITY|ELLIPSE|FERET|INTEGRATED_DENSITY|KURTOSIS|LIMIT|MAX_STANDARDS|MEAN|MEDIAN|MIN_MAX|MODE|PERIMETER|RECT|SHAPE_DESCRIPTORS|SKEWNESS|STD_DEV;
            // TODO select measurements
            // TODO compare with Zooprocess measurements

            ParticleAnalyzer pa = new ParticleAnalyzer(options, measurements, rt, 400, imp.getWidth()*imp.getHeight());
            // TODO make the lower size limit configurable
            // TODO: find a better/more efficient definition of a large particle
            Analyzer.setRedirectImage(imp);
            pa.analyze(imp2);

            n_current = rt.getCounter();
            if ( verbose ) { System.out.println("+" + (n_current - n_previous) + " " + n_current); }
            // TODO check computation of number of particles. Does rt have 1 or 2 lines when the first particle is added?
            if ( debug ) { rt.saveAs(outName + ".csv"); }


            // extract particles images
            ImageProcessor ip = imp.getProcessor();
            ImageProcessor ipROI = imp.getProcessor();

            for (int i = n_previous; i < n_current; i++) {
                // read coordinates of particle
                int x = (int) rt.getValueAsDouble(13-2, i);
                int y = (int) rt.getValueAsDouble(14-2, i);
                int w = (int) rt.getValueAsDouble(15-2, i);
                int h = (int) rt.getValueAsDouble(16-2, i);
                if ( debug ) { Message.debug("x="+x+" y="+y+" w="+w+" h="+h); }

                // widen window around particle
                int factor = 2;
                // TODO make this configurable
                // do not make it too large
                int padX = Math.min(200, factor * w / 2);
                int padY = Math.min(200, factor * h / 2);

                // extract new ImageProcessor with current Region Of Interest (ROI)
                ip.setRoi(x - padX, y - padY, w + 2*padX, h + 2*padY);
                // NB: setRoi is clever enough that it does not go over the image boundaries
                ipROI = ip.crop();

                // draw marks around the particle
                // NB: we have to recompute the (x,y) corner wihin the new image
                int newX = x;
                int newY = y;
                if ( x > padX ) {
                    newX = padX;
                }
                // TODO try to find a better way to do this
                if ( y > padY ) {
                    newY = padY;
                }
                ipV.drawRect(newX, newY, w, h);

                // save an image of the particle
                ImagePlus impV = new ImagePlus("", ipV);
                FileSaver fs = new FileSaver(impV);
                fs.saveAsBmp(outName + "-" + i + ".bmp");
                // TODO rename the file using MD5 hash or something similar
                //      http://stackoverflow.com/questions/304268/getting-a-files-md5-checksum-in-java

                // clear ROI
                ip.resetRoi();
            }

            n_previous = n_current;

        }
        // save complete results table
        rt.saveAs(destDirName + "/particles.csv");

    }

}
