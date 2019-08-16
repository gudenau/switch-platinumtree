package net.gudenau.nx.ptree;

import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.image.BufferedImage;

public class TrayManager{
    private final PlatinumTree platinumTree;

    TrayManager(PlatinumTree platinumTree){
        this.platinumTree = platinumTree;
    }

    void run(){
        if(!SystemTray.isSupported()){
            return;
        }

        var tray = SystemTray.getSystemTray();
        var traySize = tray.getTrayIconSize();

        var icon = new TrayIcon(new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR));
        icon.setToolTip("PlatinumTree");

        icon.setPopupMenu(createPopupMenu());

        try{
            tray.add(icon);
        }catch(AWTException e){
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(()->SwingUtilities.invokeLater(()->tray.remove(icon))));
    }

    private PopupMenu createPopupMenu(){
        var menu = new PopupMenu();

        menu.add(createMenuItem("Enable writing", ()->platinumTree.setReadOnly(false)));
        menu.add(createMenuItem("Disable writing", ()->platinumTree.setReadOnly(true)));
        menu.add(createMenuItem("Scan for Switches", platinumTree::scan));
        menu.add(createMenuItem("Edit favorites", ()->new FavoriteEditor(platinumTree).show()));
        menu.addSeparator();
        menu.add(createMenuItem("Exit", ()->System.exit(0)));

        return menu;
    }

    private MenuItem createMenuItem(String label, Runnable task){
        var item = new MenuItem(label);
        item.addActionListener((event)->task.run());
        return item;
    }
}
