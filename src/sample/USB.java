package sample;

import java.nio.ByteBuffer;
import org.usb4java.*;
/**
 * Created by Bartek on 2017-01-29.
 */
public class USB {

    private static DeviceHandle handle;
    private static Context context;

    /** The communication timeout in milliseconds. */
    private static final int TIMEOUT = 5000;
    //private static final int TIMEOUT = 500000;

    private static final short VENDOR_ID = 0x16c0;

    private static final short PRODUCT_ID = 0x27d8;

    private static final byte[] CONNECT_HEADER = new byte[] { 0x43, 0x4E, 0x58,
            0x4E, 0x00, 0x00, 0x00, 0x01, 0x00, 0x10, 0x00, 0x00, 0x17, 0x00, 0x00,
            0x00, 0x42, 0x06, 0x00, 0x00, (byte) 0xBC, (byte) 0xB1, (byte) 0xA7,
            (byte) 0xB1 };

    private static final byte INTERFACE = 0;


    private static final byte LED_ON = 0x02;

    private static final byte LED_OFF = 0x01;

    //private static final byte[] data = new byte[] { 0x48,0x45,0x4C,0x4C,0x4F, 0x00, 0x57,0x4F,0x52,0x4C,0x44};
    private static final byte[] dataSend = new byte[] { 0x48,0x45,0x4C,0x4C,0x4F};
    private static final byte[] dataReceive = new byte[] { 0x00,0x00,0x00,0x00,0x00};


    private static final byte[] CONNECT_BODY = new byte[] { 0x68, 0x6F, 0x73,
            0x74, 0x3A, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x3A, 0x41,
            0x44, 0x42, 0x20, 0x44, 0x65, 0x6D, 0x6F, 0x00 };


    private static void dumpConfigurationDescriptors(final Device device,
                                                    final int numConfigurations)
    {
        for (byte i = 0; i < numConfigurations; i += 1)
        {
            final ConfigDescriptor descriptor = new ConfigDescriptor();
            final int result = LibUsb.getConfigDescriptor(device, i, descriptor);
            if (result < 0)
            {
                throw new LibUsbException("Unable to read config descriptor",
                        result);
            }
            try
            {
                System.out.println(descriptor.dump().replaceAll("(?m)^",
                        "  "));
            }
            finally
            {
                // Ensure that the config descriptor is freed
                LibUsb.freeConfigDescriptor(descriptor);
            }
        }
    }


    private static void dumpDevice(final Device device)
    {
        // Dump device address and bus number
        final int address = LibUsb.getDeviceAddress(device);
        final int busNumber = LibUsb.getBusNumber(device);
        System.out.println(String
                .format("Device %03d/%03d", busNumber, address));

        // Dump port number if available
        final int portNumber = LibUsb.getPortNumber(device);
        if (portNumber != 0)
            System.out.println("Connected to port: " + portNumber);

        // Dump parent device if available
        final Device parent = LibUsb.getParent(device);
        if (parent != null)
        {
            final int parentAddress = LibUsb.getDeviceAddress(parent);
            final int parentBusNumber = LibUsb.getBusNumber(parent);
            System.out.println(String.format("Parent: %03d/%03d",
                    parentBusNumber, parentAddress));
        }

        // Dump the device speed
        System.out.println("Speed: "
                + DescriptorUtils.getSpeedName(LibUsb.getDeviceSpeed(device)));

        // Read the device descriptor
        final DeviceDescriptor descriptor = new DeviceDescriptor();
        int result = LibUsb.getDeviceDescriptor(device, descriptor);
        if (result < 0)
        {
            throw new LibUsbException("Unable to read device descriptor",
                    result);
        }

        // Try to open the device. This may fail because user has no
        // permission to communicate with the device. This is not
        // important for the dumps, we are just not able to resolve string
        // descriptor numbers to strings in the descriptor dumps.
        DeviceHandle handle = new DeviceHandle();
        result = LibUsb.open(device, handle);
        if (result < 0)
        {
            System.out.println(String.format("Unable to open device: %s. "
                            + "Continuing without device handle.",
                    LibUsb.strError(result)));
            handle = null;
        }

        // Dump the device descriptor
        System.out.print(descriptor.dump(handle));

        // Dump all configuration descriptors
        dumpConfigurationDescriptors(device, descriptor.bNumConfigurations());

        // Close the device if it was opened
        if (handle != null)
        {
            LibUsb.close(handle);
        }
    }

    private static Device findDevice(short vendorId, short productId)
    {
        // Read the USB device list
        DeviceList list = new DeviceList();
        int result = LibUsb.getDeviceList(null, list);
        if (result < 0) throw new LibUsbException("Unable to get device list", result);

        try
        {
            // Iterate over all devices and scan for the right one
            for (Device device: list)
            {
                DeviceDescriptor descriptor = new DeviceDescriptor();
                result = LibUsb.getDeviceDescriptor(device, descriptor);
                if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to read device descriptor", result);
                if (descriptor.idVendor() == vendorId && descriptor.idProduct() == productId) return device;
            }
        }
        finally
        {
            // Ensure the allocated device list is freed
            LibUsb.freeDeviceList(list, true);
        }

        // Device not found
        return null;
    }



