// MotionDetection.java
// Copyright 2013 Rick Fluck
import java.awt.Dimension;
import java.awt.Point;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
public class MotionDetection {
  private static final int MIN_PIXELS = 100;
       // minimum number of non-black pixels needed for COG calculation
  private static final int LOW_THRESHOLD = 64;
  private static final int MAX_PTS = 5;
  private Mat prevImg, currImg, diffImg;     // grayscale images (diffImg is bi-level)
  private Dimension imDim = null;    // image dimensions
  private Point[] cogPoints;   // array for smoothing COG points
  private int ptIdx, totalPts;
   
  public MotionDetection(Mat firstFrame) {
    if (firstFrame.empty()) {
      System.out.println("No frame to initialize motion detector");
      System.exit(1);
    }
    System.out.println("Initializing OpenCV motion detector...");
    imDim = new Dimension( firstFrame.width(), firstFrame.height() );
    cogPoints = new Point[MAX_PTS];
    ptIdx = 0;
    totalPts = 0;
    prevImg = convertFrame(firstFrame);
    currImg = null;
    diffImg = new Mat();  
  } 
  public void calcMove(Mat currFrame) {
     // use a new image to create a new COG point
   
      if (currFrame == null) {
          System.out.println("Current frame is null");
          return;
      }
      if (currImg != null)  // store old current as the previous image
          prevImg = currImg;
      currImg = convertFrame(currFrame);
         // calculate absolute difference between currFrame & previous image;
         // large value means movement; small value means no movement
      Core.absdiff(currImg, prevImg, diffImg);
            
      Imgproc.threshold(diffImg, diffImg, LOW_THRESHOLD, 255, Imgproc.THRESH_BINARY);
      Point cogPoint = findCOG(diffImg);
      if (cogPoint != null) {    // store in points array
          cogPoints[ptIdx] = cogPoint;
          ptIdx = (ptIdx+1)%MAX_PTS;   // the index cycles around the array
          if (totalPts < MAX_PTS) totalPts++;
      }
  } 
  public Mat getCurrImg() {  
      return currImg;  
  }
  public Mat getDiffImg() {  
      return diffImg;  
  }
  public Dimension getSize() {  
      return imDim;  
  }
  private Mat convertFrame(Mat img) {
  /* Conversion involves: blurring, converting color to grayscale, and equalization */
      Mat mRgba = new Mat();
      Mat grayImg = new Mat();
      img.copyTo(mRgba); 
      img.copyTo(grayImg);
    // convert to gray scale
      Imgproc.cvtColor(mRgba, grayImg, Imgproc.COLOR_BGR2GRAY); 
    // blur image to get reduce camera noise
      Imgproc.GaussianBlur(grayImg, grayImg, new Size(9,9),0,0); 
    // spread out the gray scale range       
      Imgproc.equalizeHist( grayImg, grayImg );
      return grayImg;
  }
  private Point findCOG(Mat diffImg) {
  /*  If there are enough non-black pixels in the difference image
      (non-black means a difference, i.e. movement), then calculate the moments,
      and use them to calculate the (x,y) center of the white areas.
      These values are returned as a Point object. */
   
    Point pt = null;
    int numPixels = Core.countNonZero(diffImg);   // non-zero (non-black) means motion
    if (numPixels > MIN_PIXELS) {
      
      Moments moments = new Moments();
      moments = Imgproc.moments(diffImg, true);    // 1 == treat image as binary (0,255) --> (0,1)
      double m00 = moments.get_m00();
      double m10 = moments.get_m10();;
      double m01 = moments.get_m01();
      if (m00 != 0) {   // create XY Point
        int xCenter = (int) Math.round(m10/m00);
        int yCenter = (int) Math.round(m01/m00);
        pt = new Point(xCenter, yCenter);
      }
    }
    return pt;
  }  
   
  public Point getCOG() {
  /* return average of points stored in cogPoints[],
     to smooth the position */
      if (totalPts == 0)
      return null;
      int xTot = 0;
      int yTot = 0;
      for(int i=0; i < totalPts; i++) {
          xTot += cogPoints[i].x;
          yTot += cogPoints[i].y;
    }
    return new Point( (int)(xTot/totalPts), (int)(yTot/totalPts));
  }
}
