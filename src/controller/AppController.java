package controller;

import model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 *AppController
 *
 *จัดการ login/logout 
 *จัดการ list/filter/sort ตัว Project
 *ตรวจ validation และสร้าง Pledge
 */
public class AppController {

    public enum SortMode { NEWEST, CLOSING_SOON, TOP_FUNDED }

    private final Repository repo;
    private final Validation validator;
    private User currentUser;

    public AppController(Repository repo, Validation validator) {
        this.repo = repo;
        this.validator = validator;
    }

    //--------- Auth ---------
    public boolean login(String username, String password) {
        Optional<User> u = repo.findUserByUsername(username);
        if (u.isPresent() && Objects.equals(u.get().getPassword(), password)) {
            currentUser = u.get();
            return true;
        }
        return false;
    }

    public void logout() { currentUser = null; }
    public boolean isLoggedIn() { return currentUser != null; }
    public User getCurrentUser() { return currentUser; }

    //--------- Query ---------
    public List<Project> listProjects(SortMode sortMode, String categoryFilter, String keyword) {
        List<Project> all = new ArrayList<>(repo.listProjects());

        //filter category
        if (categoryFilter != null && !categoryFilter.isBlank()) {
            String cat = categoryFilter.trim().toLowerCase();
            all = all.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().toLowerCase().contains(cat))
                    .collect(Collectors.toList());
        }

        //keyword in name
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim().toLowerCase();
            all = all.stream()
                    .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(k))
                    .collect(Collectors.toList());
        }

        //sort
        Comparator<Project> cmp;
        switch (sortMode) {
            case CLOSING_SOON -> cmp = Comparator.comparing(Project::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()));
            case TOP_FUNDED -> cmp = Comparator.comparing(Project::getRaised, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
            case NEWEST -> {
                //ไม่มี createdAt ใน Entity เดิจะใช้ projectIdเรียงจากมากไปน้อย 
                cmp = Comparator.comparingInt(p -> safeParseInt(p.getId()));
                cmp = cmp.reversed();
            }
            default -> cmp = Comparator.comparing(Project::getId);
        }
        all.sort(cmp);
        return all;
    }

    public Optional<Project> getProject(String projectId) {
        return repo.getProject(projectId);
    }

    public List<RewardTier> getRewardTiers(String projectId) {
        return repo.listRewardTiers(projectId);
    }

    public long countSuccess() { return repo.countPledgeByStatus(PledgeStatus.SUCCESS); }
    public long countReject()  { return repo.countPledgeByStatus(PledgeStatus.REJECT); }

    //--------- Pledge ---------
    public static class PledgeResult {
        public final boolean ok;
        public final String pledgeId;
        public final List<String> errors;

        public PledgeResult(boolean ok, String pledgeId, List<String> errors) {
            this.ok = ok;
            this.pledgeId = pledgeId;
            this.errors = errors;
        }
    }

    /**
     *สร้างรายการสนับสนุนใหม่ *ต้อง login ก่อน
     *ตรวจ:
     *-มีผู้ใช้ล็อกอิน
     *-โครงการต้องมีอยู่ และ deadline > วันนี้
     *-amount > 0
     *-ถ้าเลือก reward tier ต้องเป็นนamount ≥ minAmount และ quota > 0
     */
    public PledgeResult createPledge(String projectId, double amount, String tierNameOrNull) {
        List<String> errors = new ArrayList<>();
        if (!isLoggedIn()) errors.add("Please log in before pledging.");

        Optional<Project> optProj = repo.getProject(projectId);
        if (optProj.isEmpty()) errors.add("Project not found: " + projectId);

        Project proj = optProj.orElse(null);
        if (proj != null) {
            if (!validator.isFutureDeadline(proj.getDeadline())) {
                errors.add("Project deadline has passed (deadline: " + proj.getDeadline() + ").");
            }
        }

        if (amount <= 0) errors.add("Amount must be greater than 0.");

        RewardTier tier = null;
        if (tierNameOrNull != null && !tierNameOrNull.isBlank() && proj != null) {
            tier = repo.getRewardTier(projectId, tierNameOrNull).orElse(null);
            if (tier == null) {
                errors.add("Reward tier '" + tierNameOrNull + "' not found for this project.");
            } else {
                if (!validator.meetsMinAmount(amount, tier)) {
                    errors.add("Amount is below this reward's minimum (min: " + tier.getMinAmount() + ").");
                }
                if (!validator.hasQuota(tier)) {
                    errors.add("This reward has no remaining quota.");
                }
            }
        } else {
           
            if (amount <= 0) errors.add("Amount must be greater than 0.");
        }

        String newId = nextPledgeId();

        //ผลลัพธ์
        if (!errors.isEmpty()) {
            Pledge reject = new Pledge(newId,
                    isLoggedIn() ? currentUser.getUserId() : "-",
                    projectId, amount, tierNameOrNull, PledgeStatus.REJECT, LocalDateTime.now());
            repo.addPledge(reject);
            return new PledgeResult(false, newId, errors);
        }

        //สำเร็จ จะบันทึก + อัปเดตยอด/โควตา
        Pledge success = new Pledge(newId,
                currentUser.getUserId(),
                projectId, amount, tierNameOrNull, PledgeStatus.SUCCESS, LocalDateTime.now());
        repo.addPledge(success);
        return new PledgeResult(true, newId, List.of());
    }

    //--------- Helpers ---------
    private String nextPledgeId() {
        int n = repo.listPledges().size() + 1;
        return String.format("P%03d", n);
    }

    private int safeParseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
