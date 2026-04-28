package com.focusdesk.app;

import com.focusdesk.dao.DatabaseManager;
import com.focusdesk.dao.PomodoroDAO;

public class PomodoroTest {
    public static void main(String[] args) throws Exception {
        DatabaseManager.init();

        PomodoroDAO dao = new PomodoroDAO();
        dao.logSession(1, 25); // user_id=1 must exist

        var list = dao.listByUser(1);
        System.out.println("Sessions: " + list.size());
        for (var s : list) {
            System.out.println(s.getId() + " user=" + s.getUserId() + " focus=" + s.getFocusMinutes());
        }
    }
}