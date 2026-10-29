package android.hardware.radio;
public class RadioManager { public static class ProgramInfo { public ProgramSelector getSelector(){return null;}
 public RadioMetadata getMetadata(){return null;} public ProgramSelector.Identifier getLogicallyTunedTo(){return null;}
 public ProgramSelector.Identifier getPhysicallyTunedTo(){return null;}
 public java.util.Collection<ProgramSelector.Identifier> getRelatedContent(){return null;} public int getSignalStrength(){return 0;}
 public java.util.Map<String,String> getVendorInfo(){return null;} } }
