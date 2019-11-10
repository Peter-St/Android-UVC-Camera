package humer.uvc_camera;

import java.math.BigInteger;

public class LockCameraVariables {

    private static byte[] bNumControlTerminal;
    private static byte[] bNumControlUnit;

    //  Camera Terminal Descriptor
    public static boolean Scanning_Mode;
    public static boolean Auto_Exposure_Mode;
    public static boolean Auto_Exposure_Priority;
    public static boolean Exposure_Time_Absolute;
    public static boolean Exposure_Time_Relative;
    public static boolean Focus_Absolute;
    public static boolean Focus_Relative;
    public static boolean Iris_Absolute;
    public static boolean Iris_Relative;
    public static boolean Zoom_Absolute;
    public static boolean Zoom_Relative;
    public static boolean PanTilt_Absolute;
    public static boolean PanTilt_Relative;
    public static boolean Roll_Absolute;
    public static boolean Roll_Relative;
    public static boolean Reserved_one;
    public static boolean Reserved_two;
    public static boolean Focus_Auto;
    public static boolean Privacy;

    // Processing Unit Descriptor
    public static boolean Brightness;
    public static boolean Contrast;
    public static boolean Hue;
    public static boolean Saturation;
    public static boolean Sharpness;
    public static boolean Gamma;
    public static boolean White_Balance_Temperature;
    public static boolean White_Balance_Component;
    public static boolean Backlight_Compensation;
    public static boolean Gain;
    public static boolean Power_Line_Frequency;
    public static boolean Hue_Auto;
    public static boolean White_Balance_Temperature_Auto;
    public static boolean White_Balance_Component_Auto;
    public static boolean Digital_Multiplier;
    public static boolean Digital_Multiplier_Limit;
    public static boolean Analog_Video_Standard;
    public static boolean Analog_Video_Lock_Status;


    public LockCameraVariables(byte[] terminal, byte[] unit)  {
        this.bNumControlTerminal = terminal;
        this.bNumControlUnit = unit;
    }


     /*
    D0: Brightness
    D1: Contrast
    D2: Hue
    D3: Saturation
    D4: Sharpness
    D5: Gamma
    D6: White Balance Temperature
    D7: White Balance Component
    D8: Backlight Compensation
    D9: Gain
    D10: Power Line Frequency
    D11: Hue, Auto
    D12: White Balance Temperature, Auto
    D13: White Balance Component, Auto
    D14: Digital Multiplier
    D15: Digital Multiplier Limit
    D16: Analog Video Standard
    D17: Analog Video Lock Status
    D18..(n*8-1): Reserved. Set to zero.
    */
    public void initUnit () {
        if(bNumControlUnit.length > 0) {
            int n = 0;
            Brightness = (BigInteger.valueOf(bNumControlUnit[0]).testBit(n++));            // (0)
            Contrast = (BigInteger.valueOf(bNumControlUnit[0]).testBit(n++));
            Hue = (BigInteger.valueOf(bNumControlUnit[0]).testBit(n++));
            Saturation = (BigInteger.valueOf(bNumControlUnit[0]).testBit(n++));
            Sharpness = (BigInteger.valueOf(bNumControlUnit[0]).testBit(n++));
            Gamma = (BigInteger.valueOf(bNumControlUnit[0]).testBit(n++));
            White_Balance_Temperature = (BigInteger.valueOf(bNumControlUnit[0]).testBit(n++));
            White_Balance_Component = (BigInteger.valueOf(bNumControlUnit[0]).testBit(n++));       // (7)
        }

        // next byte
        if(bNumControlUnit.length > 1) {
            int n = 0;
            Backlight_Compensation = (BigInteger.valueOf(bNumControlUnit[1]).testBit(n++));      // (8)
            Gain = (BigInteger.valueOf(bNumControlUnit[1]).testBit(n++));
            Power_Line_Frequency = (BigInteger.valueOf(bNumControlUnit[1]).testBit(n++));
            Hue_Auto = (BigInteger.valueOf(bNumControlUnit[1]).testBit(n++));
            White_Balance_Temperature_Auto = (BigInteger.valueOf(bNumControlUnit[1]).testBit(n++));
            White_Balance_Component_Auto = (BigInteger.valueOf(bNumControlUnit[1]).testBit(n++));
            Digital_Multiplier = (BigInteger.valueOf(bNumControlUnit[1]).testBit(n++));
            Digital_Multiplier_Limit = (BigInteger.valueOf(bNumControlUnit[1]).testBit(n++));
        }

        // next byte
        if(bNumControlUnit.length > 2) {
            int n = 0;
            Analog_Video_Standard = (BigInteger.valueOf(bNumControlUnit[2]).testBit(n++));   // (16)  (Reserved)
            Analog_Video_Lock_Status = (BigInteger.valueOf(bNumControlUnit[2]).testBit(n++));   // (17)
        }
    }

