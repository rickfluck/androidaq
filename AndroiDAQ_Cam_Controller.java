//AndroiDAQ_Cam_Controller.java
// Copyright Controlled Capture Systems, Rick Fluck 2013-2014 
/* Portions of code are credited to Andrew Davison, July 2013
 * Uses AndroiDAQ module and various firmware changes to control one  
 * servo and one stepper motor
 * Uses face detection and motion detection using JavaCV by Samuel Audet 
 * AndroiDAQ information at: http://www.controlcapture.com/androiddaqmod
 */
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.highgui.VideoCapture;
import org.opencv.highgui.Highgui;
public class AndroiDAQ_Cam_Controller extends JPanel implements Runnable {
    private static final long serialVersionUID = 1L;
    // dimensions of each image; the panel is the same size as the image 
    private static final int WIDTH = 640;  
    private static final int HEIGHT = 480;
    private static final int DELAY = 45;  // time (ms) between redraws of the panel
    //private static final int DETECT_DELAY = 150;   // time (ms) between each face detection
    private static final String CROSSHAIRS_FNM = "/crosshairs.png";
    private static final int MIN_MOVE_REPORT = 3;    // for reporting a move
     
    private static final int HALF_HORIZONTAL = 320; //Makes up for crosshair size
    private static final int HALF_VERTICAL = 240;  //Makes up for crosshair size
    private static final int CAMERA_ID = 0;
    private Rectangle faceRect;     // holds the coordinates of the highlighted face
    private boolean faceDetected = false;
    private volatile boolean isRunning;
    private volatile boolean isFinished;
        // used for the average ms snap time information
    private int imageCount = 0;
    private Font msgFont;
    Rect rect;
    private Point prevXYPoint = null; // holds the coordinates of the motion COG
    private Point xyPoint = null; 
    private BufferedImage crosshairs;
    private AndroiDAQ_Comm serial;
    MotionDetection md;
    FaceDetection fd;
   
    private BufferedImage image; 
    Mat webcam_image;
    int yfactor;
    int prevYfactor;
    int xStep;
    int yStep;
    long detectDuration;
    boolean trackFace = false;
    boolean motionDetect = false;
    boolean processing = false;
        Thread thread = new Thread(this);
     
  public AndroiDAQ_Cam_Controller() {
    setBackground(Color.LIGHT_GRAY);
    msgFont = new Font("SansSerif", Font.BOLD, 18);
    // load the crosshairs image (a transparent PNG)
    crosshairs = loadImage(CROSSHAIRS_FNM);
    faceRect = new Rectangle();
    thread.setDaemon(true);
    thread.start();   // start updating the panel's image
  } 
  private BufferedImage loadImage(String imFnm) {
  // return an image
    BufferedImage image = null;
    try {
      image = ImageIO.read(this.getClass().getResource(imFnm));   // read in as an image
       //System.out.println("Reading image " + imFnm);
    } catch (Exception e) {
      System.out.println("Could not read image from " + imFnm);
    }
    return image;
  }  
   
  public Dimension getPreferredSize() {
  // make the panel wide enough for an image
  return new Dimension(WIDTH, HEIGHT); }
   
  public void writeToPort(String send) {
    //System.out.print("String to send: " + send + "\n");
   AndroiDAQ_Comm.writetoport(send);
  }
   
  public void run() {
     //display the current webcam image every DELAY ms
     //The time statistics gathered here include the time taken to
     //detect movement.
      
      VideoCapture grabber = initGrabber(CAMERA_ID);
      webcam_image = new Mat();  
      if (grabber == null)
          return;
         
      Point pt;
      serial = new AndroiDAQ_Comm();
      //long duration;
      isRunning = true;
      isFinished = false;
      BufferedImage temp; 
      if(grabber.isOpened()){
          while (isRunning) {
              //long startTime = System.currentTimeMillis();
              if (!processing) {
                  grabber.read(webcam_image);
                  temp = matToBufferedImage(webcam_image);
                  this.setImage(temp);  // update detector with new image
              }
              if (getTrackFace()) {
                   
                  if ((pt = fd.detect(webcam_image)) != null) {    // get new COG
                        processing = true;
                        prevXYPoint = xyPoint; 
                        xyPoint = pt;
                        //System.out.println("FaceCenterPoint =: " + cogPoint);
                        reportXYChanges(xyPoint, prevXYPoint);
                  }
              }
              if (getMotionDetect()) {
                   
                  md.calcMove(webcam_image);    // update detector with new image
                  if ((pt = md.getCOG()) != null) {    // get new COG
                      processing = true;
                      prevXYPoint = xyPoint; 
                      xyPoint = pt;
                      reportXYChanges(xyPoint, prevXYPoint);
                  } 
              }
              //System.out.println("imageCount =: " + imageCount);
              if (imageCount == 1) {
                  md = new MotionDetection(webcam_image);
                  fd = new FaceDetection();
              }
              imageCount++;
              repaint();
              //duration = System.currentTimeMillis() - startTime;
          }
      }
  closeGrabber(grabber, CAMERA_ID);
  System.out.println("Execution terminated");
  isFinished = true;
  }  
  private BufferedImage getImage(){  
      return image;  
  }  
   
