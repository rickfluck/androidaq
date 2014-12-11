// FaceDetection.java
// Copyright 2013 Rick Fluck
import java.awt.Point;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
public class FaceDetection {
     
    private CascadeClassifier face_cascade; 
    boolean faceDetected;
    // Create a constructor method  
    public FaceDetection() {
        face_cascade = new CascadeClassifier("resources/lbpcascade_frontalface.xml");  
        if(face_cascade.empty()) {
            System.out.println("--(!)Error loading A\n");  
            return;  
        } else {
            System.out.println("Face classifier loaded");  
        }  
    }  
    public Point detect(Mat inputframe){
        Point center = null;
        Mat mRgba=new Mat();  
        Mat mGrey=new Mat();  
        MatOfRect faces = new MatOfRect();  
        inputframe.copyTo(mRgba);  
        inputframe.copyTo(mGrey);  
        Imgproc.cvtColor( mRgba, mGrey, Imgproc.COLOR_BGR2GRAY);  
        Imgproc.equalizeHist( mGrey, mGrey );  
        face_cascade.detectMultiScale(mGrey, faces);
        int total = faces.toArray().length;
        if (total == 0) {
             //System.out.println("No faces found");
             faceDetected = false;
             return null;
         } else if (total > 1)  { // this case should not happen, but included for safety
             //System.out.println("Multiple faces detected (" + total + "); using the first");
             faceDetected = true;
         } else {
             //System.out.println("Face detected");
             faceDetected = true;
         }
        //System.out.println(String.format("Detected %s faces", faces.toArray().length));  
        for(Rect rect:faces.toArray()) {
            int xPoint = (int) (rect.x + rect.width*0.5);
            int yPoint = (int) (rect.y + rect.height*0.5);
            center = new Point(xPoint, yPoint);  
            //System.out.println("Points center = " + xPoint + "," + yPoint); 
           //Core.ellipse( mRgba, center, new Size( rect.width*0.5, rect.height*0.5), 
           //0, 0, 360, new Scalar( 255, 0, 255 ), 4, 8, 0 );  
        }  
        return center;  
    } 
     
    public boolean faceDetected() {
         
        return faceDetected;        
    }  
}
