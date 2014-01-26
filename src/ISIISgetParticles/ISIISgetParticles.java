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
        for (File file:bmpFiles) {

            String fileName = file.getName();
            if ( verbose ) { System.out.println(fileName); }

            // open file
    		ImagePlus imp = IJ.openImage(file.getAbsolutePath());

            // remove extension
            String fileNameNoExt = fileName.substring(0, fileName.lastIndexOf('.'));
            // prepare destination
            if ( debug ) { Message.debug("outName = " + outName); }
            String outName = destDirName + "/" + fileNameNoExt;
            // save a copy of the original image
            if ( debug ) { IJ.save(imp, outName + "-0-orig.png"); }


            // threshold image
    		ImagePlus imp2 = new Duplicator().run(imp);
            IJ.setThreshold(imp2, 0, threshold);
            // TODO: set a fixed threshold. It seems something around 160-200 combined together with a large minimum particle size (several hundrer pixels) is a good compromize to keep transparent organisms as one particle and not be polluted by small particles
            // IJ.setAutoThreshold(imp, "MaxEntropy");
            // TODO: seems good but deserves more testing. Is much longer to compute
            Prefs.blackBackground = false;
            IJ.run(imp2, "Convert to Mask", "");
            if ( debug ) { IJ.save(imp2, outName + "-1-mask.png"); }


            // analyse particles
            // GUI scripting version
            IJ.run("Set Measurements...", "area mean standard modal min centroid center perimeter bounding fit shape feret's integrated median skewness kurtosis area_fraction stack redirect=None decimal=3");
            Analyzer.setRedirectImage(imp);
            IJ.run(imp2, "Analyze Particles...", "size=400-Infinity circularity=0.00-1.00 show=Nothing exclude clear");
            IJ.saveAs("Results", outName + ".txt");

            // analyse particles
            // programmatic version
            ResultsTable rt = new ResultsTable();
            rt.reset();
            int options = 0; // set all ParticleAanalyzer options to false
            int measurements = AREA+MEAN+CENTROID;
            // TODO: set more measurements
            ParticleAnalyzer pa = new ParticleAnalyzer(options, measurements, rt, 400, 1000000);
            // TODO: find a better definition of "infinity"
            Analyzer.setRedirectImage(imp);
            pa.analyze(imp2);
            int nParticles = rt.getCounter();
            if ( verbose ) { System.out.println("found " + nParticles + " particles"); }
            rt.saveAs(outName + ".csv");

        }

    }

}
