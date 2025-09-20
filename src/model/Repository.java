package model;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 *Repository 
 *เก็บ cache ในmem ทำ Map/List
 *โหลด/บันทึกจาก CSV ในโฟลเดอร์ resources
 *
 *CSV Format หัวตาราง
 *projects.csv     : projectId,name,goal,deadline,category,raised
 *reward_tiers.csv : projectId,tierName,minAmount,quota
 *pledges.csv      : pledgeId,userId,projectId,amount,tierName,status,createdAt
 *users.csv        : userId,username,displayName,password
 */
public class Repository {

    //==== stores ใน mem ====
    private final Map<String, Project> projects = new LinkedHashMap<>();
    private final Map<String, List<RewardTier>> rewardByProject = new HashMap<>();
    private final Map<String, Pledge> pledges = new LinkedHashMap<>();
    private final Map<String, User> users = new LinkedHashMap<>();

    //==== CSV paths ====
    private final Path dir = Paths.get("resources");
    private final Path fProjects = dir.resolve("projects.csv");
    private final Path fRewards  = dir.resolve("reward_tiers.csv");
    private final Path fPledges  = dir.resolve("pledges.csv");
    private final Path fUsers    = dir.resolve("users.csv");

    public Repository() {
        loadAll();
    }

    //---------- query (เรียกจาก Controller) ----------

    public Collection<Project> listProjects() {
        return new ArrayList<>(projects.values());
    }

    public Optional<Project> getProject(String projectId) {
        return Optional.ofNullable(projects.get(projectId));
    }

    public List<RewardTier> listRewardTiers(String projectId) {
        return new ArrayList<>(rewardByProject.getOrDefault(projectId, List.of()));
    }

    public Optional<RewardTier> getRewardTier(String projectId, String tierName) {
        return listRewardTiers(projectId).stream()
                .filter(t -> t.getTierName().equals(tierName))
                .findFirst();
    }

    public Collection<Pledge> listPledges() {
        return new ArrayList<>(pledges.values());
    }

    public long countPledgeByStatus(PledgeStatus status) {
        return pledges.values().stream().filter(p -> p.getStatus() == status).count();
    }

