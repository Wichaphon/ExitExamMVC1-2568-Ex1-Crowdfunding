package view;

import controller.AppController;
import controller.AppController.SortMode;
import model.Project;
import model.RewardTier;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainView extends JFrame {

    private final AppController controller;

    //--- Login bar ---
    private final JTextField tfUser = new JTextField(10);
    private final JPasswordField tfPass = new JPasswordField(10);
    private final JButton btnLogin = new JButton("Login");
    private final JButton btnLogout = new JButton("Logout");
    private final JLabel lbWho = new JLabel("User: -");

    //--- Projects tab controls ---
    private final JComboBox<String> cbSort = new JComboBox<>(new String[]{"NEWEST", "CLOSING_SOON", "TOP_FUNDED"});
    private final JTextField tfCategory = new JTextField(10);
    private final JTextField tfKeyword = new JTextField(12);
    private final JButton btnRefresh = new JButton("Refresh");
    private final JButton btnPledge = new JButton("Pledge Selected");

    private final ProjectTableModel projectModel = new ProjectTableModel();
    private final JTable projectTable = new JTable(projectModel);

    //--- Stats tab ---
    private final JLabel lbSuccess = new JLabel("SUCCESS: 0");
    private final JLabel lbReject = new JLabel("REJECT: 0");
    private final JButton btnStatRefresh = new JButton("Refresh Stats");

    public MainView(AppController controller) {
        super("Crowdfund MVC 66050386");
        System.out.println("[UI] entering MainView constructor");
        this.controller = controller;

        //Frame
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        //UI
        add(buildLoginBar(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);

        //Wire events
        btnLogin.addActionListener(e -> doLogin());
        btnLogout.addActionListener(e -> doLogout());
        btnRefresh.addActionListener(e -> refreshProjects());
        btnPledge.addActionListener(e -> doPledgeSelected());
        btnStatRefresh.addActionListener(e -> refreshStats());

        refreshProjects();
        refreshStats();
        updateAuthUI();
        pack(); 
        if (getWidth() < 900 || getHeight() < 500) {
            setSize(980, 640); 
        }
        setLocationRelativeTo(null);  
        setVisible(true);                
        toFront();                      
        System.out.println("[UI] MainView visible = " + isShowing());

        //บางเครื่องต้องreq focus หลัง setVisible
        SwingUtilities.invokeLater(() -> {
            requestFocus();
            projectTable.requestFocusInWindow();
            System.out.println("[UI] focus requested; isShowing=" + isShowing());
        });
    }

    //===== UI builders =====
    private JComponent buildLoginBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.add(new JLabel("Username:")); p.add(tfUser);
        p.add(new JLabel("Password:")); p.add(tfPass);
        p.add(btnLogin); p.add(btnLogout);
        p.add(Box.createHorizontalStrut(12));
        p.add(lbWho);
        return p;
    }

    private JComponent buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Projects", buildProjectsTab());
        tabs.addTab("Stats", buildStatsTab());
        return tabs;
    }

    private JComponent buildProjectsTab() {
        JPanel root = new JPanel(new BorderLayout());

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Sort:")); filters.add(cbSort);
        filters.add(new JLabel("Category:")); filters.add(tfCategory);
        filters.add(new JLabel("Keyword:")); filters.add(tfKeyword);
        filters.add(btnRefresh);
        root.add(filters, BorderLayout.NORTH);

        projectTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectTable.setAutoCreateRowSorter(true);
        root.add(new JScrollPane(projectTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnPledge);
        root.add(bottom, BorderLayout.SOUTH);

        return root;
    }

    private JComponent buildStatsTab() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0; gc.insets = new Insets(6,6,6,6);
        p.add(lbSuccess, gc);
        gc.gridy++;
        p.add(lbReject, gc);
        gc.gridy++;
        p.add(btnStatRefresh, gc);
        return p;
    }

    //===== Actions ต่างๆ=====
    private void doLogin() {
        var user = tfUser.getText().trim();
        var pass = new String(tfPass.getPassword()).trim();
        boolean ok = controller.login(user, pass);
        JOptionPane.showMessageDialog(this, ok ? "Login success" : "Invalid username or password",
                ok ? "Info" : "Error",
                ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        updateAuthUI();
    }

    private void doLogout() {
        controller.logout();
        updateAuthUI();
    }

    private void updateAuthUI() {
        boolean in = controller.isLoggedIn();
        btnLogout.setEnabled(in);
        lbWho.setText("User: " + (in ? controller.getCurrentUser().getDisplayName() : "-"));
    }

    private void refreshProjects() {
        SortMode mode = switch (String.valueOf(cbSort.getSelectedItem())) {
            case "CLOSING_SOON" -> SortMode.CLOSING_SOON;
            case "TOP_FUNDED" -> SortMode.TOP_FUNDED;
            default -> SortMode.NEWEST;
        };
        String cat = tfCategory.getText().trim();
        String kw = tfKeyword.getText().trim();

        List<Project> rows = controller.listProjects(mode, cat, kw);
        projectModel.setData(rows);
    }

    private void refreshStats() {
        lbSuccess.setText("SUCCESS: " + controller.countSuccess());
        lbReject.setText("REJECT: " + controller.countReject());
    }

    private void doPledgeSelected() {
    int row = projectTable.getSelectedRow();
    if (row < 0) {
        JOptionPane.showMessageDialog(this, "Please select a project from the table first",
                "Warning", JOptionPane.WARNING_MESSAGE);
        return;
    }
    int modelIndex = projectTable.convertRowIndexToModel(row);
    Project p = projectModel.getAt(modelIndex);

    if (!controller.isLoggedIn()) {
        JOptionPane.showMessageDialog(this, "Please log in before pledging",
                "Warning", JOptionPane.WARNING_MESSAGE);
        return;
    }

    List<RewardTier> tiers = new ArrayList<>(controller.getRewardTiers(p.getId()));
    EntityForm dlg = new EntityForm(this, controller, p, tiers, () -> {
        refreshProjects();
        refreshStats();
    });
    dlg.setVisible(true);
}


    //===== Table model =====
    private static class ProjectTableModel extends AbstractTableModel {
        private final String[] cols = {"ID", "Name", "Goal", "Raised", "Progress %", "Deadline", "Category"};
        private final DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        private List<Project> data = new ArrayList<>();

        public void setData(List<Project> rows) {
            this.data = new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public Project getAt(int row) { return data.get(row); }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Project p = data.get(r);
            return switch (c) {
                case 0 -> p.getId();
                case 1 -> p.getName();
                case 2 -> String.format("%.2f", p.getGoal());
                case 3 -> String.format("%.2f", p.getRaised());
                case 4 -> String.format("%.2f", (p.getRaised() / p.getGoal()) * 100.0);
                case 5 -> p.getDeadline().format(fmt);
                case 6 -> p.getCategory();
                default -> "";
            };
        }
    }
}
