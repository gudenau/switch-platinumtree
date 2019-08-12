package net.gudenau.nx.ptree;

import net.gudenau.nx.ptree.usb.Usb;
import net.gudenau.nx.ptree.usb.UsbHotplugHandler;
import net.gudenau.nx.ptree.util.Platform;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

public class PlatinumTree{
    private static final short SWITCH_VID = (short)0x057E;
    private static final short SWITCH_PID = (short)0x3000;

    private Usb usb;
    private UsbHotplugHandler hotplugHandler;

    private List<Client> clients = new ArrayList<>();

    private boolean readOnly = true;

    public static void main(String[] arguments){
        Runtime.getRuntime().addShutdownHook(new Thread(PlatinumTree::shutdown));
        Platform.init();

        new PlatinumTree().run();
    }

    private void run(){
        SwingUtilities.invokeLater(new TrayManager(this)::run);

        usb = new Usb();
        if(usb.isHotplugSupported()){
            hotplugHandler = usb.registerHotplugHandler(
                Usb.HOTPLUG_ARRIVED, 0,
                SWITCH_VID, SWITCH_PID, -1,
                (ctx, device, event, object)->{
                    registerClient(new Client(this, usb, device, readOnly));
                    return 0;
                },
                null
            );
        }
        usb.findDevices(SWITCH_VID, SWITCH_PID).stream()
            .map((device)->new Client(this, usb, device, readOnly))
            .forEach(this::registerClient);
        if(usb.isHotplugSupported()){
            while(true){
                usb.handleCallbacks();
            }
        }
    }

    void scan(){
        usb.findDevices(SWITCH_VID, SWITCH_PID).stream()
            .filter((device)->!isHandled(device))
            .map((device)->new Client(this, usb, device, readOnly))
            .forEach(this::registerClient);
    }

    private boolean isHandled(long device){
        for(var client : clients){
            if(client.getDevice() == device){
                return true;
            }
        }
        return false;
    }

    private void registerClient(Client client){
        clients.add(client);
        new Thread(client).start();
    }

    private static void shutdown(){
        for(int i = 0; i < 100; i++){
            System.gc();
        }
    }

    void deregisterClient(Client client){
        clients.remove(client);
    }

    void setReadOnly(boolean readOnly){
        clients.forEach((client)->client.setReadOnly(readOnly));
        this.readOnly = readOnly;
    }
}
