Serial interface for AndroiDAQ module. More information about
the AndroiDAQ module here: http://www.controlcapture.com/androiddaqmod

@author: Rick Fluck
'''
import time
import sys
import serial
from serial.tools import list_ports
import threading
import wx
import cv2

#capture = cv2.VideoCapture(0)
#capture.set(cv2.cv.CV_CAP_PROP_FRAME_WIDTH, 320)
#capture.set(cv2.cv.CV_CAP_PROP_FRAME_HEIGHT, 240)
fps=15

class SerialComm(wx.Frame):
    
    def __init__(self, parent, title):
        # Override to set size and no resize allowed
        super(SerialComm, self).__init__(parent, title = title, size=(590,330))
        # Get serial port list on machine
        portList = list(self.serial_ports())
        # Insert hint for end user
        portList.insert(0, 'select port to connect')
        #Create users interface
        self.InitUI(portList)
        self.Centre()
        self.Show(True)
       
    def InitUI(self, myPorts):  
       
        #Create communications panel having +20 border size
        commPanel = wx.Panel(self)
        font = wx.SystemSettings_GetFont(wx.SYS_SYSTEM_FONT)
        font.SetPointSize(10)           
        commPanelSizer = wx.BoxSizer(wx.VERTICAL)
       
        #Create a ports list which is filled with the systems Serial Ports
        portsBox = wx.BoxSizer(wx.HORIZONTAL)
        portLabel = wx.StaticText(commPanel, label ='Comm Ports')
        portsBox.Add(portLabel, flag=wx.RIGHT, border = 8)
        portsListBox = wx.ComboBox(commPanel, pos = (50, 30), choices = myPorts)
        portsListBox.SetSelection(0)
        portsListBox.Bind(wx.EVT_COMBOBOX, self.OnSelect)
        portsBox.Add(portsListBox, proportion = 1)
        commPanelSizer.Add(portsBox, flag = wx.EXPAND|wx.LEFT|wx.RIGHT|wx.TOP, border = 10)
       
        #Create an input text control for user to enter commands to Serial port           
        inBoxTag = wx.BoxSizer(wx.HORIZONTAL)
        inLabel = wx.StaticText(commPanel, label =' Comm Port Input:')
        inLabel.SetFont(font)
        inBoxTag.Add(inLabel)
        commPanelSizer.Add(inBoxTag, flag = wx.LEFT|wx.TOP, border = 10)
        inBox = wx.BoxSizer(wx.HORIZONTAL)
        self.inTextBox = wx.TextCtrl(commPanel, style = wx.TE_PROCESS_ENTER)
        self.inTextBox.Bind(wx.EVT_KEY_DOWN, self.onEnter)
        inBox.Add(self.inTextBox, proportion = 1)
        commPanelSizer.Add(inBox, flag = wx.EXPAND|wx.LEFT|wx.RIGHT, border = 10)
       
        #Create an output text control to view output from Serial port
        outBoxTag = wx.BoxSizer(wx.HORIZONTAL)
        outLabel = wx.StaticText(commPanel, label = 'Comm Port Output:')
        outLabel.SetFont(font)
        outBoxTag.Add(outLabel)
        commPanelSizer.Add(outBoxTag, flag = wx.LEFT | wx.TOP, border=10)
        outBox = wx.BoxSizer(wx.VERTICAL)
        self.outTextBox = wx.TextCtrl(commPanel, style = wx.TE_MULTILINE)
        font1 = wx.Font(8, wx.MODERN, wx.NORMAL, wx.NORMAL, False, u'Consolas')
        self.outTextBox.SetFont(font1)
        outBox.Add(self.outTextBox, proportion = 1, flag=wx.EXPAND)
        commPanelSizer.Add(outBox, 2, flag = wx.BOTTOM|wx.LEFT|wx.RIGHT|wx.EXPAND, border = 10)
        commPanel.SetSizer(commPanelSizer)
   
    def onEnter(self, event):
        # Triggers if an event happens
        keycode = event.GetKeyCode()
        # If either Enter key is pressed do...
        if keycode == wx.WXK_RETURN or keycode == wx.WXK_NUMPAD_ENTER: 
            # Get the user's input value from the input text control place in variable recv
            recv = self.inTextBox.GetValue()
            # If recv is exit, close Serial port and leave application
            if recv == 'exit':
                self.ser.close()
                self.Destroy()
            else:
                # send the characters to the device connected to Serial port
                # (note an added \r\n carriage return and line feed
                # to the characters - this is requested by AndroiDAQ)
                self.ser.write(str(recv).encode())
                self.ser.write('\r\n')
                # Clear the user input text control
                self.inTextBox.SetValue('')
   
        event.Skip()
           
    def OnSelect(self, e):
        # Triggered when an item is selected in the CommPorts list
        # Get the string name of the item in the list
        portName = e.GetString()
        # Send the item string name to connect to Serial port
        self.serial_port_connect(portName)
           
    def onClose(self, event):
        # Event trigger on closing of window
        # Close serial port
        self.ser.close()
        # Close window
        self.Destroy()   

    def receiving(self, ser):
        # Serial port receiving, ran under thread
        while True:
            # Small delay for CPU savings
            time.sleep(0.1)
            try:
                if ser.inWaiting() &gt;0:
                    msg = ser.readline()
                    if msg:
                        self.handle_data(msg)
            except:
                pass
 
    def handle_data(self, data):
        # Outside caller to receive data and place in main thread GUI
        wx.CallAfter(self.outTextBox.AppendText, data)
        #print(data) 
             
    def serial_port_connect(self, portNum):       
        # Configure the serial connections, you may need to adjust these for other devices
        # this is set up for AndroiDAQ
        self.ser = serial.Serial(
            port = portNum,
            baudrate = 115200,
            parity = serial.PARITY_NONE,
            stopbits = serial.STOPBITS_ONE,
            bytesize = serial.EIGHTBITS,
            timeout = 0
        )
        # Check for open Serial port
        if self.ser.isOpen():
            # Show that port is open and that you are connected
            self.outTextBox.SetValue('Connected to AndroiDAQ.\r\nEnter your commands in Comm Port Input below.\r\nEnter "exit" to quit the application.\r\n')
        # Set focus to user input text control
        self.inTextBox.SetFocus()
        # Create and start Serial port receiving thread as daemon for auto stop and cleanup
        self.thread = threading.Thread(target=self.receiving, args=(self.ser,))
        self.thread.setDaemon(True)
        self.thread.start()
       
    def serial_ports(self):
        #Returns a list for all available serial ports
        # Check system platform as Windows is different than Linux/Unix
        if sys.platform == 'win32':
            # windows
            for i in range(256):
                try:
                    s = serial.Serial(i)
                    s.close()
                    yield 'COM' + str(i + 1)
                except serial.SerialException:
                    pass
        else:
            # unix
            for port in list_ports.comports():
                yield port[0]
   
app = wx.App()
SerialComm(None, title = 'AndroiDAQ Comm')
app.MainLoop()   
