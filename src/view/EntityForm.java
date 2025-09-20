package view;

import controller.AppController;
import model.Project;
import model.RewardTier;

import javax.swing.*;
import java.awt.*;
import java.util.List;

//EntityForm คือ Pledge Dialog (UI)
 
public class EntityForm extends JDialog {

    private final AppController controller;
    private final Project project;
    private final List<RewardTier> tiers;
    private final Runnable onSuccessRefresh;

    private final JLabel lbProjName = new JLabel();
    private final JLabel lbGoal = new JLabel();
    private final JLabel lbRaised = new JLabel();
    private final JLabel lbDeadline = new JLabel();
    private final JComboBox<String> cbTier = new JComboBox<>();
    private final JTextField tfAmount = new JTextField(10);

    private final JButton btnOk = new JButton("Pledge");
    private final JButton btnCancel = new JButton("Cancel");

    public EntityForm(Frame owner, AppController controller, Project project,
                      List<RewardTier> tiers, Runnable onSuccessRefresh) {
        super(owner, "Pledge – " + project.getName(), true);
        this.controller = controller;
        this.project = project;
        this.tiers = tiers;
        this.onSuccessRefresh = onSuccessRefresh;

        setSize(520, 320);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        add(buildInfoPanel(), BorderLayout.NORTH);
        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        btnOk.addActionListener(e -> doPledge());
        btnCancel.addActionListener(e -> dispose());

        loadData();
    }

    private JComponent buildInfoPanel() {
        JPanel p = new JPanel(new GridLayout(2, 2, 8, 4));
        p.setBorder(BorderFactory.createTitledBorder("Project"));
        p.add(new JLabel("Name:")); p.add(lbProjName);
        p.add(new JLabel("Goal / Raised / Deadline:"));
        JPanel right = new JPanel(new GridLayout(1,3,6,0));
        right.add(lbGoal); right.add(lbRaised); right.add(lbDeadline);
        p.add(right);
        return p;
    }

    private JComponent buildFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Your Support"));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 8, 6, 8);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0; gc.gridy = 0;
        p.add(new JLabel("Reward Tier:"), gc);
        gc.gridx = 1; p.add(cbTier, gc);

        gc.gridx = 0; gc.gridy++;
        p.add(new JLabel("Amount:"), gc);
        gc.gridx = 1; p.add(tfAmount, gc);

        return p;
    }

    private JComponent buildButtons() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        p.add(btnCancel);
        p.add(btnOk);
        return p;
    }

    private void loadData() {
        lbProjName.setText(project.getName());
        lbGoal.setText(String.format("%.2f", project.getGoal()));
        lbRaised.setText(String.format("%.2f", project.getRaised()));
        lbDeadline.setText(project.getDeadline().toString());

        cbTier.addItem("(no reward)");
        for (var t : tiers) {
            cbTier.addItem(t.getTierName() + "  [min " + t.getMinAmount() + ", quota " + t.getQuota() + "]");
        }
    }

    private void doPledge() {
        String tierNameOrNull = null;
        int idx = cbTier.getSelectedIndex();
        if (idx > 0) {
            
            String full = String.valueOf(cbTier.getSelectedItem());
            int cut = full.indexOf("  [");
            tierNameOrNull = (cut > 0) ? full.substring(0, cut) : full;
        }

        double amount;
        try {
            amount = Double.parseDouble(tfAmount.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid amount.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        var res = controller.createPledge(project.getId(), amount, tierNameOrNull);
        if (res.ok) {
            JOptionPane.showMessageDialog(this, "Success! Pledge ID: " + res.pledgeId, "Info", JOptionPane.INFORMATION_MESSAGE);
            if (onSuccessRefresh != null) onSuccessRefresh.run();
            dispose();
        } else {
            StringBuilder sb = new StringBuilder("Failed:\n");
            for (String err : res.errors) sb.append("- ").append(err).append("\n");
            JOptionPane.showMessageDialog(this, sb.toString(), "Validation Errors", JOptionPane.WARNING_MESSAGE);
        }
    }
}