  private void setImage(BufferedImage newimage){  
    image = newimage;
    return;  
  } 
   
  public static BufferedImage matToBufferedImage(Mat matrix) {
      int cols = matrix.cols();  
      int rows = matrix.rows();  
      int elemSize = (int)matrix.elemSize();  
      byte[] data = new byte[cols * rows * elemSize];  
      int type;  
      matrix.get(0, 0, data);  
      switch (matrix.channels()) {
        case 1:  
            type = BufferedImage.TYPE_BYTE_GRAY;  
            break;  
        case 3:  
            type = BufferedImage.TYPE_3BYTE_BGR;  
            // bgr to rgb  
            byte b;  
            for(int i=0; i<data.length; i=i+3) {
                b = data[i];  
                data[i] = data[i+2];  
                data[i+2] = b;  
            }  
            break;  
            default:  
            return null;  
      }  
      BufferedImage image2 = new BufferedImage(cols, rows, type);  
      image2.getRaster().setDataElements(0, 0, cols, rows, data);  
      return image2;  
  } 
   
  private VideoCapture initGrabber(int ID) {
      System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
       
      VideoCapture grabber =new VideoCapture(0); 
      try {
          //grabber = FrameGrabber.createDefault(ID);
          //grabber = new FrameGrabber(ID);
          //grabber = new OpenCVFrameGrabber("");
          //grabber.setFormat("dshow");       // using DirectShow
          grabber.set(Highgui.CV_CAP_PROP_FRAME_WIDTH , WIDTH);    
          // default is too small: 320x240
          grabber.set(Highgui.CV_CAP_PROP_GIGA_FRAME_SENS_HEIGH, HEIGHT);
          //grabber..start();                     prevYfactor = yfactor;
      } catch(Exception e) {
        System.out.println("Could not start grabber");  
        System.out.println(e);
        System.exit(1);
      }
      return grabber;
  } 
  private void closeGrabber(VideoCapture grabber, int ID)  {
    try {
        isRunning = false;
        grabber.release();
    } catch(Exception e) {
        System.out.println("Problem stopping grabber " + ID);  }
  }  
   
  public void setTrackFace(boolean setting) {
      trackFace = setting;
  }
   
  public boolean getTrackFace() {
    return trackFace;
  }
   
  public void setMotionDetect(boolean setting) {
      motionDetect = setting;
  }
   
  public boolean getMotionDetect() {
    return motionDetect;
  }
   
  public void centerPanTilt() {
      String text = "013\r";
      serial.writetoport(text);
  }
   
