package net.gudenau.nx.ptree;

import net.gudenau.nx.ptree.usb.Usb;
import net.gudenau.nx.ptree.usb.UsbHotplugHandler;
import net.gudenau.nx.ptree.util.Platform;

public class PlatinumTree{
    private static final short SWITCH_VID = (short)0x057E;
    private static final short SWITCH_PID = (short)0x3000;

    private Usb usb;
    private UsbHotplugHandler hotplugHandler;

    public static void main(String[] arguments){
        Runtime.getRuntime().addShutdownHook(new Thread(PlatinumTree::shutdown));
        Platform.init();

        new PlatinumTree().run();
    }

    private void run(){
        usb = new Usb();
        if(usb.isHotplugSupported()){
            hotplugHandler = usb.registerHotplugHandler(
                Usb.HOTPLUG_ARRIVED, 0,
                SWITCH_VID, SWITCH_PID, -1,
                (ctx, device, event, object)->{
                    registerClient(new Client(usb, device));
                    return 0;
                },
                null
            );
        }
        usb.findDevices(SWITCH_VID, SWITCH_PID).stream()
            .map((device)->new Client(usb, device))
            .forEach(this::registerClient);
        if(usb.isHotplugSupported()){
            while(true){
                usb.handleCallbacks();
            }
        }
    }

    private void registerClient(Client client){
        new Thread(client).start();
    }

    private static void shutdown(){
        for(int i = 0; i < 100; i++){
            System.gc();
        }
    }
}