    private static byte[] read(DeviceHandle handle)
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(2);
        buffer.rewind();
        int transfered = LibUsb.controlTransfer(handle,
                (byte) (LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE | LibUsb.ENDPOINT_IN),
                (byte)0x02,
                (short)2,//(short) 2,
                (short) 1, buffer, TIMEOUT);

        if (transfered < 0)
            throw new LibUsbException("Control transfer failed", transfered);
        //if (transfered != 1)
        //  throw new RuntimeException("Not all data was received from device:"+transfered);

        byte[] data= new byte[2];
        buffer.rewind();
        for (int i=0; i<2; i++){
            data[i]= buffer.get();
        }

        return data;
    }




    private static byte[] Read(byte bRequest, short wValue, int length) // read in uC
    {

        ByteBuffer buffer = ByteBuffer.allocateDirect(length);

        //buffer.put(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
        //buffer.put(data);
        buffer.rewind();

        int transfered = LibUsb.controlTransfer(handle,
                //(byte) (LibUsb.REQUEST_TYPE_VENDOR | LibUsb.RECIPIENT_INTERFACE),  //0x40
                //(byte) (LibUsb.ENDPOINT_IN |LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE),
                (byte) (LibUsb.ENDPOINT_IN |LibUsb.REQUEST_TYPE_VENDOR | LibUsb.RECIPIENT_DEVICE),  //0xc0 192
                //(byte) 192, // bmRequestType
                bRequest, // bRequest
                wValue, // wValue
                (short) 1, // wIndex
                buffer, // data
                TIMEOUT // timeout
        );

        if (transfered < 0) throw new LibUsbException("Control transfer failed", transfered);
        System.out.println(transfered + " bytes received");

        byte[] data2= new byte[transfered];
        buffer.rewind();
        for (int i=0; i<transfered; i++){
            data2[i]= buffer.get();
        }

        return data2;

    }

    private static byte[] Write(byte bRequest, short wValue, byte[] data ) // write in uC
    {

        ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);

        //buffer.put(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
        buffer.put(data);

        int transfered = LibUsb.controlTransfer(handle,
                //(byte) (LibUsb.REQUEST_TYPE_VENDOR | LibUsb.RECIPIENT_INTERFACE),  //0x40
                //(byte) (LibUsb.ENDPOINT_IN |LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE),
                (byte) (LibUsb.ENDPOINT_OUT |LibUsb.REQUEST_TYPE_VENDOR | LibUsb.RECIPIENT_DEVICE),  //0xc0 192
                //(byte) 192, // bmRequestType
                bRequest, // bRequest
                wValue, // wValue
                (short) 1, // wIndex
                buffer, // data
                TIMEOUT // timeout
        );

        if (transfered < 0) throw new LibUsbException("Control transfer failed", transfered);
        System.out.println(transfered + " bytes sent");

        byte[] data2= new byte[transfered];
        buffer.rewind();
        for (int i=0; i<transfered; i++){
            data2[i]= buffer.get();
        }

        return data2;

    }


    private static String toHexString(byte[] bytes) {

        final String hexChars = "0123456789ABCDEF";
        StringBuffer sbTmp = new StringBuffer();
        char[] cTmp = new char[2];

        /*
        if (bytes==null){
            return new String("null");
        }
*/
        for (int i = 0; i < bytes.length; i++) {
            cTmp[0] = hexChars.charAt((bytes[i] & 0xF0) >>> 4);
            cTmp[1] = hexChars.charAt(bytes[i] & 0x0F);
            sbTmp.append(cTmp);
        }

        return sbTmp.toString();


    }


    public static void ConnectWithDevice()
    {
        context = new Context();
        int result = LibUsb.init(context);

        if (result != LibUsb.SUCCESS)
        {
            throw new LibUsbException("Unable to initialize libusb", result);
        }


        handle = LibUsb.openDeviceWithVidPid(context, VENDOR_ID,
                PRODUCT_ID);
        if (handle == null)
        {
            System.err.println("Test device not found.");
            System.exit(1);
        }


        result = LibUsb.claimInterface(handle, INTERFACE);
        if (result != LibUsb.SUCCESS)
        {
            throw new LibUsbException("Unable to claim interface", result);
        }
    }


    public static void SendData(byte bRequest,short wValue, byte[] data)
    {
        Write(bRequest,wValue,data);

    }

    public static byte[] ReadData(byte bRequest, short wValue, int length)
    {
        return Read(bRequest,wValue,length);
    }

    public static void ExitUsb()
    {
        // Close the device
        LibUsb.close(handle);

        // Deinitialize the libusb context
        LibUsb.exit(context);
    }

}
