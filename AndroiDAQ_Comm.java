//AndroiDAQ_Comm.java
// Copyright Controlled Capture Systems, Rick Fluck 2013-2014 
/* Uses JSSC for JAVA available at: 
 *https://github.com/scream3r/java-simple-serial-connector/releases. 
 * Uses AndroiDAQ module and various firmware changes to control 
 * one servo and one stepper motor
 * AndroiDAQ information at: http://www.controlcapture.com/androiddaqmod
 */
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
public class AndroiDAQ_Comm extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;
    static SerialPort serialPort;   
    static Boolean fromMain = false;    
    JPanel mainPanel;
    JFrame frame;
    JComboBox<Object> selector;
    JButton connectButton;
    static JTextArea text;
    JTextField inputText;
    JScrollPane scroller;
    AndroiDAQ_Interface motiondec;
    String[] myPorts;
     
    public AndroiDAQ_Comm() {
        super();
        // added below so other programs can use this  
        //String SerialPortID = "/dev/ttyAMA0";
        //System.setProperty("gnu.io.rxtx.SerialPorts", SerialPortID);
        myPorts = getAvailablePorts();
        if (fromMain) {
            buildGUI(); 
        } else {
            try {
                connect(myPorts[2].toString());
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }
        //motiondec = new MotionDetector();
    }
    public  String[] getAvailablePorts() {
        String[] list = SerialPortList.getPortNames();
        return list;
       }
    void connect ( String portName ) throws Exception {
        serialPort = new SerialPort(portName);
        if ( serialPort.isOpened() ) {
            System.out.println("Error: Port is currently in use");
        } else {
            try {
                serialPort.openPort();//Open serial port
                serialPort.setParams(SerialPort.BAUDRATE_115200, 
                                     SerialPort.DATABITS_8,
                                     SerialPort.STOPBITS_1,
                                     SerialPort.PARITY_NONE);
                //Set params. Also you can set params 
                //by this string: serialPort.setParams(9600, 8, 1, 0);
                //Add SerialPortEventListener
                serialPort.addEventListener(new SerialReader());
                if (fromMain) {
                    text.append("Connected to USB Port" + "\n");
                } else {
                    AndroiDAQ_Interface.text.append("Connected to USB Port" + "\n");
                }
            } catch (SerialPortException ex) {
                System.out.println(ex);
            }
        }     
    }
    public static void disconnect() {
        if (serialPort != null) {
            try {
                serialPort.closePort();//Close serial port
            }  catch (SerialPortException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
        }
            System.out.println("Disconnected to Serial Port");
        }
    }
    public static class SerialReader implements SerialPortEventListener {
        int byteCount;
        @Override
        public void serialEvent(SerialPortEvent event) {
             
            if(event.isRXCHAR()){//If data is available
                //System.out.print("event: " + event.getEventValue());
                try {
                    while ((byteCount = serialPort.getInputBufferBytesCount()) > 0) {
                        String message = serialPort.readString(byteCount);
                        //System.out.print("message: " + message.toString());
                        if (!fromMain) {
                            AndroiDAQ_Interface.text.append(message);
                        }else {
                            text.append(message + "\r");
                        }
                    }
                } catch (SerialPortException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    public static void writetoport(String send) {
        try {
            serialPort.writeString(send);
        } catch ( SerialPortException e) {
            e.printStackTrace();
        }
   }
    public void buildGUI() {
        frame = new JFrame("AndroiDAQ COM");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        JPanel actionPanel  = new JPanel(new GridLayout(1, 0));
        JLabel input = new JLabel("Ports (select one):");
        selector = new JComboBox<Object>();
        selector.setModel(new DefaultComboBoxModel<Object>(myPorts));
        connectButton = new JButton("Connect to Port");
        connectButton.addActionListener(new ConnectListener());
        actionPanel.add(input);
        actionPanel.add(selector);
        actionPanel.add(connectButton);
        inputText = new JTextField(50);
        inputText.addActionListener(this);
        text = new JTextArea(10,50);
        text.setLineWrap(true);
        scroller = new JScrollPane (text);
        scroller.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {  
            public void adjustmentValueChanged(AdjustmentEvent e) {  
                e.getAdjustable().setValue(e.getAdjustable().getMaximum());  
            }
        });
        panel.add(actionPanel, BorderLayout.NORTH);
        panel.add(inputText, BorderLayout.CENTER);
        panel.add(scroller ,BorderLayout.SOUTH);
        //motionPanel = new MotionPanel(); // the sequence of pictures appear here
        //frame.getContentPane().add(BorderLayout.NORTH, inputText);
        frame.getContentPane().add(BorderLayout.NORTH, panel);
        //frame.getContentPane().add(BorderLayout.CENTER, motionPanel);
        //frame.setBounds(50,50,600,300);
        frame.pack();
        frame.setVisible(true);
    }
    public static void main ( String[] args ) {
    fromMain = true;
        new AndroiDAQ_Comm();
    } 
    @Override
    public void actionPerformed(ActionEvent arg0) {
        String text = "0" + inputText.getText() + "\r";
        //System.out.print("String: " + text + "\n");
        writetoport(text);
        inputText.setText("");
    }
    public class ConnectListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String portToConnect = selector.getSelectedItem().toString();
            //System.out.print("Port to connect to is: " + portToConnect + "\r");
            try {
                //connect("/dev/ttyUSB0");
                connect(portToConnect);
            } catch ( Exception e1 ) {
                e1.printStackTrace();
            } 
        }
    }
}