  private void reportXYChanges(Point xyPoint, Point prevXYPoint) {
  // compare cogPoint and prevCogPoint
      if (prevXYPoint == null)
      return;
     
      //For face detection, camera is mounted on pan tilt 
      //assembly to center detected face
      //if (faceDetected) {
      if (fd.faceDetected()) {
          if (xyPoint.x < HALF_HORIZONTAL) {
              //xStep = (int) (Math.round(HALF_HORIZONTAL - cogPoint.x)/28);   
              xStep = (int) (Math.round(HALF_HORIZONTAL - xyPoint.x)/57.6);   
              //System.out.println("Stepper Left by: " + Math.abs(xStep));
              if (xStep != 0) {
                  String text = "015\r" + "0" + Math.abs(xStep) + "\r" + "00\r";
                  //System.out.println("String to be sent: " + text);
                  serial.writetoport(text);
                  try {
                      Thread.sleep(15);  // wait until DELAY time has passed
                  } catch (Exception ex) {
                  }
              }
              xStep = 0;
          }
          if (xyPoint.x > HALF_HORIZONTAL) {
              //xStep = (int) (Math.round(cogPoint.x - HALF_HORIZONTAL)/28);   
              xStep = (int) (Math.round(xyPoint.x - HALF_HORIZONTAL)/57.6); 
              //System.out.println("Stepper Right by: " + Math.abs(xStep));
              if (xStep != 0) {
                  String text = "015\r" + "0" + Math.abs(xStep) + "\r" + "01\r";
                  // System.out.println("  String to be sent: " + text);
                  serial.writetoport(text);
                  try {
                      Thread.sleep(15);  // wait until DELAY time has passed
                  } catch (Exception ex) {
                  }
              }
              xStep = 0;
          }
          if (xyPoint.y > HALF_VERTICAL) {
              yStep = (int) (Math.round(xyPoint.y - HALF_VERTICAL)/2.8);     
              // so + y-axis is up screen
              //int yfactor = 1500 + ((cogPoint.y * 2) - 480);
              yfactor = 1500 - yStep;
              System.out.println("Calc mS for Servo: " + yfactor);
              if (yfactor != prevYfactor) {
                  prevYfactor = yfactor;
                   
                  String text = "014\r" + "0" + yfactor + "\r";
                  //System.out.println("  String to be sent: " + text);
                  serial.writetoport(text);
                  try {
                      Thread.sleep(15);  // wait until DELAY time has passed
                  } catch (Exception ex) {
                  }
              }
          } 
          if (xyPoint.y < HALF_VERTICAL){
              yStep = (int) (Math.round(HALF_VERTICAL - xyPoint.y)/2.8);    
              // so + y-axis is up screen
              //int yfactor =  1500 - (480 - (cogPoint.y * 2));
              yfactor =  1500 + yStep;
              System.out.println("Calc mS for Servo: " + yfactor);
              if (yfactor != prevYfactor) {
                  prevYfactor = yfactor;
                   
                  String text = "014\r" + "0" + yfactor + "\r";
                  //System.out.println("  String to be sent: " + text);
                  serial.writetoport(text);
                  try {
                      Thread.sleep(15);  // wait until DELAY time has passed
                  } catch (Exception ex) {
                  }
              }
          }
          processing = false;
      }
      if (motionDetect) {
          // calculate the distance moved and convert to steps needed
          int xStep = (int) (Math.round(xyPoint.x - prevXYPoint.x));  
          int yStep = -1 *(xyPoint.y - prevXYPoint.y);      // so + y-axis is up screen
          //System.out.println("xStep, yStep: (" + xStep + ", " +yStep + ")");
          //System.out.println("yStep: " + yStep);
          //System.out.println("COGY: " + cogPoint.y);
          //System.out.println("COGX: " + cogPoint.x);
          int distMoved = (int) Math.round( Math.sqrt( (xStep*xStep) + (yStep*yStep)) );
          //System.out.println("xStep: " + xStep);
          Math.round( Math.toDegrees( Math.atan2(yStep, xStep)) );
         
          // for motion detection, camera needs to be removed from pan tilt 
          //assembly as it will be an aimer for camera view
          if (distMoved > MIN_MOVE_REPORT) {
              if (xStep < 0) {
                  int xfactor = (int) (Math.round(HALF_HORIZONTAL - xyPoint.x) /57.6);  
                  //System.out.println("  Calc steps Stepper right: " + xfactor);
                  String text = "015\r" + "0" + Math.abs(xfactor) + "\r" + "00\r";
                  //System.out.println("  String to be sent: " + text);
                  serial.writetoport(text);
                  try {
                      Thread.sleep(15);  // wait until DELAY time has passed
                  } catch (Exception ex) {
                  }
              } 
              if (xStep > 0) {
                  int xfactor = (int) (Math.round(xyPoint.x - HALF_HORIZONTAL) /57.6); 
                  //System.out.println("  Calc steps Stepper left: " + xfactor);
                  String text = "015\r" + "0" + Math.abs(xfactor) + "\r" + "01\r";
                  // System.out.println("  String to be sent: " + text);
                  serial.writetoport(text);
                  try {
                      Thread.sleep(15);  // wait until DELAY time has passed
                  } catch (Exception ex) {
                  }
              }
          
              if (xyPoint.y < 240) {
                  int xfactor = (int) (1500 - (((xyPoint.y * 2) - 480) /2.8));
                  //System.out.println("  Calc mS for Servo: " + xfactor);
                  String text = "014\r" + "0" +xfactor + "\r";
                  //System.out.println("  String to be sent: " + text);
                  serial.writetoport(text);
                  try {
                      Thread.sleep(15);  // wait until DELAY time has passed
                  } catch (Exception ex) {
                  }
              } 
              if (xyPoint.y > 240) {
                  int xfactor =  (int) (1500 + ((480 - (xyPoint.y * 2))/2.8));
                  //System.out.println("  Calc mS for Servo: " + xfactor);
                  String text = "014\r" + "0" + xfactor + "\r";
                  // System.out.println("  String to be sent: " + text);
                  serial.writetoport(text);
                  try {
                      Thread.sleep(15);  // wait until DELAY time has passed
                  } catch (Exception ex) {
                  }
              } 
          }
          processing = false;
      }
  } 
  public void paintComponent(Graphics g) {
      // Draw the image, the rectangle (and crosshairs) around a detected
      // face, and the average ms snap time at the bottom left of the panel. 
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g;
      g2.setFont(msgFont);
      BufferedImage temp = getImage(); 
      // draw the image, stats, and detection rectangle
      if (temp != null) {
          g2.setColor(Color.YELLOW);
          g2.drawImage(temp,10,10,temp.getWidth()- 20,temp.getHeight() - 20, this);   
          // draw the snap
          //String statsMsg = String.format("Snap Avg. Time:  %.1f ms",
                                        //((double) totalTime / imageCount));
          //g2.drawString(statsMsg, 10, HEIGHT-15);  
                        // write statistics in bottom-left corner
          if (getTrackFace()) {
              //drawRect(g2); //called to draw crosshairs as well.
              if (xyPoint != null)
                  drawCrosshairs2(g, xyPoint.x, xyPoint.y);   // positioned at COG
          }
          if (!getTrackFace()) {
              removeCrosshairs(g2);
          }
          if (getMotionDetect()) {
              if (xyPoint != null)
                  drawCrosshairs2(g, xyPoint.x, xyPoint.y);   // positioned at COG
          }
          if (!getMotionDetect()) {
              removeCrosshairs2(g);
          }
      } else {  // no image yet
          g2.setColor(Color.BLUE);
          g2.drawString("Loading from camera " + CAMERA_ID + "...", 5, HEIGHT-10);
      }
  } 
  @SuppressWarnings("unused")
private void drawRect(Graphics2D g2) {
      //use the face rectangle to draw a yellow rectangle around the face, with 
      //crosshairs at its center.
      // The drawing of faceRect is in a synchronized block since it may be being
      // updated or used for image saving at the same time in other threads.
     
      synchronized(faceRect) {
          if (faceRect.width == 0) {
              return;
          }
          // draw a thick yellow rectangle
          g2.setColor(Color.YELLOW);
          g2.setStroke(new BasicStroke(6)); 
          //g2.drawRect(faceRect.x, faceRect.y, faceRect.width, faceRect.height);
           
          int xCenter = faceRect.x + faceRect.width/2;
          int yCenter = faceRect.y + faceRect.height/2;
          prevXYPoint = xyPoint; 
          xyPoint = new Point(xCenter, yCenter) ;
          //System.out.println("cogPoint " + cogPoint);
          if (faceDetected) {
              drawCrosshairs(g2, xCenter, yCenter);
          } else {
              removeCrosshairs(g2);
          }
      }
  }  
  private void drawCrosshairs(Graphics2D g2, int xCenter, int yCenter) {
      // draw crosshairs graphic or a red circle
      if (crosshairs != null) {
          g2.drawImage(crosshairs, xCenter - crosshairs.getWidth()/2, 
                              yCenter - crosshairs.getHeight()/2, this);
      } else {    
          g2.setColor(Color.RED);
          g2.fillOval(xCenter-10, yCenter-10, 20, 20);
      }
  } 
   
  private void drawCrosshairs2(Graphics g, int xCenter, int yCenter) {
      // draw crosshairs graphic or a red circle
  
      if (crosshairs != null) {
          g.drawImage(crosshairs, xCenter - crosshairs.getWidth()/2, 
                              yCenter - crosshairs.getHeight()/2, this);
      } else {    
          g.setColor(Color.RED);
          g.fillOval(xCenter-10, yCenter-10, 20, 20);
      }
  }  
   
  private void removeCrosshairs(Graphics2D g2) {
      // draw crosshairs graphic or a red circle
      //System.out.println("Removing Crosshairs");
      g2.drawImage(null, 0, 0, this);
  } 
  private void removeCrosshairs2(Graphics g) {
      // draw crosshairs graphic or a red circle
      //System.out.println("Removing Crosshairs");
      g.drawImage(null, 0, 0, this);
  }   
  public void closeDown() {
    //Terminate run() and wait for it to finish.
    //This stops the application from exiting until everything
    //has finished. 
    
    isRunning = false;
    while (!isFinished) {
      try {
        Thread.sleep(DELAY);
      } 
      catch (Exception ex) {}
    }
    //serial.disconnect();
  } 
}
