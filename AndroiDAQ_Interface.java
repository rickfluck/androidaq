// AndroiDAQ_Interface.java
// Copyright Controlled Capture Systems, Rick Fluck 2013-2014 
/* Portions of code are credited to Andrew Davison,  July 2013
 * Uses AndroiDAQ module and various firmware changes to control 
 * one servo and one stepper motor
 * Uses face detection and motion detection using JavaCV by Samuel Audet  
 *  https://code.google.com/p/javacv/ 
 * AndroiDAQ information at:http://www.controlcapture.com/androiddaqmod
 */
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
public class AndroiDAQ_Interface  implements ActionListener {
  // GUI components
  private AndroiDAQ_Cam_Controller motionPanel;
    JFrame frame;
    static JTextArea text;
    JTextField inputText;
    JScrollPane scroller;
    JButton button1;
    JButton button2;
    boolean tracking = false;
    boolean detecting = false;
     
     
  public AndroiDAQ_Interface() {
    super();
    frame = new JFrame("AndroiDAQ USB Camera Interface");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(500,400); 
    frame.addWindowListener(new java.awt.event.WindowAdapter()  {
        public void windowClosing(WindowEvent winEv){
            AndroiDAQ_Comm.disconnect();
            motionPanel.closeDown();
        }
    });
     
    //Main Panel
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    panel.setBackground(Color.LIGHT_GRAY);
     
    //Button Panel
    JPanel actionPanel  = new JPanel(new GridLayout(1, 0));
    button1 = new JButton("Track Face");
    button1.addActionListener(new TrackFaceListener());
    button2 = new JButton("Motion Detect");
    button2.addActionListener(new MotionDetectListener());
     
    //Input Panel
    JPanel inputPanel = new JPanel(new BorderLayout());
    JLabel input = new JLabel("DAQ Input:");
    inputText = new JTextField(50);
    inputText.addActionListener(this);
    text = new JTextArea(10,50);
    text.setLineWrap(true);
     
    //Output Panel
    JPanel outputPanel = new JPanel(new BorderLayout());
    JLabel output = new JLabel("DAQ Output:");
    scroller = new JScrollPane (text);
    scroller.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {  
        public void adjustmentValueChanged(AdjustmentEvent e) {  
            e.getAdjustable().setValue(e.getAdjustable().getMaximum());  
        }
    });
    actionPanel.add(button1);
    actionPanel.add(button2);
    inputPanel.add(input, BorderLayout.NORTH);
    inputPanel.add(inputText, BorderLayout.CENTER);
    outputPanel.add(output, BorderLayout.NORTH);
    outputPanel.add(scroller, BorderLayout.CENTER);
    panel.add(actionPanel, BorderLayout.NORTH);
    panel.add(inputPanel, BorderLayout.CENTER);
    panel.add(outputPanel ,BorderLayout.SOUTH);
    motionPanel = new AndroiDAQ_Cam_Controller();
    // the sequence of pictures appear here
    motionPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    BufferedImage image = null;
    try {
        image = ImageIO.read(frame.getClass().getResource("/CCapturelogo.png"));
    } catch (IOException e) {
        e.printStackTrace();
    }
    frame.setIconImage(image);
    frame.getContentPane().add(BorderLayout.CENTER, panel);
    frame.getContentPane().add(BorderLayout.NORTH, motionPanel);
    frame.addWindowListener( new WindowAdapter() {
        public void windowOpened( WindowEvent e ){
            inputText.requestFocus();
        }
    });
    //frame.setBounds(20,20,620,800);
    frame.setLocationByPlatform(true);
    frame.pack();
    frame.setVisible(true);
  } 
  @Override
    public void actionPerformed(ActionEvent arg0) {
        String text = "0" + inputText.getText() + "\r";
        System.out.print("String: " + text + "\n");
        motionPanel.writeToPort(text);
        inputText.setText("");
    }
  // -------------------------------------------------------
    public class TrackFaceListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.print("TrackFaceListener: " + e);
            if (!tracking) {
                button1.setText("Tracking For Faces");
                button2.setEnabled(false);
                motionPanel.setTrackFace(true);
                tracking = true;
            } else {
                button1.setText("Track Face");
                button2.setEnabled(true);
                motionPanel.setTrackFace(false);
                tracking = false;
            }
        }
    }
    public class MotionDetectListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.print("MotionDetectListener: " + e);
            if (!detecting) {
                button2.setText("Detecting Motion");
                button1.setEnabled(false);
                motionPanel.setMotionDetect(true);
                motionPanel.centerPanTilt();
                detecting = true;
            } else {
                button2.setText("Motion Detect");
                button1.setEnabled(true);
                motionPanel.setMotionDetect(false);
                detecting = false;
            }
        }
    }
    public static void main( String args[] )  {  
        new AndroiDAQ_Interface();  
    }
}
