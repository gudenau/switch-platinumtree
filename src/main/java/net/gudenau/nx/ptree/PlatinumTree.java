package net.gudenau.nx.ptree;

import net.gudenau.nx.ptree.usb.Usb;
import net.gudenau.nx.ptree.usb.UsbHotplugHandler;
import net.gudenau.nx.ptree.util.Platform;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PlatinumTree{
    private static final short SWITCH_VID = (short)0x057E;
    private static final short SWITCH_PID = (short)0x3000;

    private Usb usb;
    private UsbHotplugHandler hotplugHandler;

    private List<Client> clients = new ArrayList<>();

    private boolean readOnly = true;
    private List<Favorite> favorites = new ArrayList<>();

    public static void main(String[] arguments){
        Runtime.getRuntime().addShutdownHook(new Thread(PlatinumTree::shutdown));
        Platform.init();

        new PlatinumTree().run();
    }

    private void run(){
        SwingUtilities.invokeLater(new TrayManager(this)::run);

        var configDir = Platform.getOS().getConfigDir();
        if(!configDir.exists()){
            configDir.mkdirs();
        }
        var favoriteFile = new File(configDir, "favorites");
        if(favoriteFile.exists()){
            try(var file = new RandomAccessFile(favoriteFile, "r")){
                var count = file.readInt();
                for(int i = 0; i < count; i++){
                    var length = file.readInt();
                    var buffer = new byte[length];
                    file.readFully(buffer);
                    var name = new String(buffer, StandardCharsets.UTF_8);

                    length = file.readInt();
                    buffer = new byte[length];
                    file.readFully(buffer);
                    var drive = new String(buffer, StandardCharsets.UTF_8);

                    length = file.readInt();
                    buffer = new byte[length];
                    file.readFully(buffer);
                    var path = new String(buffer, StandardCharsets.UTF_8);

                    favorites.add(new Favorite(name, drive, path));
                }
            }catch(IOException e){
                throw new RuntimeException("Failed to read favorites", e);
            }
        }else{
            switch(Platform.getOS()){
                case LINUX:{
                    favorites.add(new Favorite("Home", "root", System.getProperty("user.home")));
                    favorites.add(new Favorite("Downloads", "root", System.getProperty("user.home") + "/Downloads"));
                } break;
                default: break;
            }

            try{
                favoriteFile.createNewFile();
                saveFavorites();
            }catch(IOException e){
                throw new RuntimeException("Failed to create favorite file", e);
            }
        }

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

    void saveFavorites(){
        var favoriteFile = new File(Platform.getOS().getConfigDir(), "favorites");
        try(var file = new RandomAccessFile(favoriteFile, "rw")){
            file.writeInt(favorites.size());
            for(var favorite : favorites){
                var buffer = favorite.getName().getBytes(StandardCharsets.UTF_8);
                file.writeInt(buffer.length);
                file.write(buffer);

                buffer = favorite.getDrive().getBytes(StandardCharsets.UTF_8);
                file.writeInt(buffer.length);
                file.write(buffer);

                buffer = favorite.getPath().getBytes(StandardCharsets.UTF_8);
                file.writeInt(buffer.length);
                file.write(buffer);
            }
        }catch(IOException e){
            System.out.println("Failed to save favorites");
            e.printStackTrace();
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

    List<Favorite> getFavorites(){
        return favorites;
    }
}
