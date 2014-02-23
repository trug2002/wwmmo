package au.com.codeka.warworlds.server.cron;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.EmpireRank;

/**
 * Updates the ranks of empires.
 */
public class UpdateRanksCronJob extends CronJob {

    @Override
    public void run(String extra) throws Exception {
        TreeMap<Integer, EmpireRank> ranks = new TreeMap<Integer, EmpireRank>();

        String sql = "SELECT id AS empire_id FROM empires WHERE state <> 2";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                EmpireRank rank = new EmpireRank(rs);
                ranks.put(rank.getEmpireID(), rank);
            }
        }

        sql = "SELECT empire_id, SUM(num_ships) FROM fleets WHERE empire_id IS NOT NULL GROUP BY empire_id";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                int empireID = rs.getInt(1);
                int totalShips = rs.getInt(2);
                if (!ranks.containsKey(empireID)) {
                    continue;
                }
                ranks.get(empireID).setTotalShips(totalShips);
            }
        }

        sql = "SELECT empire_id, SUM(num_ships) FROM fleets WHERE empire_id IS NOT NULL GROUP BY empire_id";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                int empireID = rs.getInt(1);
                int totalShips = rs.getInt(2);
                if (!ranks.containsKey(empireID)) {
                    continue;
                }
                ranks.get(empireID).setTotalShips(totalShips);
            }
        }

        sql = "SELECT empire_id, COUNT(*) FROM buildings GROUP BY empire_id";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                int empireID = rs.getInt(1);
                int totalBuildings = rs.getInt(2);
                if (!ranks.containsKey(empireID)) {
                    continue;
                }
                ranks.get(empireID).setTotalBuildings(totalBuildings);
            }
        }

        sql = "SELECT empire_id, COUNT(*), SUM(population) FROM colonies WHERE empire_id IS NOT NULL GROUP BY empire_id";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                int empireID = rs.getInt(1);
                int totalColonies = rs.getInt(2);
                int totalPopulation = rs.getInt(3);
                if (!ranks.containsKey(empireID)) {
                    continue;
                }
                ranks.get(empireID).setTotalColonies(totalColonies);
                ranks.get(empireID).setTotalPopulation(totalPopulation);
            }
        }

        sql = "SELECT empire_id, COUNT(*) FROM (" +
               " SELECT empire_id, star_id" +
               " FROM stars" +
               " INNER JOIN colonies ON colonies.star_id = stars.id" +
               " WHERE colonies.empire_id IS NOT NULL" +
               " GROUP BY empire_id, star_id" +
              ") AS stars" +
             " GROUP BY empire_id";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                int empireID = rs.getInt(1);
                int totalStars = rs.getInt(2);
                if (!ranks.containsKey(empireID)) {
                    continue;
                }
                ranks.get(empireID).setTotalStars(totalStars);
            }
        }

        ArrayList<EmpireRank> sortedRanks = new ArrayList<EmpireRank>(ranks.values());
        Collections.sort(sortedRanks, new Comparator<EmpireRank>() {
            @Override
            public int compare(EmpireRank left, EmpireRank right) {
                int diff = right.getTotalPopulation() - left.getTotalPopulation();
                if (diff != 0)
                    return diff;

                diff = right.getTotalColonies() - left.getTotalColonies();
                if (diff != 0)
                    return diff;

                diff = right.getTotalStars() - left.getTotalStars();
                if (diff != 0)
                    return diff;

                diff = right.getTotalShips() - left.getTotalShips();
                return diff;
            }
        });

        sql = "INSERT INTO empire_ranks (empire_id, rank, total_stars, total_colonies," +
                                       " total_buildings, total_ships, total_population)" +
             " VALUES (?, ?, ?, ?, ?, ?, ?)" +
             " ON DUPLICATE KEY UPDATE" +
                 " rank = ?, total_stars = ?, total_colonies = ?, total_buildings = ?," +
                 " total_ships = ?, total_population = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            int rankValue = 1;
            for (EmpireRank rank : sortedRanks) {
                stmt.setInt(1, rank.getEmpireID());
                stmt.setInt(2, rankValue);
                stmt.setInt(3, rank.getTotalStars());
                stmt.setInt(4, rank.getTotalColonies());
                stmt.setInt(5, rank.getTotalBuildings());
                stmt.setInt(6, rank.getTotalShips());
                stmt.setInt(7, rank.getTotalPopulation());
                stmt.setInt(8, rankValue);
                stmt.setInt(9, rank.getTotalStars());
                stmt.setInt(10, rank.getTotalColonies());
                stmt.setInt(11, rank.getTotalBuildings());
                stmt.setInt(12, rank.getTotalShips());
                stmt.setInt(13, rank.getTotalPopulation());
                stmt.update();

                rankValue ++;
            }
        }
    }
}