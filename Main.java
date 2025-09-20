import controller.AppController;
import model.*;
import view.MainView;

import javax.swing.*;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");

        Repository repo = new Repository();
        Validation validator = new Validation();
        AppController controller = new AppController(repo, validator);

        seedDemoData(repo, controller);

        System.out.println("[BOOT] projects=" + repo.listProjects().size());

        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable ignore) {}
            MainView ui = new MainView(controller);
            ui.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            ui.setVisible(true);
        });
    }
    
    //สร้าง seed ไว้ใน db -------------

    private static void seedDemoData(Repository repo, AppController controller) {
        //----- Users (upsert ได้ ไม่กระทบตัวprogress) -----
        repo.upsertUser(new User("U001", "alice",   "Alice",   "alice123"));
        repo.upsertUser(new User("U002", "bob",     "Bob",     "bob123"));
        repo.upsertUser(new User("U003", "charlie", "Charlie", "charlie123"));
        repo.upsertUser(new User("U004", "diana",   "Diana",   "diana123"));
        repo.upsertUser(new User("U005", "eric",    "Eric",    "eric123"));
        repo.upsertUser(new User("U006", "fiona",   "Fiona",   "fiona123"));
        repo.upsertUser(new User("U007", "george",  "George",  "george123"));
        repo.upsertUser(new User("U008", "helen",   "Helen",   "helen123"));
        repo.upsertUser(new User("U009", "ivan",    "Ivan",    "ivan123"));
        repo.upsertUser(new User("U010", "jane",    "Jane",    "jane123"));

        //----- Projects CREATE-IF-ABSENT (จะไม่ไปรีเซ็ต raised เดิม) -----
        ensureProject(repo, "10000001", "Smart Hydro Farm",    150000, LocalDate.now().plusDays(30), "TECH");
        ensureProject(repo, "10000002", "Indie Board Game",     50000, LocalDate.now().plusDays(25), "ART");
        ensureProject(repo, "10000003", "Community Clinic",    200000, LocalDate.now().plusDays(45), "HEALTH");
        ensureProject(repo, "10000004", "After-School Program",  80000, LocalDate.now().plusDays(40), "EDUCATION");
        ensureProject(repo, "10000005", "Green City Trees",     120000, LocalDate.now().plusDays(35), "ENV");
        ensureProject(repo, "10000006", "Street Food Festival",  60000, LocalDate.now().plusDays(20), "FOOD");
        ensureProject(repo, "10000007", "Open Source IDE",      180000, LocalDate.now().plusDays(55), "TECH");
        ensureProject(repo, "10000008", "Local Band Album",      70000, LocalDate.now().plusDays(28), "MUSIC");

        //----- Reward tiers CREATE-IF-ABSENT (ไม่reset quota ที่ถูกใชไปแล้ว) -----
        ensureReward(repo, "10000001", "Supporter",   200, 500);
        ensureReward(repo, "10000001", "Starter Kit", 1000, 50);
        ensureReward(repo, "10000001", "Pro Kit",     3000, 10);

        ensureReward(repo, "10000002", "Early Bird",  300, 100);
        ensureReward(repo, "10000002", "Collector",  1200, 20);

        ensureReward(repo, "10000003", "Donor",       500, 400);
        ensureReward(repo, "10000003", "Sponsor",    3000, 40);

        ensureReward(repo, "10000004", "Backer",      250, 200);
        ensureReward(repo, "10000004", "Patron",     1500, 30);

        ensureReward(repo, "10000005", "Sapling",     300, 300);
        ensureReward(repo, "10000005", "Groves",     2000, 25);

        ensureReward(repo, "10000006", "Taster",      150, 300);
        ensureReward(repo, "10000006", "VIP Pass",    900, 40);

        ensureReward(repo, "10000007", "Contributor", 400, 300);
        ensureReward(repo, "10000007", "Sponsor",    2500, 25);

        ensureReward(repo, "10000008", "Fan",         200, 300);
        ensureReward(repo, "10000008", "Producer",   1800, 20);

        //----- Pledges seed ครั้งแรกเท่านั้น (ถ้ายังไม่มีไฟล์) -----
        if (repo.listPledges().isEmpty()) {
            //ตัวอย่างทั้ง SUCCESS/REJECT
            controller.login("alice", "alice123");
            controller.createPledge("10000001", 1000, "Starter Kit"); //SUCCESS
            controller.createPledge("10000001", 100,  "Supporter");   //REJECT

            controller.login("bob", "bob123");
            controller.createPledge("10000002", 300,  "Early Bird");  //SUCCESS
            controller.createPledge("10000002", 200,  "Early Bird");  //REJECT

            controller.login("charlie", "charlie123");
            controller.createPledge("10000003", 700,  null);          //SUCCESS(no reward)

            controller.login("diana", "diana123");
            controller.createPledge("10000004", 100,  "Backer");      //REJECT(below min)

            controller.login("eric", "eric123");
            controller.createPledge("10000005", 2200, "Groves");      //SUCCESS

            controller.login("fiona", "fiona123");
            controller.createPledge("10000006", 900,  "VIP Pass");    //SUCCESS

            controller.login("george", "george123");
            controller.createPledge("10000007", 500,  "Contributor"); //SUCCESS

            controller.login("helen", "helen123");
            controller.createPledge("10000008", 1800, "Producer");    //SUCCESS

            // เพิ่มเติม: successful pledges เพื่อให้ progress ชัดเจนตั้งแต่เปิดแอป
            controller.login("ivan", "ivan123");
            controller.createPledge("10000001", 200,  "Supporter");   //SUCCESS

            controller.login("jane", "jane123");
            controller.createPledge("10000002", 1200, "Collector");   //SUCCESS

            controller.login("alice", "alice123");
            controller.createPledge("10000007", 2500, "Sponsor");     //SUCCESS

            controller.login("bob", "bob123");
            controller.createPledge("10000008", 200,  "Fan");         //SUCCESS

            controller.login("charlie", "charlie123");
            controller.createPledge("10000005", 300,  "Sapling");     //SUCCESS

            controller.login("diana", "diana123");
            controller.createPledge("10000006", 150,  "Taster");      //SUCCESS

            controller.login("eric", "eric123");
            controller.createPledge("10000003", 3000, "Sponsor");     //SUCCESS

            controller.login("fiona", "fiona123");
            controller.createPledge("10000004", 250,  "Backer");      //SUCCESS

            controller.login("george", "george123");
            controller.createPledge("10000002", 300,  "Early Bird");  //SUCCESS

            controller.login("helen", "helen123");
            controller.createPledge("10000001", 3000, "Pro Kit");     //SUCCESS

            controller.logout();
        }
    }

    //--- helpers create-if-absent ---
    private static void ensureProject(Repository repo, String id, String name, double goal,
                                      LocalDate deadline, String category) {
        if (repo.getProject(id).isEmpty()) {
            repo.upsertProject(new Project(id, name, goal, deadline, category));
        }
    }

    private static void ensureReward(Repository repo, String projectId, String tierName,
                                     double minAmount, int quota) {
        if (repo.getRewardTier(projectId, tierName).isEmpty()) {
            repo.upsertRewardTier(new RewardTier(projectId, tierName, minAmount, quota));
        }
    }
}