    /*
    D0: Scanning Mode
    D1: Auto-Exposure Mode
    D2: Auto-Exposure Priority
    D3: Exposure Time (Absolute)
    D4: Exposure Time (Relative)
    D5: Focus (Absolute)
    D6 : Focus (Relative)
    D7: Iris (Absolute)
    D8 : Iris (Relative)
    D9: Zoom (Absolute)
    D10: Zoom (Relative)
    D11: PanTilt (Absolute)
    D12: PanTilt (Relative)
    D13: Roll (Absolute)
    D14: Roll (Relative)
    D15: Reserved
    D16: Reserved
    D17: Focus, Auto
    D18: Privacy
    D19..(n*8-1): Reserved, set to zero
    */
    public void initTerminal () {
        //byte 0;
        int n = 0;
        Scanning_Mode = BigInteger.valueOf(bNumControlTerminal[0]).testBit(n++);   // (0)
        Auto_Exposure_Mode = BigInteger.valueOf(bNumControlTerminal[0]).testBit(n++);
        Auto_Exposure_Priority = BigInteger.valueOf(bNumControlTerminal[0]).testBit(n++);
        Exposure_Time_Absolute = BigInteger.valueOf(bNumControlTerminal[0]).testBit(n++);
        Exposure_Time_Relative = BigInteger.valueOf(bNumControlTerminal[0]).testBit(n++);
        Focus_Absolute = BigInteger.valueOf(bNumControlTerminal[0]).testBit(n++);
        Focus_Relative = BigInteger.valueOf(bNumControlTerminal[0]).testBit(n++);
        Iris_Absolute = BigInteger.valueOf(bNumControlTerminal[0]).testBit(n++);   // (7)
        // byte 1;
        n = 0;
        Iris_Relative = BigInteger.valueOf(bNumControlTerminal[1]).testBit(n++);   // (8)
        Zoom_Absolute = BigInteger.valueOf(bNumControlTerminal[1]).testBit(n++);
        Zoom_Relative = BigInteger.valueOf(bNumControlTerminal[1]).testBit(n++);
        PanTilt_Absolute = BigInteger.valueOf(bNumControlTerminal[1]).testBit(n++);
        PanTilt_Relative = BigInteger.valueOf(bNumControlTerminal[1]).testBit(n++);
        Roll_Absolute = BigInteger.valueOf(bNumControlTerminal[1]).testBit(n++);
        Roll_Relative = BigInteger.valueOf(bNumControlTerminal[1]).testBit(n++);
        Reserved_one = BigInteger.valueOf(bNumControlTerminal[1]).testBit(n++);   // (15) (Reserved)
        // byte 2;
        n = 0;
        Reserved_two = BigInteger.valueOf(bNumControlTerminal[2]).testBit(n++);   // (16)  (Reserved)
        Focus_Auto = BigInteger.valueOf(bNumControlTerminal[2]).testBit(n++);   // (17)
        Privacy = BigInteger.valueOf(bNumControlTerminal[2]).testBit(n++);   // (18)
    }

}