    public Optional<User> findUserByUsername(String username) {
        return users.values().stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    //---------- Mutations ----------

    /**เพิ่ม/อัพเดต Project (ใช้ตอน seed หรือตอนจะ edit) */
    public void upsertProject(Project p) {
        projects.put(p.getId(), p);
        saveProjects();
    }

    /**เพิ่ม/อัพเดต RewardTier (เป็น unique ต่อตัว projectId + tierName)*/
    public void upsertRewardTier(RewardTier t) {
        var list = new ArrayList<>(rewardByProject.getOrDefault(t.getProjectId(), new ArrayList<>()));
        // แทนที่ของเดิมถ้าชื่อซ้ำ
        list.removeIf(x -> x.getTierName().equals(t.getTierName()));
        list.add(t);
        rewardByProject.put(t.getProjectId(), list);
        saveRewardTiers();
    }

    /**เพิ่มผู้ใช้ */
    public void upsertUser(User u) {
        users.put(u.getUserId(), u);
        saveUsers();
    }

    /**
     * บันทึก pledge 1 รายการ (ควรถูกตรวจแล้วใน Controller/Service)
     *ถ้า SUCCESS ก็เพิ่ม raised ของ project และลดquotaของ tier
     *บันทึกลง CSV
     */
    public void addPledge(Pledge p) {
        pledges.put(p.getPledgeId(), p);
        if (p.getStatus() == PledgeStatus.SUCCESS) {

            //เพิ่มยอด project
            var proj = projects.get(p.getProjectId());
            if (proj != null) proj.addRaised(p.getAmount());

            //ลด quota tier (ถ้ามี)
            
            if (p.getTierName() != null) {
                getRewardTier(p.getProjectId(), p.getTierName()).ifPresent(RewardTier::consumeOneQuota);
            }
            saveProjects();
            saveRewardTiers();
        }
        savePledges();
    }

    //---------- Load / Save ----------

    private void loadAll() {
        try { Files.createDirectories(dir); } catch (IOException ignored) {}

        loadProjects();
        loadRewardTiers();
        loadUsers();
        loadPledges(); //หลัง projects/rewards/users แล้ว จะได้อ้างอิงได้
    }

    private void loadProjects() {
        projects.clear();
        if (!Files.exists(fProjects)) { saveProjects(); return; }
        try (var br = Files.newBufferedReader(fProjects)) {
            String line; boolean skipHeader = true;
            while ((line = br.readLine()) != null) {
                if (skipHeader && line.startsWith("projectId")) { skipHeader = false; continue; }
                if (line.isBlank()) continue;
                String[] parts = splitCsv(line, 6);
                String id = parts[0];
                String name = parts[1];
                double goal = parseDouble(parts[2]);
                LocalDate deadline = LocalDate.parse(parts[3]);
                String category = parts[4];
                double raised = parseDouble(parts[5]);

                Project p = new Project(id, name, goal, deadline, category);
                //override raised จากไฟล์
                if (raised > 0) p.addRaised(raised);
                projects.put(id, p);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void saveProjects() {
        try (var bw = Files.newBufferedWriter(fProjects)) {
            bw.write("projectId,name,goal,deadline,category,raised");
            bw.newLine();
            for (Project p : projects.values()) {
                bw.write(String.join(",",
                        p.getId(),
                        esc(p.getName()),
                        String.valueOf(p.getGoal()),
                        p.getDeadline().toString(),
                        esc(p.getCategory()),
                        String.valueOf(p.getRaised())
                ));
                bw.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadRewardTiers() {
        rewardByProject.clear();
        if (!Files.exists(fRewards)) { saveRewardTiers(); return; }
        try (var br = Files.newBufferedReader(fRewards)) {
            String line; boolean skipHeader = true;
            while ((line = br.readLine()) != null) {
                if (skipHeader && line.startsWith("projectId")) { skipHeader = false; continue; }
                if (line.isBlank()) continue;
                String[] parts = splitCsv(line, 4);
                RewardTier t = new RewardTier(
                        parts[0], parts[1], parseDouble(parts[2]), Integer.parseInt(parts[3]));
                upsertRewardTier(t); 
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void saveRewardTiers() {
        try (var bw = Files.newBufferedWriter(fRewards)) {
            bw.write("projectId,tierName,minAmount,quota");
            bw.newLine();
            for (var entry : rewardByProject.entrySet()) {
                for (var t : entry.getValue()) {
                    bw.write(String.join(",",
                            t.getProjectId(),
                            esc(t.getTierName()),
                            String.valueOf(t.getMinAmount()),
                            String.valueOf(t.getQuota())
                    ));
                    bw.newLine();
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadUsers() {
        users.clear();
        if (!Files.exists(fUsers)) { saveUsers(); return; }
        try (var br = Files.newBufferedReader(fUsers)) {
            String line; boolean skipHeader = true;
            while ((line = br.readLine()) != null) {
                if (skipHeader && line.startsWith("userId")) { skipHeader = false; continue; }
                if (line.isBlank()) continue;
                String[] parts = splitCsv(line, 4);
                var u = new User(parts[0], parts[1], parts[2], parts[3]);
                users.put(u.getUserId(), u);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void saveUsers() {
        try (var bw = Files.newBufferedWriter(fUsers)) {
            bw.write("userId,username,displayName,password");
            bw.newLine();
            for (User u : users.values()) {
                bw.write(String.join(",",
                        u.getUserId(),
                        esc(u.getUsername()),
                        esc(u.getDisplayName()),
                        esc(u.getPassword())
                ));
                bw.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadPledges() {
        pledges.clear();
        if (!Files.exists(fPledges)) { savePledges(); return; }
        try (var br = Files.newBufferedReader(fPledges)) {
            String line; boolean skipHeader = true;
            while ((line = br.readLine()) != null) {
                if (skipHeader && line.startsWith("pledgeId")) { skipHeader = false; continue; }
                if (line.isBlank()) continue;
                String[] parts = splitCsv(line, 7);
                Pledge p = new Pledge(
                        parts[0], parts[1], parts[2], parseDouble(parts[3]),
                        emptyToNull(parts[4]),
                        PledgeStatus.valueOf(parts[5]),
                        LocalDateTime.parse(parts[6])
                );
                pledges.put(p.getPledgeId(), p);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void savePledges() {
        try (var bw = Files.newBufferedWriter(fPledges)) {
            bw.write("pledgeId,userId,projectId,amount,tierName,status,createdAt");
            bw.newLine();
            for (Pledge p : pledges.values()) {
                bw.write(String.join(",",
                        p.getPledgeId(),
                        p.getUserId(),
                        p.getProjectId(),
                        String.valueOf(p.getAmount()),
                        nullToEmpty(p.getTierName()),
                        p.getStatus().name(),
                        p.getCreatedAt().toString()
                ));
                bw.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    //---------- Helpers ----------
    private static String[] splitCsv(String line, int expect) {
        //ใช้split
        String[] arr = line.split(",", -1);
        if (arr.length < expect) {
            //เติมช่องว่างให้ครบ
            String[] pad = new String[expect];
            Arrays.fill(pad, "");
            System.arraycopy(arr, 0, pad, 0, arr.length);
            return pad;
        }
        return arr;
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0d; }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace(",", " "); //กัน ,
    }

    private static String nullToEmpty(String s) { return (s == null) ? "" : s; }
    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
}
