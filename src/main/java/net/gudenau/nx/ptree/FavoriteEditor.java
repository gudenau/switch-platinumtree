package net.gudenau.nx.ptree;

import net.gudenau.nx.ptree.util.Platform;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.List;

class FavoriteEditor{
    private final PlatinumTree platinumTree;
    private final List<Favorite> favorites;

    private JFrame frame;
    private DefaultListModel<Favorite> model;
    private JList<Favorite> list;

    FavoriteEditor(PlatinumTree platinumTree){
        this.platinumTree = platinumTree;
        favorites = platinumTree.getFavorites();
    }

    void show(){
        frame = new JFrame("Platinum Tree");
        frame.setContentPane(createContentPane());
        frame.setSize(320, 240);
        frame.setMinimumSize(frame.getSize());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        var screen = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(
            (screen.width - frame.getWidth()) >> 1,
            (screen.height - frame.getHeight()) >> 1
        );
        frame.setVisible(true);
    }

    private JPanel createContentPane(){
        var panel = new JPanel(new BorderLayout());

        model = new DefaultListModel<>();
        list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new FavoriteRenderer());
        favorites.forEach(model::addElement);
        panel.add(list, BorderLayout.CENTER);

        panel.add(createEditPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createEditPanel(){
        var panel = new JPanel(new GridBagLayout());

        var constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.gridx = 1;
        constraints.gridy = 1;
        panel.add(createButton("Add", this::add), constraints);

        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 2;
        panel.add(createButton("Edit", this::edit), constraints);

        constraints.anchor = GridBagConstraints.EAST;
        constraints.gridx = 3;
        panel.add(createButton("Delete", this::delete), constraints);

        return panel;
    }

    private void add(){
        var dialog = new JDialog(frame, "Add Favorite");
        dialog.setModal(true);
        dialog.setContentPane(createAddPane(dialog));
        dialog.setResizable(false);
        dialog.pack();
        var frameLocation = frame.getLocation();
        var frameSize = frame.getSize();
        var dialogSize = dialog.getSize();
        dialog.setLocation(
            ((frameSize.width - dialogSize.width) >> 1) + frameLocation.x,
            ((frameSize.height - dialogSize.height) >> 1) + frameLocation.y
        );
        dialog.setVisible(true);
    }

    private JPanel createAddPane(JDialog dialog){
        return createEditPane(dialog, null);
    }

    private JPanel createEditPane(JDialog dialog, Favorite favorite){
        var panel = new JPanel(new GridBagLayout());
        var constraints = new GridBagConstraints();

        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0;
        panel.add(new JLabel("Path"), constraints);

        constraints.gridy = 2;
        panel.add(new JLabel("Name"), constraints);

        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        var pathField = new JTextField(20);
        panel.add(pathField, constraints);

        constraints.gridy = 2;
        var nameField = new JTextField(20);
        panel.add(nameField, constraints);

        if(favorite != null){
            nameField.setText(favorite.getName());
            if(Platform.getOS() == Platform.OS.WINDOWS){
                pathField.setText(favorite.getDrive() + ":" + favorite.getName());
            }else{
                pathField.setText(favorite.getPath());
            }
        }

        constraints.gridx = 3;
        constraints.gridy = 1;
        constraints.weightx = 0;
        panel.add(createButton("Browse", ()->{
            var chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setFileFilter(new FileFilter(){
                @Override
                public boolean accept(File f){
                    return f.isDirectory();
                }

                @Override
                public String getDescription(){
                    return "Directory";
                }
            });
            if(chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION){
                var file = chooser.getSelectedFile();
                pathField.setText(file.getAbsolutePath());
                if(nameField.getText().isBlank()){
                    nameField.setText(file.getName());
                }
            }
        }), constraints);

        constraints.gridy = 2;
        panel.add(createButton("Add", ()->{
            var path = pathField.getText();
            var name = nameField.getText();

            if(path.isBlank() || name.isBlank()){
                return;
            }

            String drive;
            if(Platform.getOS() == Platform.OS.WINDOWS){
                var index = path.indexOf(":");
                drive = path.substring(0, index);
                path = path.substring(index + 1);
            }else{
                drive = "root";
            }

            if(favorite == null){
                favorites.add(new Favorite(name, drive, path));
            }else{
                favorite.set(name, drive, path);
            }

            platinumTree.saveFavorites();
            model.removeAllElements();
            favorites.forEach(model::addElement);

            dialog.dispose();
        }), constraints);

        return panel;
    }

    private void edit(){
        var favorite = list.getSelectedValue();
        if(favorite == null){
            return;
        }

        var dialog = new JDialog(frame, "Edit Favorite");
        dialog.setModal(true);
        dialog.setContentPane(createEditPane(dialog, favorite));
        dialog.setResizable(false);
        dialog.pack();
        var frameLocation = frame.getLocation();
        var frameSize = frame.getSize();
        var dialogSize = dialog.getSize();
        dialog.setLocation(
            ((frameSize.width - dialogSize.width) >> 1) + frameLocation.x,
            ((frameSize.height - dialogSize.height) >> 1) + frameLocation.y
        );
        dialog.setVisible(true);
    }

    private void delete(){
        var favorite = list.getSelectedValue();
        if(favorite == null){
            return;
        }
        favorites.remove(favorite);
        platinumTree.saveFavorites();
        model.removeAllElements();
        favorites.forEach(model::addElement);
    }

    private JButton createButton(String label, Runnable task){
        var button = new JButton(label);
        button.addActionListener((e)->task.run());
        return button;
    }

    private static class FavoriteRenderer extends JLabel implements ListCellRenderer<Favorite>{
        FavoriteRenderer(){
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Favorite> list, Favorite value, int index, boolean isSelected, boolean cellHasFocus){
            if(isSelected){
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }else{
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            setText(value.getName());

            return this;
        }
    }
}
