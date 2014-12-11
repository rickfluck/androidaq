// OpenCVWebCam.java
// Copyright 2012-2014 Rick Fluck, Controlled Capture Systems.
import java.awt.*;  
import java.awt.image.BufferedImage;  
import javax.swing.JFrame;
import javax.swing.JPanel; 
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.VideoCapture;
public class OpenCVWebCamTest extends JPanel {  
    private static final long serialVersionUID = 1L;  
    private BufferedImage image;  
   
    // Create a constructor method  
    public OpenCVWebCamTest(){  
        super();  
    }  
    private BufferedImage getimage(){  
        return image;  
    }  
    private void setimage(BufferedImage newimage){  
        image = newimage;
        return;  
    }  
   
    /**  
    * Converts/writes a Mat into a BufferedImage.  
    *  
    * @param matrix Mat of type CV_8UC3 or CV_8UC1  
    * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY  
    */ 
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
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        BufferedImage temp = getimage();
        //int width = temp.getWidth();
        //int height = temp.getHeight();
        g2.drawImage(temp,10,10,640,480, this);  
    }  
    public static void main(String arg[]){  
        // Load the native library.  
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        JFrame frame = new JFrame("BasicPanel");  
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
        frame.setSize(400,400);  
        OpenCVWebCamTest panel = new OpenCVWebCamTest();  
        frame.setContentPane(panel);       
        frame.setVisible(true);       
        Mat webcam_image = new Mat();  
        BufferedImage temp;  
        VideoCapture capture =new VideoCapture(0);  
        if( capture.isOpened()) {
            while( true ) {  
                capture.read(webcam_image);  
                if( !webcam_image.empty()) {  
                    frame.setSize(webcam_image.width()+40,webcam_image.height()+60);  
                    temp = matToBufferedImage(webcam_image);  
                    panel.setimage(temp);  
                    panel.repaint();  
                } else {  
                    System.out.println(" --(!) No captured frame -- Break!");  
                    break;  
                }  
            }  
        }  
        return;  
    }  
}
